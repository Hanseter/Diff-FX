package io.github.hanseter.diffview

import javafx.beans.value.ObservableValue
import javafx.scene.control.ScrollPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.CodeArea

/**
 * A wrapper to make [CodeArea]s usable in the [TextOutline].
 */
class CodeAreaOutlineWrapper(control: CodeArea) : TextControl<VirtualizedScrollPane<CodeArea>> {

    override val control: VirtualizedScrollPane<CodeArea> = VirtualizedScrollPane(control).apply {
        vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        HBox.setHgrow(this, Priority.ALWAYS)
    }

    /**
     * Used to colorize lines differently in the [TextOutline]
     */
    var lineColorizer: (Int, String) -> Paint = { _, _ -> Color.GRAY }

    override val lineCount: Int
        get() = control.content.paragraphs.size

    override val firstVisibleLine: Int
        get() = control.content.firstVisibleParToAllParIndex()

    override val lastVisibleLine: Int
        get() = control.content.lastVisibleParToAllParIndex()

    override val contentHeight: Double
        get() = control.totalHeightEstimate



    override fun textProperty(): ObservableValue<String> = control.content.textProperty()

    override fun scrollToYPercent(y: Double) {
        control.estimatedScrollYProperty().value = y * control.totalHeightEstimateProperty().value
    }

    override fun addVisibleLinesChangedCallback(callback: () -> Unit) {
        control.content.visibleParagraphs.addChangeObserver { callback() }
    }

    override fun getLineColor(line: String, index: Int): Paint = lineColorizer(index, line)
}