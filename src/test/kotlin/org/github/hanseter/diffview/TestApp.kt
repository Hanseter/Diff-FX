package org.github.hanseter.diffview

import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.HorizontalDirection
import javafx.scene.Scene
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.stage.Stage
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.GenericStyledArea
import org.fxmisc.richtext.model.Paragraph
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import org.reactfx.collection.ListModification
import java.util.function.Consumer
import java.util.regex.Pattern
import kotlin.math.min


fun main(args: Array<String>) {
    Application.launch(TestApp::class.java, *args)
}

class TestApp : Application() {
    val mainNode =
        InPlaceDiffView(
            "abc\n123\ndef abc jik\nabc\n" +
                    "123\n" +
                    "def\nabc\n" +
                    "123\n" +
                    "def\nabc\n" +
                    "123\n" +
                    "def\nabc\n" +
                    "123\n" +
                    "def\n" +
                    "def\n" +
                    "def\n",
            "123\n" +
                    "abc\n123\ndefg abc hij\nabc\n" +
                    "123\n" +
                    "def\nabc\n" +
                    "123\n" +
                    "def\nabc\n" +
                    "123\n" +
                    "def\nabc\n" +
                    "123\n" +
                    "def\n"
        ).node
//        TextOutline(
//        CodeArea(
//            this.javaClass.classLoader.getResourceAsStream("TestFile.java").bufferedReader().readText()
//        ).apply {
//            addLineNumbers()
//            visibleParagraphs.addModificationObserver(
//                VisibleParagraphStyler(this, this@TestApp::computeHighlighting)
//            )
//            stylesheets.add(TestApp::class.java.classLoader.getResource("java-keywords.css").toExternalForm())
//        }).apply {
//        side = HorizontalDirection.RIGHT
//    }.node
//        SyncedCodeAreas(
//        "abc\n123\ndef\nabc\n" +
//                "123\n" +
//                "def\nabc\n" +
//                "123\n" +
//                "def\nabc\n" +
//                "123\n" +
//                "def\nabc\n" +
//                "123\n" +
//                "def\n",
//        "abc\n123\ndefg\nabc\n" +
//                "123\n" +
//                "def\nabc\n" +
//                "123\n" +
//                "def\nabc\n" +
//                "123\n" +
//                "def\nabc\n" +
//                "123\n" +
//                "def\n"
//    ).mainPane

    override fun start(stage: Stage) {
        stage.scene = Scene(mainNode, 400.0, 400.0)
        stage.show()
    }

    private fun computeHighlighting(text: String): StyleSpans<Collection<String>> {
        val matcher = PATTERN.matcher(text)
        var lastKwEnd = 0
        val spansBuilder = StyleSpansBuilder<Collection<String>>()
        while (matcher.find()) {
            val styleClass = checkNotNull(
                if (matcher.group("KEYWORD") != null) "keyword" else if (matcher.group("PAREN") != null) "paren" else if (matcher.group(
                        "BRACE"
                    ) != null
                ) "brace" else if (matcher.group("BRACKET") != null) "bracket" else if (matcher.group("SEMICOLON") != null) "semicolon" else if (matcher.group(
                        "STRING"
                    ) != null
                ) "string" else if (matcher.group("COMMENT") != null) "comment" else null
            ) /* never happens */
            spansBuilder.add(emptyList(), matcher.start() - lastKwEnd)
            spansBuilder.add(listOf(styleClass), matcher.end() - matcher.start())
            lastKwEnd = matcher.end()
        }
        spansBuilder.add(emptyList(), text.length - lastKwEnd)
        return spansBuilder.create()
    }

    private class VisibleParagraphStyler<PS, SEG, S>(
        private val area: GenericStyledArea<PS, SEG, S>,
        private val computeStyles: java.util.function.Function<String, StyleSpans<S>>
    ) :
        Consumer<ListModification<out Paragraph<PS, SEG, S>>> {
        private var prevParagraph = 0
        private var prevTextLength = 0

        override fun accept(lm: ListModification<out Paragraph<PS, SEG, S>>) {
            if (lm.addedSize > 0) Platform.runLater {
                val paragraph = min(
                    (area.firstVisibleParToAllParIndex() + lm.from).toDouble(),
                    (area.paragraphs.size - 1).toDouble()
                ).toInt()
                val text = area.getText(paragraph, 0, paragraph, area.getParagraphLength(paragraph))
                if (paragraph != prevParagraph || text.length != prevTextLength) {
                    if (paragraph < area.paragraphs.size - 1) {
                        val startPos = area.getAbsolutePosition(paragraph, 0)
                        area.setStyleSpans(startPos, computeStyles.apply(text))
                    }
                    prevTextLength = text.length
                    prevParagraph = paragraph
                }
            }
        }
    }

    companion object {
        val KEYWORDS: Array<String> = arrayOf(
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
        )

        val KEYWORD_PATTERN: String = "\\b(" + java.lang.String.join("|", *KEYWORDS) + ")\\b"
        const val PAREN_PATTERN: String = "\\(|\\)"
        const val BRACE_PATTERN: String = "\\{|\\}"
        const val BRACKET_PATTERN: String = "\\[|\\]"
        const val SEMICOLON_PATTERN: String = "\\;"
        const val STRING_PATTERN: String = "\"([^\"\\\\]|\\\\.)*\""
        const val COMMENT_PATTERN: String =
            ("//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/" // for whole text processing (text blocks)
                    + "|" + "/\\*[^\\v]*" + "|" + "^\\h*\\*([^\\v]*|/)") // for visible paragraph processing (line by line)

        val PATTERN: Pattern = Pattern.compile(
            ("(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")")
        )
    }

}