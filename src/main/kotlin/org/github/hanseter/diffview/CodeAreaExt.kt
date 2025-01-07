@file:JvmName("CodeAreaExt")

package org.github.hanseter.diffview

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.text.TextAlignment
import org.fxmisc.richtext.CodeArea
import java.util.function.IntFunction


fun CodeArea.addLineNumbers() {
    addLineNumbers(text.count { it == '\n' } + 1)
}

fun CodeArea.addLineNumbers(lineCount: Int) {
    paragraphGraphicFactory = createParagraphFactory(lineCount.toString().length)
}

private fun createParagraphFactory(maxLen: Int) = IntFunction<Node> {
    HBox(
        Label("$it").apply {
            textAlignment = TextAlignment.RIGHT
            padding = Insets(0.0, 5.0, 0.0, 0.0)
        }
    ).apply {
        prefWidth = 10.0 * maxLen + 5
        minWidth = Region.USE_PREF_SIZE
        maxWidth = Region.USE_PREF_SIZE
        alignment = Pos.CENTER_RIGHT
        background = Background(BackgroundFill(javafx.scene.paint.Color.GRAY, null, null))
    }
}