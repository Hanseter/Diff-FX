package io.github.hanseter.diffview

import javafx.beans.value.ObservableValue
import javafx.scene.paint.Color
import org.fxmisc.richtext.CodeArea

/**
 * A wrapper to make [CodeArea]s usable in the [TextOutline].
 */
class CodeAreaOutlineWrapper(override val control: CodeArea) : TextControl<CodeArea> {

    /**
     * Used to colorize lines differently in the [TextOutline]
     */
    var lineColorizer: (Int, String) -> Color = { _, _ -> Color.GRAY }

    override val lineCount: Int
        get() = control.paragraphs.size

    override val firstVisibleLine: Int
        get() = control.firstVisibleParToAllParIndex()

    override val lastVisibleLine: Int
        get() = control.lastVisibleParToAllParIndex()

    override fun textProperty(): ObservableValue<String> = control.textProperty()

    override fun scrollToYPercent(y: Double) {
        control.estimatedScrollYProperty().value = y * control.totalHeightEstimateProperty().value
    }

    override fun addVisibleLinesChangedCallback(callback: () -> Unit) {
        control.visibleParagraphs.addChangeObserver { callback() }
    }
    override fun getLineColor(line: String, index: Int): Color = lineColorizer(index, line)
}