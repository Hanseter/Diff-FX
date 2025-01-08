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

/**
 * A control that shows a rough graphical representation that can be used to scroll through code areas.
 *
 * Please note, that the code areas shall not be displayed elsewhere in the scenegraph as they will be added to the [TextOutline.node].
 */
class TextOutline(private val codeAreas: List<TextControl<*>>) {

    private val _sideProperty: ObjectProperty<HorizontalDirection> =
        SimpleObjectProperty(HorizontalDirection.LEFT).apply {
            addListener { _, _, side ->
                node.children.clear()
                node.children.addAll(codeAreas.map { it.control })
                when (side) {
                    HorizontalDirection.LEFT -> node.children.add(0, scrollbar)
                    HorizontalDirection.RIGHT -> node.children.add(scrollbar)
                }
            }
        }

    /**
     * The side on which the text outline shall be displayed.
     */
    var side: HorizontalDirection
        get() = _sideProperty.get()
        set(value) = _sideProperty.set(value)

    /**
     * The side on which the text outline shall be displayed.
     */
    fun sideProperty() = _sideProperty

    private val scrollbar = Canvas().apply {
        width = 100.0 * codeAreas.size
        heightProperty().bind(codeAreas.first().control.heightProperty())
        layoutBoundsProperty().addListener { _, _, _ -> draw() }
        setOnMouseMoved(::onMouseMoved)
        setOnMousePressed(::onMousePressed)
        setOnMouseClicked(::onMouseClicked)
        setOnMouseDragged(::onMouseDragged)
        setOnMouseReleased(::onMouseReleased)
    }
    private var mousePressYInRange = -1.0

    private var visibleRange = Double.MAX_VALUE.rangeTo(Double.MAX_VALUE)

    /**
     * The hbox containing the code areas as well as the textoutline.
     */
    val node = HBox(scrollbar).apply {
        children.addAll(codeAreas.map { it.control })
        minHeight = 0.0
    }

    init {
        codeAreas.forEach { area ->
            HBox.setHgrow(area.control, Priority.ALWAYS)
            area.textProperty().addListener { _, _, _ -> draw() }
            area.addVisibleLinesChangedCallback { draw() }
        }
        draw()
    }

    private fun draw() {
        val maxLineWidth = codeAreas.flatMap { it.text.lines() }.maxOf { it.length }.coerceAtMost(500)
        val controlWithMostLines = codeAreas.maxBy { it.lineCount }
        val widthPerChar = 100.0 / (maxLineWidth + 1)
        val heightPerLine = scrollbar.height / (controlWithMostLines.lineCount + 1)
        val ctx = scrollbar.graphicsContext2D
        ctx.clearRect(0.0, 0.0, scrollbar.width, scrollbar.height)
        codeAreas.forEachIndexed { i, it ->
            ctx.translate(i * 100.0, 0.0)
            if (i > 0) {
                ctx.stroke = Color.BLACK
                ctx.lineCap = StrokeLineCap.BUTT
                ctx.lineWidth = 1.0
                ctx.strokeLine(0.0, 0.0, 0.0, scrollbar.height)
            }
            ctx.lineCap = StrokeLineCap.ROUND
            ctx.lineWidth = (heightPerLine / 2).coerceAtMost(5.0)
            drawLines(ctx, it, heightPerLine, widthPerChar)
        }
        ctx.translate(-100.0 * (codeAreas.size - 1), 0.0)
        drawVisibleTextIndiciator(ctx, controlWithMostLines)
    }

    private fun drawLines(
        ctx: GraphicsContext,
        control: TextControl<*>,
        heightPerLine: Double,
        widthPerChar: Double
    ) {
        var linePos = heightPerLine / 2
        control.text.lineSequence().forEachIndexed { i, line ->
            ctx.stroke = control.getLineColor(line, i)
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

    private fun drawVisibleTextIndiciator(ctx: GraphicsContext, control: TextControl<*>) {
        ctx.fill = Color.LIGHTBLUE.deriveColor(0.0, 1.0, 1.0, 0.3)
        ctx.stroke = Color.LIGHTBLUE
        val lineSize = control.lineCount - 1
        val startPct = control.firstVisibleLine / lineSize.toDouble()
        val endPct = control.lastVisibleLine / lineSize.toDouble()
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
        codeAreas.forEach { it.scrollToYPercent(pct) }
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