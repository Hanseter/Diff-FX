package org.github.hanseter.diffview

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.HorizontalDirection
import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap
import org.fxmisc.richtext.CodeArea

class TextOutline(private val codeArea: CodeArea) {

    private val _sideProperty: ObjectProperty<HorizontalDirection> =
        SimpleObjectProperty(HorizontalDirection.LEFT).apply {
            addListener { _, _, side ->
                when (side) {
                    HorizontalDirection.LEFT -> node.children.setAll(scrollbar, codeArea)
                    HorizontalDirection.RIGHT -> node.children.setAll(codeArea, scrollbar)
                }
            }
        }

    var side: HorizontalDirection
        get() = _sideProperty.get()
        set(value) = _sideProperty.set(value)

    fun sideProperty() = _sideProperty

    var lineColorizer: (Int, String) -> Color = { _, _ -> Color.GRAY }

    private val scrollbar = Canvas().apply {
        width = 100.0
        heightProperty().bind(codeArea.heightProperty())
        layoutBoundsProperty().addListener { _, _, _ -> draw() }
        setOnMouseMoved(::onMouseMoved)
        setOnMousePressed(::onMousePressed)
        setOnMouseClicked(::onMouseClicked)
        setOnMouseDragged(::onMouseDragged)
        setOnMouseReleased(::onMouseReleased)
    }
    private var mousePressYInRange = -1.0

    private var visibleRange = Double.MAX_VALUE.rangeTo(Double.MAX_VALUE)

    val node = HBox(scrollbar, codeArea)

    init {
        codeArea.apply {
            HBox.setHgrow(this, Priority.ALWAYS)
            textProperty().addListener { _, _, _ -> draw() }
            visibleParagraphs.addChangeObserver { draw() }
        }
        draw()
    }

    private fun draw() {
        val lines = codeArea.text.lines()
        val heightPerLine = scrollbar.height / (lines.size + 1)
        val ctx = scrollbar.graphicsContext2D
        ctx.lineCap = StrokeLineCap.ROUND
        ctx.lineWidth = (heightPerLine / 2).coerceAtMost(5.0)
        ctx.clearRect(0.0, 0.0, scrollbar.width, scrollbar.height)
        drawLines(ctx, lines, heightPerLine)
        drawVisibleTextIndiciator(ctx)
    }

    private fun drawLines(
        ctx: GraphicsContext,
        lines: List<String>,
        heightPerLine: Double
    ) {
        val maxLineWidth = lines.maxOf { it.length }.coerceAtMost(500)
        val widthPerChar = scrollbar.width / (maxLineWidth + 1)
        var linePos = heightPerLine / 2
        lines.forEachIndexed { i, line ->
            ctx.stroke = lineColorizer(i, line)
            var from = widthPerChar / 2
            var to = from
            line.forEach { char ->
                if (char.isWhitespace()) {
                    if (from != to) {
                        ctx.strokeLine(from, linePos, to, linePos)
                    }
                    from = to + widthPerChar
                    ctx.moveTo(from, linePos)
                }
                to += widthPerChar
            }
            if (from != to) {
                ctx.strokeLine(from, linePos, to, linePos)
            }
            linePos += heightPerLine
        }
    }

    private fun drawVisibleTextIndiciator(ctx: GraphicsContext) {
        ctx.fill = Color.LIGHTBLUE.deriveColor(0.0, 1.0, 1.0, 0.3)
        ctx.stroke = Color.LIGHTBLUE
        val lineSize = codeArea.paragraphs.size
        val startPct = codeArea.firstVisibleParToAllParIndex() / lineSize.toDouble()
        val endPct = codeArea.lastVisibleParToAllParIndex() / lineSize.toDouble()
        val startY = scrollbar.height * startPct
        val endY = scrollbar.height * endPct
        visibleRange = startY..endY
        ctx.fillRect(0.0, startY, scrollbar.width, endY - startY)
        ctx.strokeLine(0.0, startY, scrollbar.width, startY)
        ctx.strokeLine(0.0, endY, scrollbar.width, endY)
    }

    private fun onMouseMoved(e: MouseEvent) {
        scrollbar.cursor = if (e.y in visibleRange) Cursor.V_RESIZE else Cursor.HAND
    }

    private fun onMousePressed(e: MouseEvent) {
        mousePressYInRange = e.y - visibleRange.start
    }

    private fun onMouseClicked(e: MouseEvent) {
        if (e.y in visibleRange) {
            mousePressYInRange = -1.0
        } else {
            val rangeMidOffset = (visibleRange.endInclusive - visibleRange.start) / 2
            moveViewPortTo((e.y - rangeMidOffset))
        }
    }

    private fun moveViewPortTo(y: Double) {
        val pct = y / scrollbar.height
        codeArea.estimatedScrollYProperty().value = pct * codeArea.totalHeightEstimateProperty().value
        scrollbar.cursor = Cursor.V_RESIZE
    }

    private fun onMouseDragged(e: MouseEvent) {
        if (mousePressYInRange == -1.0) {
            mousePressYInRange = e.y - visibleRange.start
            return
        }
        moveViewPortTo(e.y - mousePressYInRange)
    }

    private fun onMouseReleased(e: MouseEvent) {
        mousePressYInRange = -1.0
    }
}