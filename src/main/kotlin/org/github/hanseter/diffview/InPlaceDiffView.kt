package org.github.hanseter.diffview

import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder

class InPlaceDiffView(
    leftText: String,
    rightText: String
) {
    private val diffBuilder = DiffRowGenerator.create()
        .showInlineDiffs(true)
        .inlineDiffByWord(true)
        .mergeOriginalRevised(true)
        .oldTag { _ -> "---" }
        .newTag { _ -> "+++" }
        .build()

    private val diff = diffBuilder
        .generateDiffRows(leftText.lines(), rightText.lines())

    private val textArea = CodeArea(diff.joinToString("\n") { it.oldLine }).apply {
        HBox.setHgrow(this, Priority.ALWAYS)
        isEditable = false
        stylesheets.add(InPlaceDiffView::class.java.getResource("InPlaceDiff.css")!!.toString())
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
    }

    val scrollBar = TextOutline(
        listOf(CodeAreaOutlineWrapper(textArea).apply {
            lineColorizer = { i, _ ->
                when (diff[i].tag) {
                    DiffRow.Tag.INSERT -> Color.GREEN
                    DiffRow.Tag.DELETE -> Color.RED
                    DiffRow.Tag.CHANGE -> Color.ORANGE
                    DiffRow.Tag.EQUAL, null -> Color.GRAY
                }
            }
        })
    )

    val node = scrollBar.node

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

}