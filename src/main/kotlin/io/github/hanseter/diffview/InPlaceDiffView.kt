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
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.awt.SystemColor.control

/**
 * A control to show the difference between two texts inline in one [CodeArea].
 */
class InPlaceDiffView(
    leftText: String,
    rightText: String
) : DiffViewBase() {

    private val diffBuilder = DiffRowGenerator.create()
        .showInlineDiffs(true)
        .inlineDiffByWord(true)
        .mergeOriginalRevised(true)
        .oldTag { _ -> "---" }
        .newTag { _ -> "+++" }
        .lineNormalizer { it }
        .build()

    override val diff: List<DiffRow> = diffBuilder
        .generateDiffRows(leftText.lines(), rightText.lines())

    private val textArea = CodeArea(diff.joinToString("\n") { it.oldLine }).apply {
        HBox.setHgrow(this, Priority.ALWAYS)
        isEditable = false
        stylesheets.add(InPlaceDiffView::class.java.getResource("diff.css")!!.toString())
        setStyleSpans(0, createStyleSpans())
        diff.forEachIndexed { i, row ->
            val style = when (row.tag) {
                DiffRow.Tag.INSERT -> "added"
                DiffRow.Tag.DELETE -> "removed"
                DiffRow.Tag.CHANGE -> "changed"
                DiffRow.Tag.EQUAL, null -> return@forEachIndexed
            }
            setParagraphStyle(i, listOf(style))
        }
        addLineNumbers()
        estimatedScrollYProperty().addListener { _, _, _ -> onScrolled() }
    }

    override val scrollBar = TextOutline(
        listOf(CodeAreaOutlineWrapper(textArea).apply {
            lineColorizer = { i, _ ->
                when (diff[i].tag) {
                    DiffRow.Tag.INSERT -> cssHelper.newLineColor.get()
                    DiffRow.Tag.DELETE -> cssHelper.removedLineColor.get()
                    DiffRow.Tag.CHANGE -> cssHelper.changedLineColor.get()
                    DiffRow.Tag.EQUAL, null -> cssHelper.unchangedLineColor.get()
                }
            }
        })
    )

    private fun createStyleSpans(): StyleSpans<Collection<String>> {
        val spansBuilder = StyleSpansBuilder<Collection<String>>()
        var length = 0
        diff.forEach {
            when (it.tag) {
                DiffRow.Tag.INSERT -> {
                    addToSpans(length, spansBuilder, "added", it)
                    length = 0
                }

                DiffRow.Tag.DELETE -> {
                    addToSpans(length, spansBuilder, "removed", it)
                    length = 0
                }

                DiffRow.Tag.CHANGE -> {
                    addToSpans(length, spansBuilder, "changed", it)
                    length = 0
                }

                DiffRow.Tag.EQUAL, null -> length += it.oldLine.length + 1
            }
        }
        if (length > 0) {
            spansBuilder.add(emptyList(), length)
        }
        return spansBuilder.create()
    }

    private fun addToSpans(
        length: Int,
        spansBuilder: StyleSpansBuilder<Collection<String>>,
        type: String,
        it: DiffRow
    ) {
        if (length > 0) {
            spansBuilder.add(emptyList(), length)
        }
        spansBuilder.add(listOf(type), it.oldLine.length + 1)
    }

    override fun currentTopLine(): Int = textArea.firstVisibleParToAllParIndex()
    override fun currentBottomLine(): Int = textArea.lastVisibleParToAllParIndex()

    override fun scrollToPct(pct: Double) {
        textArea.estimatedScrollYProperty().value =
            pct * textArea.totalHeightEstimateProperty().value
    }
}