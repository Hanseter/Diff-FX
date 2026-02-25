package io.github.hanseter.diffview

import javafx.application.Platform
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.HorizontalDirection
import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.StrokeLineCap
import org.fxmisc.richtext.CodeArea

/**
 * A control that shows a rough graphical representation that can be used to scroll through code areas.
 *
 * Please note, that the code areas shall not be displayed elsewhere in the scenegraph as they will be added to the [TextOutline.node].
 */
class TextOutline(private val codeAreas: List<TextControl<*>>) {

    var viewPortFill: Paint = Color.LIGHTBLUE.deriveColor(0.0, 1.0, 1.0, 0.3)
        set(value) {
            field = value
            redrawOutline()
        }
    var viewPortStroke: Paint = Color.LIGHTBLUE
        set(value) {
            field = value
            redrawOutline()
        }

    var minLineHeight = 0.5
        set(value) {
            field = value
            redrawOutline()
        }
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


    private val _textOutlineWidthProperty: SimpleDoubleProperty =
        SimpleDoubleProperty(100.0).apply {
            addListener { _, _, width ->
                outlineCanvas.width = width.toDouble()
                viewPortCanvas.width = width.toDouble()
                scrollbar.prefWidth = width.toDouble()
                drawTextOutline()
                drawViewPort()
            }
        }

    /**
     * The width of the text outline
     */
    var textOutlineWidth: Double
        get() = textOutlineWidthProperty().get()
        set(value) = textOutlineWidthProperty().set(value)

    /**
     * The width of the text outline
     */
    fun textOutlineWidthProperty(): DoubleProperty = _textOutlineWidthProperty

    private var dirty = false
    private val outlineCanvas = Canvas().apply {
        width = textOutlineWidth
        heightProperty().bind(codeAreas.first().control.heightProperty())
    }
    private val viewPortCanvas = Canvas().apply {
        width = textOutlineWidth
        heightProperty().bind(codeAreas.first().control.heightProperty())
        applyCss()
    }
    private val scrollbar = StackPane(outlineCanvas, viewPortCanvas).apply {
        prefWidth = textOutlineWidth
        prefHeightProperty().bind(codeAreas.first().control.heightProperty())
        layoutBoundsProperty().addListener { _, _, _ ->
            Platform.runLater {
                drawTextOutline()
                drawViewPort()
            }
        }
        setOnMouseMoved(::onMouseMoved)
        setOnMousePressed(::onMousePressed)
        setOnMouseClicked(::onMouseClicked)
        setOnMouseDragged(::onMouseDragged)
        setOnMouseReleased(::onMouseReleased)
        applyCss()
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
            area.textProperty().addListener { _, _, _ ->
                drawTextOutline()
                drawViewPort()
            }
            area.addVisibleLinesChangedCallback { drawViewPort() }
        }
        drawTextOutline()
        drawViewPort()
    }

    /**
     * Redraws the text outline asynchronously.
     */
    fun redrawOutline() {
        if (dirty) return
        dirty = true
        Platform.runLater {
            if (!dirty) return@runLater
            drawTextOutline()
            drawViewPort()
            dirty = false
        }
    }

    private fun drawTextOutline() {
        val maxLineWidth = findLongestLine()
        val controlWithMostLines = codeAreas.maxBy { it.lineCount }
        val height = scrollbar.height.coerceAtMost(codeAreas.maxOf { it.contentHeight })
        val widthPerCodeArea = textOutlineWidth / codeAreas.size
        val widthPerChar = widthPerCodeArea / (maxLineWidth + 1)
        val heightPerLine = height / (controlWithMostLines.lineCount + 1)
        val ctx = outlineCanvas.graphicsContext2D
        ctx.clearRect(0.0, 0.0, scrollbar.width, scrollbar.height)
        codeAreas.forEachIndexed { i, it ->
            ctx.translate(i * widthPerCodeArea, 0.0)
            if (i > 0) {
                ctx.stroke = Color.BLACK
                ctx.lineCap = StrokeLineCap.BUTT
                ctx.lineWidth = 1.0
                ctx.strokeLine(0.0, 0.0, 0.0, scrollbar.height)
            }
            ctx.lineCap = StrokeLineCap.ROUND
            ctx.lineWidth = (heightPerLine / 2).coerceAtLeast(minLineHeight).coerceAtMost(5.0)
            drawLines(ctx, it, heightPerLine, widthPerChar)
        }
        ctx.translate(-widthPerCodeArea * (codeAreas.size - 1), 0.0)
    }

    private fun drawViewPort() {
        val controlWithMostLines = codeAreas.maxBy { it.lineCount }
        val ctx = viewPortCanvas.graphicsContext2D

        ctx.clearRect(0.0, 0.0, scrollbar.width, scrollbar.height)
        drawVisibleTextIndiciator(ctx, controlWithMostLines)
    }

    private fun findLongestLine(): Int {
        var longest = 0
        codeAreas.forEach { area ->
            var currentLineLength = 0
            area.text.forEach { c ->
                if (c == '\n') {
                    if (currentLineLength > longest) {
                        if (currentLineLength >= 500) return 500
                        longest = currentLineLength
                    }
                    currentLineLength = 0
                } else {
                    currentLineLength++
                }
            }
            if (currentLineLength > longest) {
                if (currentLineLength >= 500) return 500
                longest = currentLineLength
            }
        }
        return longest
    }

    private fun drawLines(
        ctx: GraphicsContext,
        control: TextControl<*>,
        heightPerLine: Double,
        widthPerChar: Double
    ) {
        var linePos = heightPerLine / 2
        var index = 0
        val line = StringBuilder()
        control.text.forEach { c ->
            if (c != '\n') {
                line.append(c)
                return@forEach
            }
            ctx.stroke = control.getLineColor(line.toString(), index)
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
            line.clear()
            index++
        }
    }

    private fun drawVisibleTextIndiciator(ctx: GraphicsContext, control: TextControl<*>) {
        ctx.fill = viewPortFill
        ctx.stroke = viewPortStroke
        val lineSize = control.lineCount - 1
        val startPct: Double
        val endPct: Double
        if (lineSize > 0) {
            startPct = control.firstVisibleLine / lineSize.toDouble()
            endPct = control.lastVisibleLine / lineSize.toDouble()
        } else {
            startPct = 0.0
            endPct = 1.0
        }
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

    companion object {
        fun forCodeArea(ca: CodeArea): TextOutline =
            TextOutline(listOf(CodeAreaOutlineWrapper(ca)))

        fun forCodeAreas(cas: List<CodeArea>): TextOutline =
            TextOutline(cas.map { CodeAreaOutlineWrapper(it) })
    }
}