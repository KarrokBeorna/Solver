import javafx.beans.property.SimpleIntegerProperty
import tornadofx.*

class Logics: Controller() {

    val numBombs = SimpleIntegerProperty()
    val numFlags = SimpleIntegerProperty()
    val numTrueFlags = SimpleIntegerProperty()
    val numClicks = SimpleIntegerProperty()

    class Cell(
        var isVisible: Boolean = false, var flag: Boolean = false, val numberOfBombs: Int,
        val aroundCell: List<Int>, val index: Int, var useless: Boolean = false
    )

    class Data(val opening: MutableSet<Int>, val flagging: MutableSet<Int>, val index: Int)

    val board = mutableListOf<Cell>()
    val openingNow = mutableSetOf<Int>()
    val flagsNow = mutableSetOf<Int>()
    var boom = false

    /**
     * Состояния solver'a:
     * -2 ---------------------- нет сложных ситуаций
     * -1 ---------------------- разрешение на исследование закрытых клеток
     * 0..close.size * 2 - 1 --- двойной перебор закрытых клеток: в первом случае - флагами, во втором - открытием
     * close.size * 2 ---------- рандомим клетку, т.к. нет подходящих случаев
     */
    var solverState = -2
    var stop = 0

    val checkList = mutableSetOf<Cell>()
    private val tempCheckList = mutableSetOf<Int>()
    val recheck = mutableSetOf<Cell>()
    var numberOfChecks = 0
    val listBombs = mutableListOf<Int>()
    private val closed = mutableSetOf<Cell>()
    private val light = mutableSetOf<Cell>()

    /**
     * Загрузка начальных элементов
     */
    private fun elements() {
        bombs()
        cells()
    }

    /**
     * Рандомное расположение бомб
     */
    private fun bombs() {
        val num = (30..45).random()
        for (i in 1..num) {
            val place = (0..224).random()
            if (place !in 96..98 &&
                place !in 111..113 &&
                place !in 126..128 &&
                place !in listBombs
            ) {
                listBombs.add(place)
                numBombs.value++
            }
        }
    }

    /**
     * Запись необходимой информации в ячейки и запись ячейки в список ячеек
     */
    private fun cells() {
        for (i in 0..224) {
            if (i !in listBombs) {
                val listCellArea = cellArea(i)
                var num = 0
                for (ind in listCellArea) {
                    if (ind in listBombs)
                        num++
                }
                board.add(
                    Cell(
                        isVisible = false, flag = false,
                        numberOfBombs = num, aroundCell = cellArea(i), index = i, useless = false
                    )
                )
            } else board.add(
                Cell(
                    isVisible = false, flag = false,
                    numberOfBombs = 9, aroundCell = cellArea(i), index = i, useless = false
                )
            )
        }
    }

    /**
     * Индексы вокруг клетки
     */
    private fun cellArea(index: Int): List<Int> {
        return when {
            index == 0 -> {
                listOf(index + 1, index + 15, index + 16)
            }
            index == 14 -> {
                listOf(index - 1, index + 14, index + 15)
            }
            index == 210 -> {
                listOf(index + 1, index - 14, index - 15)
            }
            index == 224 -> {
                listOf(index - 1, index - 16, index - 15)
            }
            index in 1..13 -> {
                listOf(index - 1, index + 14, index + 15, index + 16, index + 1)
            }
            index % 15 == 0 -> {
                listOf(index - 15, index - 14, index + 1, index + 16, index + 15)
            }
            index % 15 == 14 -> {
                listOf(index - 15, index - 16, index - 1, index + 14, index + 15)
            }
            index in 210..223 -> {
                listOf(index - 1, index - 14, index - 15, index - 16, index + 1)
            }
            else -> {
                listOf(index - 15, index - 14, index + 1, index + 16, index + 15, index + 14, index - 1, index - 16)
            }
        }
    }

    /**
     * Первый клик и первые открытия
     */
    fun firstClick(): Data {

        val first = board[112]

        openingNow.add(112)
        first.useless = true
        first.isVisible = true

        for (i in first.aroundCell) {

            val researched = board[i]

            if (researched.numberOfBombs == 0) {

                empty(researched)
                openingNow.add(i)

            } else if (researched.numberOfBombs in 1..8) {

                openingNow.add(i)
                checkList.add(researched)
                researched.isVisible = true

            }
        }

        numClicks.value++

        return Data(openingNow, flagsNow, 112)
    }

    /**
     * Открытие области рядом с пустыми клетками
     */
    private fun empty(cell: Cell) {

        cell.isVisible = true
        cell.useless = true

        for (i in cell.aroundCell) {

            val researched = board[i]

            if (researched.numberOfBombs == 0 && !researched.useless) {

                empty(researched)
                openingNow.add(i)

            } else if (researched.numberOfBombs in 1..8 && !researched.useless) {

                openingNow.add(i)
                checkList.add(researched)
                researched.isVisible = true

            }
        }

    }

    /**
     * Функция для подсчёта
     * - открытых,
     * - флагнутых,
     * - закрытых ячеек
     */
    private fun around(cell: Cell): Triple<Int, Int, MutableSet<Int>> {

        var numOpenAround = 0
        var numberFlags = 0
        val close = mutableSetOf<Int>()

        for (i in cell.aroundCell) {

            val researched = board[i]

            when {

                researched.isVisible -> numOpenAround++
                researched.flag -> numberFlags++
                else -> close.add(i)

            }
        }
        return Triple(numOpenAround, numberFlags, close)
    }

    /**
     * Функция для простейшей проверки
     */
    private fun trivialCheck(current: Cell, numOpenAround: Int, numberFlags: Int, close: MutableSet<Int>) {
        /**
         * Если число ЗАКРЫТЫХ равно числу в ячейке, то
         * помечаем эти ячейки флажками,
         *
         * иначе, если число ФЛАГОВ уже равно числу в ячейке,
         * то все закрытые ячейки открываем,
         *
         * иначе пока что удаляем элемент из проверки и добавляем
         * его в множество повторных проверок
         */
        if (current.aroundCell.size - numOpenAround == current.numberOfBombs) {

            checkList.remove(current)
            numberOfChecks = 0
            for (i in close) {
                flagsNow.add(i)
                board[i].flag = true
                numFlags.value++
            }
                current.useless = true

        } else if (numberFlags == current.numberOfBombs) {

            openingNow.addAll(close)
            checkList.remove(current)
            current.useless = true
            numberOfChecks = 0

            for (i in close) {
                val researched = board[i]
                if (researched.numberOfBombs != 0) {
                    checkList.add(researched)
                    researched.isVisible = true
                } else {
                    empty(researched)
                }
            }

        } else {
            checkList.remove(current)
            recheck.add(current)
            numberOfChecks++
        }
    }

    /**
     * Основная функция обнаружения бомб
     */
    fun checkBombs(): Data {

        if (checkList.isEmpty()) recheck()

        var current = checkList.sortedWith(compareBy(Cell::numberOfBombs, Cell::index)).first()
        var around = around(current)
        var numOpenAround = around.first
        var numberFlags = around.second
        var close = around.third

        /**
         * Если ячейка еще в списке проверки, но она уже не несет никакой новой информации,
         * то не наступаем на неё и больше её не заметим никогда
         */
        while (close.isEmpty() || current.useless) {

            current.useless = true
            checkList.remove(current)

            if (checkList.isNotEmpty()) {
                current = checkList.sortedWith(compareBy(Cell::numberOfBombs, Cell::index)).first()
                around = around(current)
                numOpenAround = around.first
                numberFlags = around.second
                close = around.third
            } else recheck()

        }

        openingNow.clear()
        flagsNow.clear()
        light.clear()
        light.add(current)
        numClicks.value++

        trivialCheck(current, numOpenAround, numberFlags, close)

        if (flagsNow.all { it in listBombs }) numTrueFlags.value = numFlags.value else numTrueFlags.value = 0

        return Data(openingNow, flagsNow, current.index)
    }

    /**
     * Каждую "небесполезную" перепроверяемую ячейку добавим в список проверок
     */
    private fun recheck() {
        recheck.forEach { if (!it.useless) checkList.add(it) }
        recheck.clear()
    }

    private val oldClosed = mutableSetOf<Int>()
    private val allClosed = mutableListOf<Cell>()


    /**
     * Добавление закрытых клеток для исследования
     */
    fun researchedClosed() {

        recheck()
        numberOfChecks = 0
        openingNow.clear()
        flagsNow.clear()

        if (checkList.isEmpty() && recheck.isEmpty()) {

            board.forEach { if (!it.isVisible && !it.flag) allClosed.add(it)}
            stop = -1

            if (allClosed.size + numTrueFlags.value == listBombs.size) allClosed.forEach { flagsNow.add(it.index) }

        } else {

            for (i in checkList) {
                val around = around(i)
                around.third.forEach { closed.add(board[it]) }
            }

            stop = closed.size * 2
            checkList.forEach { tempCheckList.add(it.index) }
            solverState++
            closed.forEach { oldClosed.add(it.index) }
        }
    }

    fun update() {

        numberOfChecks = 0
        oldClosed.forEach { board[it].isVisible = false; board[it].flag = false }
        tempCheckList.forEach { checkList.add(board[it]); board[it].useless = false }

        solverState++

        if (closed.isEmpty()) {
            oldClosed.forEach { closed.add(board[it]) }
        }

    }

    private fun forSolver(tempList: MutableSet<Int>, current: Cell) {

        if (numberOfChecks > (checkList.size + recheck.size + 1) || checkList.isEmpty() && recheck.isEmpty()) {

            closed.remove(current)
            update()

        } else {

            val cell = checkList.minBy { it.index }!!
            val around = around(cell)
            val numOpenAround = around.first
            val numberFlags = around.second
            val close = around.third

            light.add(cell)

            if (numberFlags > cell.numberOfBombs || (close.size + numberFlags) < cell.numberOfBombs) {

                tempList.add(current.index)
                closed.remove(current)
                update()

            } else if (cell.aroundCell.size - numOpenAround == cell.numberOfBombs) {

                checkList.remove(cell)
                cell.useless = true
                numberOfChecks = 0

                for (i in close) {
                    board[i].flag = true
                }

            } else if (numberFlags == cell.numberOfBombs) {

                checkList.remove(cell)
                cell.useless = true
                numberOfChecks = 0

                for (i in close) {
                    board[i].isVisible = true
                }

            } else {
                checkList.remove(cell)
                recheck.add(cell)
                numberOfChecks++
            }

        }
    }

    fun solve(): MutableSet<Cell> {

        light.clear()
        if (checkList.isEmpty()) recheck()
        val current = closed.minBy { it.index }!!
        light.add(current)

        if (solverState < stop / 2) {

            board[current.index].flag = true

            forSolver(openingNow, current)

        } else {

            board[current.index].isVisible = true

            forSolver(flagsNow, current)

        }

        return light
    }

    fun randOrAct(): Pair<MutableSet<Int>, MutableSet<Int>> {

        light.clear()
        solverState = -2
        tempCheckList.clear()
        oldClosed.clear()
        numClicks.value++
        recheck()

        return if (flagsNow.isNotEmpty() || openingNow.isNotEmpty()) {

            if (openingNow.isNotEmpty()) openingNow.forEach { board[it].isVisible = true; checkList.add(board[it]) }
            if (flagsNow.isNotEmpty()) {
                flagsNow.forEach { board[it].flag = true }
                numFlags.value += flagsNow.size
                numTrueFlags.value += flagsNow.size
            }
            closed.clear()
            openingNow to flagsNow

        } else if (stop == -1) {

            val open = allClosed.random()
            if (open.index in listBombs) boom = true
            else {
                board[open.index].isVisible = true
                checkList.add(open)
            }
            closed.clear()
            mutableSetOf(open.index) to mutableSetOf()

        } else {

            val open = closed.random()
            if (open.index in listBombs) boom = true
            else {
                board[open.index].isVisible = true
                checkList.add(open)
            }
            closed.clear()
            mutableSetOf(open.index) to mutableSetOf()

        }
    }

    fun restart() {
        board.clear()
        checkList.clear()
        recheck.clear()
        listBombs.clear()
        numberOfChecks = 0
        solverState = -2
        stop = 0
        openingNow.clear()
        flagsNow.clear()
        light.clear()
        boom = false
        numTrueFlags.value = 0
        numFlags.value = 0
        numClicks.value = 0
        numBombs.value = 0
        elements()
    }
}


/**
 * Добавить случай, когда угловая клетка огорожена бомбами, но в ней самой нет бомб (нужно смотреть на кол-во флажков)
 * ?    ?   х   1
 * х    х   х   2
 * 2    2   1   1
 * Добавить рандом в клетки с "?"
 *
 * Добавить проверку на равенство закрытых клеток и кол-ва бомб, то есть добавить список закрытых клеток
 */

/**
 * 1) Почему я оставил recheck: потому что я хочу проходить сначала по меньшим элементам с наименьшим индексом,
 * если я буду обратно добавлять, то он опять вернется к ним
 */