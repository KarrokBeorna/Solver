import javafx.beans.property.SimpleIntegerProperty
import tornadofx.*

class Logics: Controller() {

    val numBombs = SimpleIntegerProperty()
    val numFlags = SimpleIntegerProperty()
    val numTrueFlags = SimpleIntegerProperty()
    val numClicks = SimpleIntegerProperty()

    class Cell(var isVisible: Boolean = false, var flag: Boolean = false, var numberOfBombs: Int,
               val aroundCell: List<Int>, val index: Int, var useless: Boolean = false)

    val listCell = mutableListOf<Cell>()
    val openingNow = mutableSetOf<Int>()
    val flagsNow = mutableSetOf<Int>()
    var boom = false

    private val openSet = mutableSetOf<Int>()
    private val emptyCells = mutableListOf<Cell>()
    private val setFlags = mutableSetOf<Int>()
    private val checkList = mutableSetOf<Cell>()
    private val recheck = mutableSetOf<Cell>()
    private var numberOfChecks = 0
    val listBombs = mutableListOf<Int>()


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
     * Значения в ячейках в зависимости от числа бомб, расположенных в радиусе 1
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
                listCell.add(Cell(isVisible = false, flag = false,
                    numberOfBombs = num, aroundCell = cellArea(i),index = i, useless = false))
            } else listCell.add(Cell(isVisible = false, flag = false,
                numberOfBombs = 9, aroundCell = emptyList(), index = i, useless = false))
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
    fun firstClick() {

        val first = listCell[112]

        openingNow.add(112)
        first.useless = true
        first.isVisible = true

        for (i in first.aroundCell) {

            val researched = listCell[i]

            if (researched.numberOfBombs == 0) {

                emptyCells.add(researched)
                openingNow.add(i)

            }
            else if (researched.numberOfBombs in 1..8) {

                openingNow.add(i)
                checkList.add(researched)
                researched.isVisible = true

            }
        }

        numClicks.value++

        if (emptyCells.size != 0) empty()

    }

    /**
     * Открытие области рядом с пустыми клетками
     */
    private fun empty() {

        while (emptyCells.size != 0) {

            val current = emptyCells.first()
            current.isVisible = true
            current.useless = true

            for (i in current.aroundCell) {

                val researched = listCell[i]

                if (researched.numberOfBombs == 0 && !researched.useless) {

                    emptyCells.add(researched)
                    openingNow.add(i)

                }
                else if (researched.numberOfBombs in 1..8 && !researched.useless) {

                    openingNow.add(i)
                    checkList.add(researched)
                    researched.isVisible = true

                }
            }

            emptyCells.removeAt(0)
        }
    }

    /**
     * Выделяем ячейку, которую рассматриваем сейчас:
     * наименьшее число бомб и наименьший индекс
     */
    fun backlight(): Cell {
        openingNow.clear()
        flagsNow.clear()
        return checkList.sortedWith(compareBy(Cell::numberOfBombs, Cell::index)).first()
    }

    fun checkBombs() {

        numClicks.value++
        val current = backlight()
        var numOpenAround = 0
        var numberFlags = 0
        val close = mutableListOf<Int>()

        /**
         * Рассматриваем ячейки вокруг исследуемой
         */
        for (i in current.aroundCell) {

            val researched = listCell[i]

            when {

                researched.isVisible -> numOpenAround++
                researched.flag -> numberFlags++
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
        if (current.aroundCell.size - numOpenAround == current.numberOfBombs) {

            setFlags.addAll(close)
            flagsNow.addAll(close)
            checkList.remove(current)
            numberOfChecks = 0
            close.forEach { listCell[it].flag = true }
            current.useless = true

        } else if (numberFlags == current.numberOfBombs) {

            openingNow.addAll(close)
            checkList.remove(current)
            current.useless = true
            numberOfChecks = 0

            for (i in close) {
                val researched = listCell[i]
                if (researched.numberOfBombs != 0) {
                    checkList.add(researched)
                    researched.isVisible = true
                } else {
                    emptyCells.add(researched)
                }
            }

        } else {
            checkList.remove(current)
            recheck.add(current)
            numberOfChecks++
        }

        numFlags.value = setFlags.size

        if (setFlags.all { it in listBombs }) numTrueFlags.value = setFlags.size else numTrueFlags.value = 0

        if (emptyCells.isNotEmpty()) empty()

        /**
         * Если перебираемый список становится пустым, то добавляем все ранее неоднозначные элементы
         */
        if (checkList.isEmpty()) recheck()
    }

    /**
     * Для каждой перепроверяемой ячейки будем заново добавлять её в список
     * перебираемых элементов, однако, если она уже есть в "бесполезных" ячейках, то
     * навсегда забываем про неё
     */
    private fun recheck() {
        recheck.forEach{ if(!it.useless) checkList.add(it) }
        recheck.clear()
    }
}


/**
 * !!!NEED!!!
 * открывание по порядку, без прохода до конца: то есть поставили флаг и просмотрели все цифры рядом с этой ячейкой и перебрали их
 * solver для сложных случаев
 */






/**
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
        */