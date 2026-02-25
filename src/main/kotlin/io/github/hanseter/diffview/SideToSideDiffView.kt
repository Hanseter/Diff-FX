package io.github.hanseter.diffview

import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import org.fxmisc.richtext.CodeArea

/**
 * A control to show the diff between two versions of a text side by side.
 * This adds a TextOutline to scroll the texts.
 */
class SideToSideDiffView(leftText: String, rightText: String) : DiffViewBase() {

    private val diffBuilder = DiffRowGenerator.create()
        .showInlineDiffs(false)
        .inlineDiffByWord(true)
        .mergeOriginalRevised(false)
        .lineNormalizer { it }
        .build()

    override val diff: List<DiffRow> = diffBuilder
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
    }
    override val scrollBar = TextOutline(
        listOf(
            CodeAreaOutlineWrapper(left).apply {
                lineColorizer = { i, _ ->
                    when (diff[i].tag) {
                        DiffRow.Tag.DELETE -> cssHelper.removedLineColor.get()
                        DiffRow.Tag.CHANGE -> cssHelper.changedLineColor.get()
                        DiffRow.Tag.INSERT, DiffRow.Tag.EQUAL, null -> cssHelper.unchangedLineColor.get()
                    }
                }
            },
            CodeAreaOutlineWrapper(right).apply {
                lineColorizer = { i, _ ->
                    when (diff[i].tag) {
                        DiffRow.Tag.INSERT -> cssHelper.newLineColor.get()
                        DiffRow.Tag.CHANGE -> cssHelper.changedLineColor.get()
                        DiffRow.Tag.DELETE, DiffRow.Tag.EQUAL, null -> cssHelper.unchangedLineColor.get()
                    }
                }
            }
        ))

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
            onScrolled()
        }
        right.estimatedScrollYProperty().addListener { _, _, scroll ->
            if (!syncing) {
                syncing = true
                left.estimatedScrollYProperty().value = scroll
                syncing = false
            }
        }
    }

    private var longerTextArea: CodeArea =
        if (left.paragraphs.size > right.paragraphs.size) left else right

    override fun currentTopLine(): Int = longerTextArea.firstVisibleParToAllParIndex()
    override fun currentBottomLine(): Int = longerTextArea.lastVisibleParToAllParIndex()

    override fun scrollToPct(pct: Double) {
        longerTextArea.estimatedScrollYProperty().value =
            pct * longerTextArea.totalHeightEstimateProperty().value
    }
}
