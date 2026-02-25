package io.github.hanseter.diffview

import com.github.difflib.text.DiffRow
import javafx.application.Platform
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.Node
import javafx.scene.layout.StackPane


abstract class DiffViewBase {

    protected val cssHelper: DiffCssHelper = DiffCssHelper().apply {
        unchangedLineColor.addListener { _, _, _ ->
            scrollBar.redrawOutline()
            println("Changed")
        }
        changedLineColor.addListener { _, _, _ -> scrollBar.redrawOutline() }
        newLineColor.addListener { _, _, _ -> scrollBar.redrawOutline() }
        removedLineColor.addListener { _, _, _ -> scrollBar.redrawOutline() }
    }

    protected abstract val scrollBar: TextOutline

    protected abstract val diff: List<DiffRow>

    private val _minLineThickness = SimpleDoubleProperty(0.5).apply {
        addListener { _, _, thickness -> scrollBar.minLineHeight = thickness.toDouble() }
    }

    var minLineThickness: Double
        get() = _minLineThickness.value
        set(value) {
            _minLineThickness.value = value
        }

    fun minLineThicknessProperty(): DoubleProperty = _minLineThickness

    private val _node = StackPane()
        get() {
            if (field.children.isEmpty()) field.children.addAll(scrollBar.node, cssHelper.node)
            return field
        }

    val node: Node
        get() = _node

    private var lastConflict = -1

    //programmatically scrolling happens at _some_ point not immediately on the fx thread.
    //this is an innate behaviour of the controlsfx rich text control.
    //ignoring the event is a best try to mitigate this
    private var ignoreNextScroll = false

    /**
     * Scrolls the view port to the next diff.
     */
    fun scrollToNextDiff() {
        val top = if (lastConflict != -1) lastConflict else currentTopLine()
        val diffBlocks = findDiffRanges(diff)
        val diffToFocus =
            diffBlocks.firstOrNull { it.first > top } ?: diffBlocks.firstOrNull() ?: return
        ignoreNextScroll = true
        scrollToPct(diffToFocus.first / diff.size.toDouble())
        lastConflict = diffToFocus.first
    }

    protected abstract fun currentTopLine(): Int
    protected abstract fun currentBottomLine(): Int

    /**
     * Scrolls the view port to the previous diff.
     */
    fun scrollToPreviousDiff() {
        val top = if (lastConflict != -1) lastConflict else currentTopLine()
        val diffBlocks = findDiffRanges(diff)
        val diffToFocus =
            diffBlocks.lastOrNull { it.first < top } ?: diffBlocks.lastOrNull() ?: return
        ignoreNextScroll = true
        scrollToPct(diffToFocus.first / diff.size.toDouble())
        lastConflict = diffToFocus.first
    }

    /**
     * Scrolls to a value between 0 and 1. 0 Being the top and 1 being the bottom.
     */
    abstract fun scrollToPct(pct: Double)

    protected fun onScrolled() {
        if (ignoreNextScroll) {
            ignoreNextScroll = false
            return
        }
        lastConflict = -1
    }

    companion object {

        private fun findDiffRanges(rows: List<DiffRow>): List<IntRange> {
            val ret = ArrayList<IntRange>()
            val iter = rows.listIterator()
            while (iter.hasNext()) {
                if (iter.next().tag != DiffRow.Tag.EQUAL) {
                    val start = iter.previousIndex()
                    ret += start..<indexOfNextEqual(iter)
                }
            }
            return ret
        }

        private fun indexOfNextEqual(iter: ListIterator<DiffRow>): Int {
            while (iter.hasNext()) {
                if (iter.next().tag == DiffRow.Tag.EQUAL) {
                    return iter.previousIndex()
                }
            }
            return iter.previousIndex() + 1
        }
    }
}