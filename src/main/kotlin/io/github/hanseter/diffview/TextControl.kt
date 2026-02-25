package io.github.hanseter.diffview

import javafx.beans.value.ObservableValue
import javafx.scene.control.TextArea
import javafx.scene.layout.Region
import javafx.scene.paint.Paint
import org.fxmisc.richtext.CodeArea

/**
 * A wrapper interface to adapt different concrete classes like [CodeArea] or [TextArea] for the [TextOutline].
 */
interface TextControl<C : Region> {

    /**
     * The number of lines displayed in this control.
     */
    val lineCount: Int

    /**
     * The topmost line currently visible
     */
    val firstVisibleLine: Int

    /**
     * The bottommost line currently visible
     */
    val lastVisibleLine: Int

    /**
     * The displayed text.
     */
    val text: String
        get() = textProperty().value

    /**
     * The wrapped control.
     */
    val control: C

    /**
     * The height of the content of the text [control].
     */
    val contentHeight: Double

    /**
     * The displayed text.
     */
    fun textProperty(): ObservableValue<String>

    /**
     * Scrolls the text control to the provided y value in percent.
     * The value can be between 0.0, meaning the very top of the text control to 1.0, the very bottom of it.
     */
    fun scrollToYPercent(y: Double)

    /**
     * Adds a callback that should be called whenever the visible lines of the control changed.
     */
    fun addVisibleLinesChangedCallback(callback: () -> Unit)

    /**
     * Gets the color to use to draw the [line] at [index] in a [TextOutline].
     */
    fun getLineColor(line: String, index: Int): Paint


}