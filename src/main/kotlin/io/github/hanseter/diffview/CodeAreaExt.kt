@file:JvmName("CodeAreaExt")

package io.github.hanseter.diffview

import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory


/**
 * Adds line numbers to this code area.
 */
fun CodeArea.addLineNumbers() {
    paragraphGraphicFactory = LineNumberFactory.get(this)
}
