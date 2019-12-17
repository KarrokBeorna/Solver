import javafx.beans.property.SimpleIntegerProperty
import tornadofx.*

class Logics: Controller() {

    val numBombs = SimpleIntegerProperty()
    val numFlags = SimpleIntegerProperty()
    val numTrueFlags = SimpleIntegerProperty()
    val numClicks = SimpleIntegerProperty()

    /**
     * Обязательно не data class, потому что с пометкой data
     * перебор элементов просто не сходит с 1 элемента
     *
     * Почему???
     */
    class Cell(var isVisible: Boolean = false, var flag: Boolean = false, val numberOfBombs: Int,
               val aroundCell: List<Int>, val index: Int, var useless: Boolean = false)

    /**
     * Класс для выдачи данных для отображения
     */
    class Data(val opening: MutableSet<Int>, val flagging: MutableSet<Int>, val index: Int)

    val board = mutableListOf<Cell>()
    private val openingNow = mutableSetOf<Int>()
    private val flagsNow = mutableSetOf<Int>()
    var boom = false
    var stop = false

    val checkList = mutableSetOf<Cell>()
    val recheck = mutableSetOf<Cell>()
    var numberOfChecks = 0
    val listBombs = mutableListOf<Int>()
    private val light = mutableListOf<Cell>()


    fun elements() {
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
                place !in listBombs) {
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
                board.add(Cell(isVisible = false, flag = false,
                    numberOfBombs = num, aroundCell = cellArea(i),index = i, useless = false))
            } else board.add(Cell(isVisible = false, flag = false,
                numberOfBombs = 9, aroundCell = cellArea(i), index = i, useless = false))
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

            }
            else if (researched.numberOfBombs in 1..8) {

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

                }
                else if (researched.numberOfBombs in 1..8 && !researched.useless) {

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
     * Выделяем ячейку, которую рассматриваем сейчас:
     * наименьшее число бомб и наименьший индекс
     */
    private fun backlight(): Cell {

        /**
         * Если список проверок вдруг опустел, то добавляем перепроверяемые
         * ячейки, это позволит избежать выбрасывания ошибки
         */
        if (checkList.isEmpty()) recheck()

        light.clear()
        openingNow.clear()
        flagsNow.clear()

        val temp = checkList.sortedWith(compareBy(Cell::numberOfBombs, Cell::index)).first()

        light.add(temp)
        return temp
    }

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
        if (numberFlags > current.numberOfBombs || numberFlags + close.size < current.numberOfBombs) {
            stop = true
        } else if (current.aroundCell.size - numOpenAround == current.numberOfBombs) {

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

        val current = backlight()
        val around = around(current)
        val numOpenAround = around.first
        val numberFlags = around.second
        val close = around.third

        /**
         * Если ячейка еще в списке проверки, но она уже не несет никакой новой информации,
         * то не наступаем на неё и вообще забываем навсегда
         */
        if (numberFlags == close.size + numberFlags) {
            checkList.remove(current)
            current.useless = true
            checkBombs()
        }

        numClicks.value++

        trivialCheck(current, numOpenAround, numberFlags, close)

        if (flagsNow.all { it in listBombs }) numTrueFlags.value = numFlags.value else numTrueFlags.value = 0

        return Data(openingNow, flagsNow, current.index)
    }

    /**
     * Каждую "небесполезную" перепроверяемую ячейку добавим в список проверок
     */
    private fun recheck() {
        recheck.forEach{ if(!it.useless) checkList.add(it) }
        recheck.clear()
    }



    private var immediateCheck: Cell? = null

    fun startSolver(): Pair<MutableSet<Int>, MutableSet<Int>> {

        val close = mutableSetOf<Cell>()

        recheck()

        val temp = mutableSetOf<Cell>()
        temp.addAll(checkList)

        /**
         * Добавляем в список закрытых ячеек все закрытые ячейки около исследуемых
         */
        for (i in checkList) {

            val around = around(i)
            around.third.forEach{ close.add(board[it]) }

        }

        /**
         * Пытаемся симулировать постановку бомбы в каждую закрытую клетку
         */
        for (i in close) {

            /**
             * Пересоздаем первоначальный список исследумых ячеек, убираем остановку
             */
            checkList.clear()
            checkList.addAll(temp)
            stop = false

            checkList.forEach { it.useless = false }
            close.forEach { it.isVisible = false; it.flag = false }
            i.flag = true

            numberOfChecks = 0

            /**
             * Пока мы не прошли список без изменений дважды и хотя бы один из списков не пуст мы:
             */
            while (numberOfChecks < 2 * (checkList.size + recheck.size) &&
                (checkList.isNotEmpty() || recheck.isNotEmpty()) && !stop && checkList.isNotEmpty()) {

                val current = checkList.first()

                if (!current.useless) {

                    val newAround = around(current)
                    trivialCheck(current, newAround.first, newAround.second, newAround.third)

                    if (stop) {
                        openingNow.add(i.index)
                        break
                    }
                }

                if (checkList.isEmpty()) recheck()
            }
        }

        /**
         * Пытаемся симулировать открытие ячейки
         */
        for (i in close) {

            checkList.clear()
            checkList.addAll(temp)
            stop = false

            checkList.forEach { it.useless = false }
            close.forEach { it.isVisible = false; it.flag = false }
            i.isVisible = true

            numberOfChecks = 0

            /**
             * Пока мы не прошли список без изменений дважды или хотя бы один из списков не пуст мы:
             */
            while (numberOfChecks < 2 * (checkList.size + recheck.size) &&
                (checkList.isNotEmpty() || recheck.isNotEmpty()) && !stop && checkList.isNotEmpty()) {

                val current = checkList.first()

                if (!current.useless) {

                    val newAround = around(current)
                    trivialCheck(current, newAround.first, newAround.second, newAround.third)

                    if (stop) {
                        flagsNow.add(i.index)
                        break
                    }

                    if (checkList.isEmpty()) recheck()
                }
            }
        }

        return openingNow to flagsNow
    }

    /**
     * 1) Запускаем с Экрана
     * 2) Создаем список списков
     * 3) Перебираем все закрытые точки, прилегающие к открытым, создаем их список
     * 4) Предполагаем, что мина в первой клетке списка, пробуем пройти сапёра дальше, открыть эти клетки из списка,
     * если видим, что у хотя бы одной клетки начинается перебор флагов рядом с ней, или недобор, то там 100% - пусто.
     * Запоминаем её для открытия.
     * 5) То есть когда мы поставили первую мину, мы идем по обычному алгоритму, но после каждого ВОЗМОЖНОГО открытия
     * клетки или флагирования клетки мы перепроверяем все клетки, чтобы не оказалось такого, что стоит лишняя мина,
     * или не хватает закрытых клеток, чтобы поставить флаги
     * 6) После каждого прохода мы добавляем список с измененными клетками
     */




    fun difficultCase() {

        light.clear()

        for (cell in checkList) {

            /**
             * Первый случай - поиск двойки и прилегающие к ней единицы
             */
            val deuceIndex = cell.index
            val around = around(cell)

            val left = board[deuceIndex - 1]
            val right = board[deuceIndex + 1]
            val top = board[deuceIndex - 15]
            val bot = board[deuceIndex + 15]

            val aroundLeft = around(left)
            val aroundRight = around(right)
            val aroundTop = around(top)
            val aroundBot = around(bot)

            val newNumInLeft = left.numberOfBombs - aroundLeft.second
            val newNumInRight = right.numberOfBombs - aroundRight.second
            val newNumInTop = top.numberOfBombs - aroundTop.second
            val newNumInBot = bot.numberOfBombs - aroundBot.second



            if (deuceIndex % 15 in 1..13 &&
                deuceIndex / 15 in 1..13 &&
                cell.numberOfBombs == 2) {

                if (left.numberOfBombs == 1 && right.numberOfBombs == 1 && left.isVisible && right.isVisible) {

                    light.addAll(listOf(cell, left, right))
                    val opening = listOf(deuceIndex - 15, deuceIndex + 15)
                    openingNow.addAll(opening)
                    opening.forEach { board[it].isVisible = true }
                    immediateCheck = cell

                    break
                } else if (top.numberOfBombs == 1 && bot.numberOfBombs == 1 && top.isVisible && bot.isVisible) {

                    light.addAll(listOf(cell, top, bot))
                    val opening = listOf(deuceIndex - 1, deuceIndex + 1)
                    openingNow.addAll(opening)
                    opening.forEach { board[it].isVisible = true }
                    immediateCheck = cell

                    break
                }

            }

            /**
             * Второй случай - поиск "двойки" и прилегающая к ней "единица"
             */

            if (deuceIndex % 15 in 1..13 &&
                deuceIndex / 15 in 1..13 &&
                cell.numberOfBombs - around.second == 2 &&
                around.first == 5) {

                fun flagAndVoid(opening: Cell) {
                    val temp = mutableListOf<Int>()
                    for (i in opening.aroundCell) {
                        if (i !in cell.aroundCell && (i == opening.index + 16 ||
                                    i == opening.index + 14 || i == opening.index - 14 || i == opening.index - 16)) {
                            temp.add(i)
                        }
                    }

                }

                if (newNumInTop == 1 && deuceIndex >= 30) {
                    flagAndVoid(top)
                    break
                }
                else if (newNumInBot == 1 && deuceIndex <= 194) {
                    flagAndVoid(bot)
                    break
                }
                else if (newNumInLeft == 1 && deuceIndex % 15 >= 2) {
                    flagAndVoid(left)
                    break
                }
                else if (newNumInRight == 1 && deuceIndex % 15 <= 12) {
                    flagAndVoid(right)
                    break
                }
            }
        }

    }
}


/**
 * !!!NEED!!!
 * solver для сложных случаев
 */

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