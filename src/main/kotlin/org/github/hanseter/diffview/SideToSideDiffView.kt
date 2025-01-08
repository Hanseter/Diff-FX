package org.github.hanseter.diffview

import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import org.fxmisc.richtext.CodeArea

class SideToSideDiffView(
    private var leftText: String,
    private var rightText: String
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
