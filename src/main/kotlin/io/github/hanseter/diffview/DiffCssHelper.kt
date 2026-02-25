package io.github.hanseter.diffview

import javafx.beans.property.ReadOnlyObjectProperty
import javafx.css.PseudoClass
import javafx.scene.Node
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Line

class DiffCssHelper {
    private val unchangedLine = Line().apply {
        styleClass += "line"
        fill = Color.TRANSPARENT
        pseudoClassStateChanged(UNCHANGED_PSEUDO_CLASS, true)
        isManaged = false
        isVisible = false
    }

    private val newLine = Line().apply {
        styleClass += "line"
        fill = Color.GREEN
        pseudoClassStateChanged(NEW_PSEUDO_CLASS, true)
        isManaged = false
        isVisible = false
    }

    private val removedLine = Line().apply {
        styleClass += "line"
        fill = Color.RED
        pseudoClassStateChanged(REMOVED_PSEUDO_CLASS, true)
        isManaged = false
        isVisible = false
    }
    private val changedLine = Line().apply {
        styleClass += "line"
        fill = Color.ORANGE
        pseudoClassStateChanged(CHANGED_PSEUDO_CLASS, true)
        isManaged = false
        isVisible = false
    }
    private val viewPortLine = Line().apply {
        styleClass += "viewport-line"
        fill = Color.LIGHTBLUE.deriveColor(0.0, 1.0, 1.0, 0.3)
        stroke = Color.LIGHTBLUE
        isManaged = false
        isVisible = false
    }

    val node: Node =
        StackPane(changedLine, unchangedLine, newLine, removedLine, viewPortLine).apply {
            styleClass += "diff-outline"
            isManaged = false
            isVisible = false
        }

    val unchangedLineColor: ReadOnlyObjectProperty<Paint> = unchangedLine.fillProperty()
    val newLineColor: ReadOnlyObjectProperty<Paint> = newLine.fillProperty()
    val removedLineColor: ReadOnlyObjectProperty<Paint> = removedLine.fillProperty()
    val changedLineColor: ReadOnlyObjectProperty<Paint> = changedLine.fillProperty()

    val viewPortStrokeColor: ReadOnlyObjectProperty<Paint> = viewPortLine.strokeProperty()
    val viewPortFillColor: ReadOnlyObjectProperty<Paint> = viewPortLine.fillProperty()

    companion object {
        private val UNCHANGED_PSEUDO_CLASS = PseudoClass.getPseudoClass("unchanged")
        private val CHANGED_PSEUDO_CLASS = PseudoClass.getPseudoClass("changed")
        private val NEW_PSEUDO_CLASS = PseudoClass.getPseudoClass("new")
        private val REMOVED_PSEUDO_CLASS = PseudoClass.getPseudoClass("removed")
    }
}