package org.github.hanseter.diffview

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.ComboBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.Stage


fun main(args: Array<String>) {
    Application.launch(TestApp::class.java, *args)
}

class TestApp : Application() {
    private val leftText = "abc\n123\ndef abc jik\nabc\n" +
            "123\n" +
            "def\nabc\n" +
            "123\n" +
            "def\nabc\n" +
            "123\n" +
            "def\nabc\n" +
            "123\n" +
            "def\n" +
            "def\n" +
            "def\n"
    private val rightText = "123\n" +
            "abc\n123\ndefg abc hij\nabc\n" +
            "123\n" +
            "def\nabc\n" +
            "123\n" +
            "def\nabc\n" +
            "123\n" +
            "def\nabc\n" +
            "123\n" +
            "def\n"

    private val cb: ComboBox<String> = ComboBox<String>().apply {
        items.addAll("Side by Side", "Inplace")
        selectionModel.selectedIndexProperty().addListener { _, _, selected ->
            stackPane.children.clear()
            val toAdd = if (selected == 0) SideToSideDiffView(leftText, rightText).node
            else InPlaceDiffView(leftText, rightText).node
            VBox.setVgrow(toAdd, Priority.ALWAYS)
            stackPane.children.add(toAdd)
        }
    }

    private val stackPane = StackPane().apply {
        VBox.setVgrow(this, Priority.ALWAYS)
    }

    val mainNode = VBox(cb, stackPane)

    override fun start(stage: Stage) {
        cb.selectionModel.selectFirst()
        stage.scene = Scene(mainNode, 400.0, 400.0)
        stage.show()
    }

}