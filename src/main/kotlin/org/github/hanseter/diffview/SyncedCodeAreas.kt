package org.github.hanseter.diffview

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.text.TextAlignment
import org.fxmisc.richtext.CodeArea
import java.awt.Color
import java.util.function.IntFunction

class SyncedCodeAreas(
    private val leftText: String,
    private val rightText: String,
) {


    private val left = CodeArea(leftText).apply {
        HBox.setHgrow(this, Priority.ALWAYS)
        setParagraphGraphicFactory { Label("$it") }
    }
    private val right = CodeArea(rightText).apply {
        HBox.setHgrow(this, Priority.ALWAYS)
        isEditable = false
        addLineNumbers()
    }
    val mainPane = HBox(left, right)


}
