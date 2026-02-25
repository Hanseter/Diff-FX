package io.github.hanseter.diffview

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import kotlin.random.Random


fun main(args: Array<String>) {
    Application.launch(TestApp::class.java, *args)
}

class TestApp : Application() {
    private val leftText = (0..20).joinToString("\n") {
        if (Random.nextInt(100) < 1) "<Hello Moon>"
        else "Hello World"
    }
    private val rightText = (0..20).joinToString("\n") {
        if (Random.nextInt(100) < 1) "<Hello Moon>"
        else "Hello World"
    }

    private var diffView: DiffViewBase? = null

    private val cb: ComboBox<String> = ComboBox<String>().apply {
        items.addAll("Side by Side", "Inplace")
        selectionModel.selectedIndexProperty().addListener { _, _, selected ->
            stackPane.children.clear()
            val toAdd = if (selected == 0) SideToSideDiffView(leftText, rightText)
            else InPlaceDiffView(leftText, rightText)
            diffView = toAdd
            thicknessText.text.toDoubleOrNull()?.let { toAdd.minLineThickness = it }
            widthText.text.toDoubleOrNull()?.let { toAdd.textOutlineWidth = it }
            VBox.setVgrow(toAdd.node, Priority.ALWAYS)
            stackPane.children.add(toAdd.node)
        }
    }

    private val cssCb = ComboBox<String>().apply {
        items.addAll("sidebar1.css", "sidebar2.css")
        selectionModel.selectedItemProperty().addListener { _, old, selected ->
            if (old != null) stackPane.stylesheets.remove(
                TestApp::class.java.classLoader.getResource(
                    old
                ).toExternalForm().toString()
            )
            stackPane.stylesheets.add(
                TestApp::class.java.classLoader.getResource(
                    selected
                ).toExternalForm().toString()
            )
        }
    }

    val thicknessText = TextField("0.5").apply {
        textProperty().addListener { _, _, value ->
            val thickness = value.toDoubleOrNull() ?: return@addListener
            diffView?.minLineThickness = thickness
        }
    }

    val scrollNextButton = Button("Next").apply {
        setOnAction { diffView?.scrollToNextDiff() }
    }

    val scrollPrevButton = Button("Prev").apply {
        setOnAction { diffView?.scrollToPreviousDiff() }
    }
    val widthText = TextField("100").apply {
        textProperty().addListener { _, _, value ->
            val width = value.toDoubleOrNull() ?: return@addListener
            diffView?.textOutlineWidth = width
        }
    }

    private val stackPane = StackPane().apply {
        VBox.setVgrow(this, Priority.ALWAYS)
    }

    val mainNode =
        VBox(
            HBox(cb, cssCb, thicknessText, scrollNextButton, scrollPrevButton, widthText),
            stackPane
        )

    override fun start(stage: Stage) {
        cb.selectionModel.selectFirst()
        stage.scene = Scene(mainNode, 400.0, 200.0)
        stage.isMaximized = true
        stage.show()
    }

}