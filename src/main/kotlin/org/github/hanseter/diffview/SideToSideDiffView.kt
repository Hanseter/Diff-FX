package org.github.hanseter.diffview

import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import org.fxmisc.richtext.CodeArea

/**
 * A control to show the diff between two versions of a text side by side.
 * This adds a TextOutline to scroll the texts.
 */
class SideToSideDiffView(leftText: String, rightText: String) {

    private val diffBuilder = DiffRowGenerator.create()
        .showInlineDiffs(false)
        .inlineDiffByWord(true)
        .mergeOriginalRevised(false)
        .build()

    private val diff = diffBuilder
        .generateDiffRows(leftText.lines(), rightText.lines())

    private val left = CodeArea(diff.joinToString("\n") { it.oldLine }).apply {
        HBox.setHgrow(this, Priority.ALWAYS)
        isEditable = false
        addLineNumbers()
        stylesheets.add(InPlaceDiffView::class.java.getResource("diff.css")!!.toString())
        diff.forEachIndexed { i, row ->
            val style = when (row.tag) {
                DiffRow.Tag.DELETE -> "removed"
                DiffRow.Tag.CHANGE -> "changed"
                DiffRow.Tag.INSERT, DiffRow.Tag.EQUAL, null -> return@forEachIndexed
            }
            setParagraphStyle(i, listOf(style))
        }
    }
    private val right = CodeArea(diff.joinToString("\n") { it.newLine }).apply {
        HBox.setHgrow(this, Priority.ALWAYS)
        isEditable = false
        addLineNumbers()
        stylesheets.add(InPlaceDiffView::class.java.getResource("diff.css")!!.toString())
        diff.forEachIndexed { i, row ->
            val style = when (row.tag) {
                DiffRow.Tag.INSERT -> "added"
                DiffRow.Tag.CHANGE -> "changed"
                DiffRow.Tag.DELETE, DiffRow.Tag.EQUAL, null -> return@forEachIndexed
            }
            setParagraphStyle(i, listOf(style))
        }
        estimatedScrollYProperty().addListener { _, _, y ->
            //this makes absolutely no sense but manually scrolling still works fine this way
            //and it's the only way I found to scroll to the top initially
            estimatedScrollYProperty().value = 0.0
        }
    }
    private val scrollBar = TextOutline(
        listOf(
            CodeAreaOutlineWrapper(left).apply {
                lineColorizer = { i, _ ->
                    when (diff[i].tag) {
                        DiffRow.Tag.DELETE -> Color.RED
                        DiffRow.Tag.CHANGE -> Color.ORANGE
                        DiffRow.Tag.INSERT, DiffRow.Tag.EQUAL, null -> Color.GRAY
                    }
                }
            },
            CodeAreaOutlineWrapper(right).apply {
                lineColorizer = { i, _ ->
                    when (diff[i].tag) {
                        DiffRow.Tag.INSERT -> Color.GREEN
                        DiffRow.Tag.CHANGE -> Color.ORANGE
                        DiffRow.Tag.DELETE, DiffRow.Tag.EQUAL, null -> Color.GRAY
                    }
                }
            }
        ))
    val node = scrollBar.node

    init {
        keepScrollInSync()
    }

    private fun keepScrollInSync() {
        var syncing = false
        // Cannot use left.estimatedScrollYProperty().bindBidirectional(right.estimatedScrollYProperty())
        // as it leads to infinite wobbling when scrolling. Most likely because the double values are never quite the same
        left.estimatedScrollYProperty().addListener { _, _, scroll ->
            if (!syncing) {
                syncing = true
                right.estimatedScrollYProperty().value = scroll
                syncing = false
            }
        }
        right.estimatedScrollYProperty().addListener { _, _, scroll ->
            if (!syncing) {
                syncing = true
                left.estimatedScrollYProperty().value = scroll
                syncing = false
            }
        }
    }
}
