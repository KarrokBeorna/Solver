import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import tornadofx.*

class Screen: Fragment() {
    override val root = Pane()

    private val logics: Logics by inject()

    private val listCells = mutableListOf<ImageView>()
    private val frontCells = mutableListOf<Label>()
    private val lights = mutableListOf<ImageView>()
    private val flags = mutableListOf<ImageView>()

    private val start = label("Начало игры") {
        translateY = 235.0
        translateX = 120.0
        prefWidth = 250.0
        prefHeight = 121.0
        alignment = Pos.CENTER
        style {
            backgroundColor += Color.GREENYELLOW
            fontSize = 30.px
        }
        isVisible = true
        setOnMouseClicked {
            logics.elements()
            mines.isVisible = true
            mines.bind(logics.numBombs)
            checkbox.isVisible = true
            trueCheckbox.isVisible = true
            clicks.isVisible = true
            board.isVisible = true
            nextnext.isVisible = true
            next.isVisible = true
            fastnext.isVisible = true
            cellImage()
            closingCells()
            backlight()
            flags()
            imageTheEnd()
            isVisible = false
        }
    }

    private val mines = label {
        translateY = 12.0
        translateX = 20.0
        prefWidth = 100.0
        prefHeight = 50.0
        alignment = Pos.CENTER
        style {
            backgroundColor += c("#929ae9")
            fontSize = 18.px
        }
        isVisible = false
        tooltip("Кол-во оставшихся мин") {
            font = Font.font(13.0)
        }
    }
    private val checkbox = label {
        translateY = 12.0
        translateX = 140.0
        prefWidth = 100.0
        prefHeight = 50.0
        alignment = Pos.CENTER
        style {
            backgroundColor += c("#68b300")
            fontSize = 18.px
        }
        isVisible = false
        tooltip("Кол-во проставленных флажков") {
            font = Font.font(13.0)
        }
        bind(logics.numFlags)
    }
    private val trueCheckbox = label {
        translateY = 12.0
        translateX = 250.0
        prefWidth = 100.0
        prefHeight = 50.0
        alignment = Pos.CENTER
        style {
            backgroundColor += c("#68b300")
            fontSize = 18.px
        }
        isVisible = false
        tooltip("Кол-во верно проставленных флажков") {
            font = Font.font(13.0)
        }
        bind(logics.numTrueFlags)
    }
    private val clicks = label {
        translateY = 12.0
        translateX = 370.0
        prefWidth = 100.0
        prefHeight = 50.0
        alignment = Pos.CENTER
        style {
            backgroundColor += c("#929ae9")
            fontSize = 18.px
        }
        isVisible = false
        tooltip("Кол-во действий") {
            font = Font.font(13.0)
        }
        bind(logics.numClicks)
    }

    private val board = label {
        translateY = 74.0
        translateX = 20.0
        prefWidth = 450.0
        prefHeight = 450.0
        style {
            backgroundColor += Color.BLACK
        }
        isVisible = false
    }

    private val nextnext = imageview("/icons/nextnext2.2.png") {
        translateY = 535.0 //524 - up 546 - down
        translateX = 235.0
        isVisible = false
        tooltip(" Решатель сложных случаев ") {
            font = Font.font(13.0)
        }
        setOnMouseClicked {

            if (logics.solverState == -1) {
                lights.forEach { it.isVisible = false }

                logics.researchedClosed()
            } else if (logics.solverState == logics.stop || logics.stop == 500 || 
                logics.openingNow.isNotEmpty() || logics.flagsNow.isNotEmpty()) {

                logics.update()
                val act = logics.randOrAct()

                act.first.forEach { frontCells[it].isVisible = false; lights[it].isVisible = true }
                act.second.forEach { flags[it].isVisible = true; lights[it].isVisible = true }

                processTheEnd()

            }
        }
        setOnMouseMoved {
            if (logics.solverState in 0 until logics.stop && logics.openingNow.isEmpty() && logics.flagsNow.isEmpty()) {

                lights.forEach { it.isVisible = false }

                val act = logics.solve()

                act.forEach { lights[it.index].isVisible = true }
            }
        }
    }

    private fun acts() {
        if (logics.numTrueFlags.value < logics.listBombs.size) {

            lights.forEach { it.isVisible = false }

            val act = if (logics.numClicks.value == 0) {
                logics.firstClick()
            } else {
                logics.checkBombs()
            }

            lights[act.index].isVisible = true

            for (i in act.opening) {
                frontCells[i].isVisible = false
            }

            for (i in act.flagging) {
                flags[i].isVisible = true
            }
        }
        processTheEnd()
    }

    private val next = imageview("/icons/next2.2.png") {
        translateY = 535.0 //524 - up 546 - down
        translateX = 380.0
        isVisible = false
        tooltip(" Следующий ход решателя ") {
            font = Font.font(13.0)
        }
        setOnMouseClicked {
            if (logics.numberOfChecks <= 2 * (logics.checkList.size + logics.recheck.size) && logics.solverState == -2) {
                acts()
            } else logics.solverState = -1
        }
    }

    private val fastnext = imageview("/icons/fastnextnext2.png") {
        translateY = 535.0 //524 - up 546 - down
        translateX = 65.0
        isVisible = false
        tooltip(" Быстрое решение при наводке и движении ") {
            font = Font.font(13.0)
        }
        setOnMouseMoved {
            if (logics.numberOfChecks <= 2 * (logics.checkList.size + logics.recheck.size) && logics.solverState == -2) {
                acts()
            } else logics.solverState = -1
        }
    }

    private val opening = imageview("/icons/opening.png") {
        translateX = 0.0
        translateY = 524.0
        isVisible = false

        setOnMouseClicked {
            frontCells.forEach { it.isVisible = false }
            flags.forEach { it.isVisible = false }
        }
    }

    private fun closingCells() {
        for (i in 0..224) {
            val cell = label {
                translateY = 75.0 + (i / 15) * 30          //74 - board, 75-1st, 105-2nd
                translateX = 21.0 + (i % 15) * 30
                prefWidth = 28.0
                prefHeight = 28.0
                style {
                    backgroundColor += Color.LIGHTBLUE
                }
                isVisible = true
            }
            frontCells.add(cell)
        }
    }

    private fun cellImage() {
        for (i in 0..224) {
            val cell = imageview("/icons/${logics.board[i].numberOfBombs}.png") {
                translateY = 75.0 + (i / 15) * 30          //74 - board, 75-1st, 105-2nd
                translateX = 21.0 + (i % 15) * 30
                isVisible = true
            }
            listCells.add(cell)
        }
    }

    private fun backlight() {
        for (i in 0..224) {
            val light = imageview("/icons/backlight.png") {
                translateY = 74.0 + (i / 15) * 30          //74 - board, 75-1st, 105-2nd
                translateX = 20.0 + (i % 15) * 30
                isVisible = false
            }
            lights.add(light)
        }
    }

    private fun flags() {
        for (i in 0..224) {
            val checkbox = imageview("/icons/checkbox.png") {
                translateY = 75.0 + (i / 15) * 30          //74 - board, 75-1st, 105-2nd
                translateX = 21.0 + (i % 15) * 30
                isVisible = false
            }
            flags.add(checkbox)
        }
    }

    private val theEnd = mutableListOf<ImageView>()
    private fun imageTheEnd() {
        val win = imageview("/icons/Win.png") {
            translateX = 0.0
            translateY = 0.0
            isVisible = false
        }
        theEnd.add(win)
        val lose = imageview("/icons/Lose.png") {
            translateX = 0.0
            translateY = 0.0
            isVisible = false
        }
        theEnd.add(lose)
        val what = imageview ("/icons/What.png") {
            translateX = 330.0
            translateY = 70.0
            style {
                backgroundColor += Color.LIGHTGREEN
                fontSize = 18.px
            }
            isVisible = false
            setOnMouseClicked {
                theEnd.forEach { it.isVisible = false }
                opening.isVisible = true
                theEnd[3].isVisible = true
                checkbox.isVisible = false
                trueCheckbox.isVisible = false
            }
        }
        theEnd.add(what)
        val restart = imageview("/icons/restart.png") {
            translateX = 213.0
            translateY = 4.0
            isVisible = false
            setOnMouseClicked {
                logics.restart()

                listCells.forEach { it.isVisible = false }
                listCells.clear()
                cellImage()
                frontCells.clear()
                flags.clear()
                lights.forEach { it.isVisible = false }
                lights.clear()
                theEnd.forEach { it.isVisible = false }
                theEnd.clear()


                mines.bind(logics.numBombs)
                isVisible = false
                checkbox.isVisible = true
                trueCheckbox.isVisible = true
                opening.isVisible = false
                closingCells()
                backlight()
                flags()
                imageTheEnd()
            }
        }
        theEnd.add(restart)
    }
    private fun theEnd(): Int {
        if (logics.numTrueFlags.value == logics.listBombs.size) return 0
        else {
            if (logics.numTrueFlags.value != logics.numFlags.value || logics.boom ||
                logics.numFlags.value > logics.listBombs.size) return 1
        }
        return 2
    }
    private fun processTheEnd() {
        val end = theEnd()
        if (end == 0) {
            theEnd[2].isVisible = true
            theEnd[0].isVisible = true
        }
        else if (end == 1) {
            theEnd[2].isVisible = true
            theEnd[1].isVisible = true
        }
    }
}