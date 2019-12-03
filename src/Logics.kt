import javafx.beans.property.SimpleIntegerProperty
import tornadofx.*

class Logics: Controller() {

    val listNums = mutableListOf<Int>()
    val openingNow = mutableSetOf<Int>()
    private val openSet = mutableSetOf<Int>()
    private val emptyCells = mutableListOf<Int>()
    private val passed = mutableSetOf<Int>()
    private val cellsWithNums = mutableListOf<Int>()
    private val setFlags = mutableSetOf<Int>()
    private val indexAndNum = mutableSetOf<Pair<Int, Int>>()
    val flagsNow = mutableSetOf<Int>()
    private val tempMap = mutableListOf<Pair<Int, Int>>()
    private val recheck = mutableSetOf<Pair<Int, Int>>()
    var boom = false
    private var numberOfChecks = 0

    val numBombs = SimpleIntegerProperty()
    val numFlags = SimpleIntegerProperty()
    val numTrueFlags = SimpleIntegerProperty()
    val numClicks = SimpleIntegerProperty()

    val listBombs = mutableListOf<Int>()
    private val listCellArea = mutableListOf<Int>()

    fun elements() {
        bombs()
        nums()
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
     * Значения в ячейках в зависимости от числа бомб, расположенных в радиусе 1
     */
    private fun nums() {
        for (i in 0..224) {
            if (i !in listBombs) {
                cellArea(i)
                var num = 0
                for (ind in listCellArea) {
                    if (ind in listBombs)
                        num++
                }
                listNums.add(num)
            } else listNums.add(9)
        }
    }

    /**
     * Индексы вокруг клетки
     */
    private fun cellArea(index: Int) {
        listCellArea.clear()
        when {
            index == 0 -> {
                listCellArea.addAll(listOf(index + 1, index + 15, index + 16))
            }
            index == 14 -> {
                listCellArea.addAll(listOf(index - 1, index + 14, index + 15))
            }
            index == 210 -> {
                listCellArea.addAll(listOf(index + 1, index - 14, index - 15))
            }
            index == 224 -> {
                listCellArea.addAll(listOf(index - 1, index - 16, index - 15))
            }
            index in 1..13 -> {
                listCellArea.addAll(listOf(index - 1, index + 14, index + 15, index + 16, index + 1))
            }
            index % 15 == 0 -> {
                listCellArea.addAll(listOf(index - 15, index - 14, index + 1, index + 16, index + 15))
            }
            index % 15 == 14 -> {
                listCellArea.addAll(listOf(index - 15, index - 16, index - 1, index + 14, index + 15))
            }
            index in 210..223 -> {
                listCellArea.addAll(listOf(index - 1, index - 14, index - 15, index - 16, index + 1))
            }
            else -> {
                listCellArea.addAll(listOf(index - 15, index - 14, index + 1,
                    index + 16, index + 15, index + 14, index - 1, index - 16))
            }
        }
    }

    fun firstClick() {

        openSet.add(112)
        openingNow.add(112)
        passed.add(112)

        cellArea(112)

        for (i in listCellArea) {
            if (listNums[i] == 0) {
                emptyCells.add(i)
                openSet.add(i)
                openingNow.add(i)
                passed.add(i)
            } else {
                if (listNums[i] != 9) {
                    openSet.add(i)
                    openingNow.add(i)
                    cellsWithNums.add(i)
                    indexAndNum.add(i to listNums[i])
                }
            }
        }

        numClicks.value++

        if (emptyCells.size != 0) {
            empty()
        }
    }

    /**
     * Открытие области рядом с пустыми клетками
     */
    private fun empty() {
        while (emptyCells.size != 0) {

            cellArea(emptyCells.first())

            for (i in listCellArea) {
                if (listNums[i] == 0 && i !in passed) {
                    emptyCells.add(i)
                    openSet.add(i)
                    openingNow.add(i)
                    passed.add(i)
                } else {
                    if (listNums[i] in 1..8) {
                        openSet.add(i)
                        openingNow.add(i)
                        cellsWithNums.add(i)
                        indexAndNum.add(i to listNums[i])
                        recheck.add(i to listNums[i])
                    }
                }
            }

            emptyCells.removeAt(0)
        }
    }

    /**
     * Выделяем ячейку, которую рассматриваем сейчас
     */
    fun backlight(): Int {
        openingNow.clear()
        flagsNow.clear()
        tempMap.clear()
        tempMap.addAll(indexAndNum.sortedWith(compareBy { it.second; it.first }))
        return tempMap.first().first
    }

    fun checkBombs() {

        val current = backlight()
        val numInCurrent = listNums[current]

        numClicks.value++
        var numOpenAround = 0
        var numberFlags = 0
        val close = mutableListOf<Int>()

        /**
         * Запрашиваем область вокруг ячейки
         */
        cellArea(current)

        /**
         * Разделяем ячейки на
         * -открытые
         * -с флажком
         * -закрытые
         */
        for (i in listCellArea) {

            when (i) {
                in openSet -> numOpenAround++
                in setFlags -> numberFlags++
                else -> close.add(i)
            }

        }

        /**
         * Если число закрытых равно числу в ячейке, то
         * помечаем эти ячейки флажками,
         * иначе, если число флагов уже равно числу в ячейке,
         * то все закрытые ячейки открываем,
         * иначе пока что удаляем элемент из проверки и добавляем
         * его в множество повторных проверок
         */
        if (listCellArea.size - numOpenAround == numInCurrent) {
            setFlags.addAll(close)
            flagsNow.addAll(close)
            passed.add(current)
            indexAndNum.remove(current to numInCurrent)
            numberOfChecks = 0
        } else {
            if (numberFlags == numInCurrent) {
                openingNow.addAll(close)
                openSet.addAll(close)
                cellsWithNums.addAll(close)
                passed.add(current)
                indexAndNum.remove(current to numInCurrent)
                for (i in close) {
                    if (i !in passed) {
                        indexAndNum.add(i to listNums[i])
                        recheck.add(i to listNums[i])
                    }
                }
                numberOfChecks = 0
            } else {
                indexAndNum.remove(current to numInCurrent)
                recheck.add(current to numInCurrent)
                numberOfChecks++
            }
        }

        numFlags.value = setFlags.size

        if (setFlags.all { it in listBombs }) numTrueFlags.value = setFlags.size else numTrueFlags.value = 0

        /**
         * Если в спике открывающихся есть хотя бы один пустой элемент,
         * то открываем все клетки, рядом с этой пустой клеткой и прерываем
         * дальнейшую проверку
         */
        for (i in openingNow) {
            if (listNums[i] == 0) {
                emptyCells.add(i)
                empty()
                break
            }
        }

        /**
         * Если перебираемый список становится пустым, то добавляем все ранее неоднозначные элементы
         */
        if (indexAndNum.isEmpty()) recheck()

        /**
         * В случае, когда число проверок уже слишком большое, когда мы
         * ходим по одним и тем же элементам, но не можем найти тривиальное решение,
         * то рандомим на одну из закрытых клеток.
         * Однако в случае, когда число проверок больше 2 проходов, то идём в обратном порядке
         * до 4 проходов
         */
        if (numberOfChecks > indexAndNum.size * 4) {
            for (i in tempMap) {
                cellArea(i.first)

                for (ind in listCellArea) {
                    when (ind) {
                        in openSet -> numOpenAround++
                        in setFlags -> numberFlags++
                        else -> close.add(ind)
                    }
                }
            }
            val openingCell = close.random()
            val numInOpeningCell = listNums[openingCell]
            if (numInOpeningCell == 9) {
                openingNow.add(openingCell)
                boom = true
            }
            else {
                if (numInOpeningCell == 0) {
                    emptyCells.add(openingCell)
                    empty()
                } else {
                    openingNow.add(openingCell)
                    openSet.add(openingCell)
                    recheck.add(openingCell to listNums[openingCell])
                    indexAndNum.add(openingCell to listNums[openingCell])
                    cellsWithNums.add(openingCell)
                }
            }
        } else {
            if (numberOfChecks > indexAndNum.size * 2) {
                tempMap.clear()
                tempMap.addAll(indexAndNum.sortedByDescending { it.second; it.first })
                checkBombs()
            }
        }
    }

    /**
     * Для каждой перепроверяемой ячейки будем заново добавлять её в список
     * перебираемых элементов, однако, если она уже есть в "бесполезных" ячейках, то
     * навсегда забываем про неё
     */
    private fun recheck() {
        for (cell in recheck) {
            cellArea(cell.first)
            var numOpenAround = 0
            var numberFlags = 0

            for (i in listCellArea) {

                if (i in openSet) numOpenAround++
                else if (i in setFlags) numberFlags++

            }

            if (numOpenAround + numberFlags != listNums[cell.first]) {
                if (cell.first !in passed) {
                    indexAndNum.add(cell)
                }
            }
            else passed.add(cell.first)
        }
        recheck.clear()
    }
}