import javafx.scene.layout.Pane
import javafx.stage.Stage
import tornadofx.*


class Solver : View("Solver") {
    override val root = Pane(Screen().root)
}

class Application : App(Solver::class) {
    override fun start(stage: Stage) {
        stage.minHeight = 630.0
        stage.minWidth = 506.0
        super.start(stage)
    }
}