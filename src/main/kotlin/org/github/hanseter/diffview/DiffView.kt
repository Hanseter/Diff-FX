//package org.github.hanseter.diffview
//
//import javafx.application.Platform
//import javafx.beans.InvalidationListener
//import javafx.beans.Observable
//import javafx.beans.value.ChangeListener
//import javafx.beans.value.ObservableValue
//import javafx.collections.FXCollections
//import javafx.collections.ObservableList
//import javafx.event.ActionEvent
//import javafx.scene.control.*
//import javafx.scene.input.KeyCode
//import javafx.scene.input.KeyEvent
//import javafx.scene.input.MouseButton
//import javafx.scene.input.MouseEvent
//import javafx.scene.layout.*
//import javafx.scene.shape.CubicCurveTo
//import javafx.scene.shape.LineTo
//import javafx.scene.shape.MoveTo
//import javafx.scene.shape.Path
//import org.fxmisc.richtext.CodeArea
//import java.io.BufferedReader
//import java.io.IOException
//import java.io.StringReader
//import java.net.URL
//import java.nio.charset.StandardCharsets
//import java.nio.file.Files
//import java.nio.file.Paths
//import java.util.*
//import java.util.function.Consumer
//import java.util.stream.Collectors
//import kotlin.collections.ArrayList
//import kotlin.math.max
//import kotlin.math.min
//
//class DiffView(
//    private var oldRawTxt: String?,
//    private var newRawTxt: String?
//) {
//
//    private val oldCodeArea = CodeArea().apply {
//        style = LookAndFeelSet.CODE_AREA_CSS
//        prefWidth = Region.USE_COMPUTED_SIZE
//        minWidth = Region.USE_PREF_SIZE
//        isEditable = false
//    }
//    private val newCodeArea = CodeArea().apply {
//        style = LookAndFeelSet.CODE_AREA_CSS
//        prefWidth = Region.USE_COMPUTED_SIZE
//        minWidth = Region.USE_PREF_SIZE
//        isEditable = false
//    }
//    private val diffDrawPanel: Pane? = null
//    private val mainPanel: GridPane? = null
//
//    //    private val  oldScrollPane: VirtualizedOverviewScrollPane<CodeArea>? = null
////    private val  newScrollPane: VirtualizedScrollPane<CodeArea>? = null
//    private val searchTextOld: TextField? = null
//    private val searchTextNew: TextField? = null
//    private val navBar: ToolBar? = null
//    private val prevBtn: Button? = null
//    private val nextBtn: Button? = null
//
//    private var oldText: String? = null
//    private var newText: String? = null
//
//    private val diffList: List<String> = ArrayList()
//
//    private var oldStartIndex = -1
//    private var newStartIndex = -1
//    private var currentDiff = -1
//
//    private var oldFileName: String? = null
//    private var newFileName: String? = null
//
//    private var fontSize = LookAndFeelSet.FONT_SIZE
//
//    private var oldScrolled: Boolean = false
//    private var newScrolled: Boolean = false
//
//    override fun initialize(location: URL?, resources: ResourceBundle?) {
//
//        initCodePanels()
//
//        mainPanel!!.addEventHandler<KeyEvent>(KeyEvent.KEY_PRESSED, EscEventHandler(mainPanel))
//
//        diffDrawPanel!!.widthProperty()
//            .addListener { observable: ObservableValue<out Number?>?, oldValue: Number?, newValue: Number? ->
//                Platform.runLater {
//                    updatePathElements()
//                    updateDiffOverview()
//                }
//            }
//
//        searchTextOld!!.textProperty()
//            .addListener { observable: ObservableValue<out String>?, oldValue: String?, newValue: String ->
//                searchValue(searchTextOld, newValue)
//            }
//        searchTextNew!!.textProperty()
//            .addListener { observable: ObservableValue<out String>?, oldValue: String?, newValue: String ->
//                searchValue(searchTextNew, newValue)
//            }
//
//        navBar!!.toFront()
//    }
//
//
//    private fun initCodePanels() {
//        oldScrollPane = VirtualizedOverviewScrollPane<CodeArea>(oldCodeArea)
//        newScrollPane = VirtualizedScrollPane<CodeArea>(newCodeArea)
//
//        VBox.setVgrow(oldScrollPane, Priority.ALWAYS)
//        VBox.setVgrow(oldScrollPane, Priority.ALWAYS)
//        VBox.setVgrow(newScrollPane, Priority.ALWAYS)
//        HBox.setHgrow(oldScrollPane, Priority.ALWAYS)
//        HBox.setHgrow(newScrollPane, Priority.ALWAYS)
//
//        oldScrollPane.setPrefHeight(2024.0)
//
//        mainPanel!!.add(oldScrollPane, 0, 1)
//        mainPanel!!.add(newScrollPane, 2, 1)
//
//        mainPanel!!.layout()
//
//        try {
//            var hbar = GitemberUtil.getField(oldScrollPane, "hbar") as ScrollBar
//            hbar.addEventHandler(
//                MouseEvent.ANY
//            ) { event: MouseEvent ->
//                if (MouseEvent.DRAG_DETECTED == event.eventType || (MouseEvent.MOUSE_ENTERED == event.eventType)
//                    || (MouseEvent.MOUSE_MOVED == event.eventType)
//                ) {
//                    updateAllowed = false
//                } else if (MouseEvent.MOUSE_RELEASED == event.eventType
//                    || (MouseEvent.MOUSE_EXITED_TARGET == event.eventType && MouseButton.NONE == event.button)
//                ) {
//                    updateAllowed = true
//                }
//            }
//
//
//            hbar = GitemberUtil.getField(newScrollPane, "hbar")
//            hbar.addEventHandler(
//                MouseEvent.ANY
//            ) { event: MouseEvent ->
//                if (MouseEvent.DRAG_DETECTED == event.eventType || (MouseEvent.MOUSE_ENTERED == event.eventType)
//                    || (MouseEvent.MOUSE_MOVED == event.eventType)
//                ) {
//                    updateAllowed = false
//                } else if (MouseEvent.MOUSE_RELEASED == event.eventType
//                    || (MouseEvent.MOUSE_EXITED_TARGET == event.eventType && MouseButton.NONE == event.button)
//                ) {
//                    updateAllowed = true
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//
//        oldScrollPane.totalHeightEstimateProperty().addListener(
//            ChangeListener { ov: ObservableValue<out Number?>?, old_val: Number?, new_val: Number? ->
//                if (new_val != null) {
//                    oldTotalHeight = new_val.toDouble()
//                }
//            }
//        )
//
//        newScrollPane.totalHeightEstimateProperty().addListener(
//            ChangeListener { ov: ObservableValue<out Number?>?, old_val: Number?, new_val: Number? ->
//                if (new_val != null) {
//                    newTotalHeight = new_val.toDouble()
//                }
//            }
//        )
//
//        oldScrollPane.estimatedScrollYProperty()
//            .addListener(ChangeListener { ov: ObservableValue<out Number>?, old_val: Number?, new_val: Number ->
//                if (updateAllowed) {
//                    oldScrolled = true
//                    if (!newScrolled) {
//                        val `val`: Double = oldScrollPane.snapSizeY(
//                            new_val.toDouble()
//                                    * newTotalHeight / oldTotalHeight
//                        )
//                        if (!`val`.isNaN()) {
//                            newScrollPane.estimatedScrollYProperty().setValue(`val`)
//                        }
//                    }
//                }
//            })
//
//        newScrollPane.estimatedScrollYProperty()
//            .addListener(ChangeListener { ov: ObservableValue<out Number>?, old_val: Number?, new_val: Number ->
//                if (updateAllowed) {
//                    newScrolled = true
//                    if (!oldScrolled) {
//                        val `val`: Double = oldScrollPane.snapSizeY(
//                            new_val.toDouble()
//                                    * oldTotalHeight / newTotalHeight
//                        )
//                        if (!`val`.isNaN()) {
//                            oldScrollPane.estimatedScrollYProperty().setValue(`val`)
//                        }
//                    }
//                    updatePathElements()
//                }
//            })
//
//
//        oldScrollPane.estimatedScrollYProperty().addListener(InvalidationListener { observable: Observable? ->
//            if (newScrolled && oldScrolled) {
//                updatePathElements()
//                updateDiffOverview()
//                oldScrolled = false
//                newScrolled = oldScrolled
//            }
//        })
//
//
//
//        newScrollPane.estimatedScrollYProperty().addListener(InvalidationListener { observable: Observable? ->
//            if (newScrolled && oldScrolled) {
//                updatePathElements()
//                updateDiffOverview()
//                oldScrolled = false
//                newScrolled = oldScrolled
//            }
//        })
//
//
//        oldScrollPane.getVbar().windowOffsetPercentProperty()
//            .addListener(InvalidationListener { newValue: Observable? ->
//                val perCent: Double = oldScrollPane.getVbar().getWindowOffsetPercent()
//                val oldScrollPanelNewVal: Double = oldScrollPane.totalHeightEstimateProperty().getValue() * perCent
//                val newScrollPanelNewVal: Double = newScrollPane.totalHeightEstimateProperty().getValue() * perCent
//                oldScrollPane.estimatedScrollYProperty().setValue(oldScrollPanelNewVal)
//                newScrollPane.estimatedScrollYProperty().setValue(newScrollPanelNewVal)
//                oldScrolled = true
//                newScrolled = oldScrolled
//            })
//
//        newScrollPane.heightProperty()
//            .addListener(ChangeListener { observable: ObservableValue<out Number?>?, oldValue: Number?, newValue: Number? ->
//                Platform.runLater {
//                    updatePathElements()
//                    updateDiffOverview()
//                }
//            })
//    }
//
//    var oldTotalHeight: Double = 1.0
//    var newTotalHeight: Double = 1.0
//
//
//    private fun updateDiffOverview() {
//        try {
//            oldScrollPane.getVbar().setHilightPosSize(
//                oldScrollPane.estimatedScrollYProperty().getValue()
//                        / oldScrollPane.totalHeightEstimateProperty().getValue(),
//                oldCodeArea.heightProperty().getValue() / oldCodeArea.totalHeightEstimateProperty().getValue()
//            )
//        } catch (e: Exception) {
//        }
//    }
//
//    private fun getDiffPos(delta: Edit): SquarePos {
//        val leftPos: Int = delta.getBeginA()
//        val leftLines: Int = delta.getLengthA()
//        val rightPos: Int = delta.getBeginB()
//        val rightLines: Int = delta.getLengthB()
//
//        val origBottomPos = leftPos + leftLines
//        val revBottomPos = rightPos + rightLines
//
//
//        val deltaY1: Double = oldScrollPane.estimatedScrollYProperty().getValue()
//
//        val deltaY2: Double = newScrollPane.estimatedScrollYProperty().getValue()
//        val border_shift = 0 //uber node and 1 node border
//
//
//        val x1 = -1
//        val y1 = (leftPos * fontSize - deltaY1).toInt() + border_shift
//
//        val x2 = diffDrawPanel!!.width.toInt() + 1
//        val y2 = (rightPos * fontSize - deltaY2).toInt() + border_shift
//
//        val x3 = x2
//        val y3 = (revBottomPos * fontSize - deltaY2).toInt() + border_shift
//
//        val x4 = x1
//        val y4 = (origBottomPos * fontSize - deltaY1).toInt() + border_shift
//
//        return SquarePos(x1, y1, x2, y2, x3, y3, x4, y4)
//    }
//
//    private fun updatePathElements() {
//        for (i in diffDrawPanel!!.children.indices) {
//            val path = diffDrawPanel!!.children[i] as Path
//
//            val delta: Edit = diffList.get(i)
//
//            val squarePos: SquarePos = getDiffPos(delta)
//
//            val moveTo = path.elements[0] as MoveTo
//            val curve0 = path.elements[1] as CubicCurveTo
//            val lineTo0 = path.elements[2] as LineTo
//            val curve1 = path.elements[3] as CubicCurveTo
//            val lineTo1 = path.elements[4] as LineTo
//
//            moveTo.x = squarePos.getX1().toDouble()
//            moveTo.y = squarePos.getY1().toDouble()
//
//            var ccTo = getCubicCurveTo(squarePos.getX1(), squarePos.getY1(), squarePos.getX2(), squarePos.getY2())
//            curve0.x = squarePos.getX2().toDouble()
//            curve0.y = squarePos.getY2().toDouble()
//            curve0.controlX1 = ccTo.controlX1
//            curve0.controlX2 = ccTo.controlX2
//            curve0.controlY1 = ccTo.controlY1
//            curve0.controlY2 = ccTo.controlY2
//
//
//            lineTo0.x = squarePos.getX3().toDouble()
//            lineTo0.y = squarePos.getY3().toDouble()
//
//            ccTo = getCubicCurveTo(squarePos.getX3(), squarePos.getY3(), squarePos.getX4(), squarePos.getY4())
//            curve1.x = squarePos.getX4().toDouble()
//            curve1.y = squarePos.getY4().toDouble()
//            curve1.controlX1 = ccTo.controlX1
//            curve1.controlX2 = ccTo.controlX2
//            curve1.controlY1 = ccTo.controlY1
//            curve1.controlY2 = ccTo.controlY2
//
//            lineTo1.x = squarePos.getX1().toDouble()
//            lineTo1.y = squarePos.getY1().toDouble()
//        }
//    }
//
//
//    private fun getLines(content: String?): Int {
//        return BufferedReader(StringReader(content))
//            .lines()
//            .collect(Collectors.toList()).size
//    }
//
//
//    private fun scrollToFirstDiff() {
//        val totalOldLines = getLines(oldText)
//        val totalNewLines = getLines(newText)
//        //if (totalNewLines > 40 && totalOldLines > 40) {
//        if (!diffList.isEmpty()) {
//            currentDiff = 0
//            scrollToDiff(true)
//        }
//        //}
//    }
//
//    private fun scrollToDiff(forward: Boolean) {
//        val delta: Edit = diffList.get(currentDiff)
//        val origPos: Int
//        val revPos: Int
//        if (forward) {
//            origPos = max(0.0, (min(delta.getEndA().toDouble(), delta.getEndB().toDouble()) - 5).toDouble()).toInt()
//            revPos = max(0.0, (min(delta.getEndA().toDouble(), delta.getEndB().toDouble()) - 5).toDouble()).toInt()
//        } else {
//            origPos = max(0.0, (min(delta.getBeginA().toDouble(), delta.getBeginB().toDouble()) + 5).toDouble()).toInt()
//            revPos = max(0.0, (min(delta.getBeginA().toDouble(), delta.getBeginB().toDouble()) + 5).toDouble()).toInt()
//        }
//
//        //oldCodeArea.moveTo(origPos, 0);
//        //newCodeArea.moveTo(revPos, 0);
//        oldCodeArea.requestFollowCaret()
//        oldCodeArea.layout()
//        newCodeArea.requestFollowCaret()
//        newCodeArea.layout()
//
//        //updateButtonState();
//
//        //highlight active diff
//        for (i in diffDrawPanel!!.children.indices) {
//            diffDrawPanel!!.children[i].styleClass.remove("diff-active")
//        }
//        diffDrawPanel!!.children[currentDiff].styleClass.add("diff-active")
//
//        setText(oldCodeArea, oldText, oldFileName, true, currentDiff)
//        setText(newCodeArea, newText, newFileName, false, currentDiff)
//
//        //System.out.println(" orig pos " + origPos + " rev " + revPos);
//        //oldScrollPane.estimatedScrollYProperty().setValue(fontSize * origPos);
//        //newScrollPane.estimatedScrollYProperty().setValue(fontSize * revPos);
//    }
//
//
//    private fun createPathElements() {
//        diffDrawPanel!!.children.clear()
//        for (delta in this.diffList) {
//            val squarePos: SquarePos = getDiffPos(delta)
//            val moveTo = MoveTo(squarePos.getX1().toDouble(), squarePos.getY1().toDouble())
//            val curve0 = getCubicCurveTo(squarePos.getX1(), squarePos.getY1(), squarePos.getX2(), squarePos.getY4())
//            val lineTo0 = LineTo(squarePos.getX3().toDouble(), squarePos.getY3().toDouble())
//            val curve1 = getCubicCurveTo(squarePos.getX3(), squarePos.getY3(), squarePos.getX4(), squarePos.getY4())
//            val lineTo1 = LineTo(squarePos.getX1().toDouble(), squarePos.getY1().toDouble())
//
//            val path = Path()
//            path.elements.addAll(moveTo, curve0, lineTo0, curve1, lineTo1)
//            path.styleClass.add(GitemberUtil.getDiffSyleClass(delta, "diff"))
//
//            diffDrawPanel!!.children.add(path)
//        }
//    }
//
//    private fun getCubicCurveTo(x1: Int, y1: Int, x2: Int, y2: Int): CubicCurveTo {
//        val controlPointDeltaX = 15
//        return CubicCurveTo(
//            if (x2 > x1) diffDrawPanel!!.width - controlPointDeltaX else controlPointDeltaX.toDouble(), y1.toDouble(),
//            if (x2 > x1) controlPointDeltaX.toDouble() else diffDrawPanel!!.width - controlPointDeltaX, y2.toDouble(),
//            x2.toDouble(), y2.toDouble()
//        )
//    }
//
//    private fun setText(
//        codeArea: CodeArea,
//        text: String?, fileName: String?, leftSide: Boolean
//    ) {
//        setText(codeArea, text, fileName, leftSide, -1)
//    }
//
//    private fun setText(
//        codeArea: CodeArea?,
//        text: String?, fileName: String?,
//        leftSide: Boolean,
//        activeParagrah: Int
//    ) {
//        codeArea.clear()
//        codeArea.appendText(text)
//
//        val adapter: TextToSpanContentAdapter = TextToSpanContentAdapter(
//            org.apache.commons.io.FilenameUtils.getExtension(fileName),
//            this.diffList, leftSide, activeParagrah
//        )
//
//        val spans: StyleSpans<Collection<String>> = adapter.computeHighlighting(codeArea.getText())
//        if (spans != null) {
//            codeArea.setStyleSpans(0, spans)
//        }
//
//        val decoration: Map<Int, List<String>> = adapter.getDecorateByPatch(activeParagrah)
//        decoration.forEach { (paragraph: Int?, paragraphStyle: MutableList<String?>?) ->
//            codeArea.setParagraphStyle(
//                paragraph,
//                paragraphStyle
//            )
//        }
//
//        codeArea.setParagraphGraphicFactory(
//            GitemberLineNumberFactory.get(codeArea, adapter, null, activeParagrah)
//        )
//    }
//
//
//    fun repeatSearch(keyEvent: KeyEvent) {
//        if (keyEvent.code == KeyCode.ENTER) {
//            if (keyEvent.source === searchTextOld) {
//                oldStartIndex++
//                searchValue(keyEvent.source as TextField, searchTextOld!!.text)
//            } else {
//                newStartIndex++
//                searchValue(keyEvent.source as TextField, searchTextNew!!.text)
//            }
//        }
//    }
//
//    fun searchValue(textField: TextField?, value: String) {
//        val searchCodeArea: CodeArea?
//        var startIndex: Int
//        if (textField === searchTextOld) {
//            searchCodeArea = oldCodeArea
//            startIndex = oldStartIndex
//        } else {
//            searchCodeArea = newCodeArea
//            startIndex = newStartIndex
//        }
//
//        startIndex = searchCodeArea.getText().indexOf(value, startIndex)
//        if (startIndex == -1) {
//            startIndex = searchCodeArea.getText().indexOf(value)
//        }
//
//        if (startIndex > -1) {
//            searchCodeArea.moveTo(startIndex)
//            searchCodeArea.selectRange(startIndex, startIndex + value.length)
//        } else {
//            searchCodeArea.selectRange(0, 0)
//        }
//        searchCodeArea.requestFollowCaret()
//
//        if (textField === searchTextOld) {
//            oldStartIndex = startIndex
//        } else {
//            newStartIndex = startIndex
//        }
//    }
//
//    fun prevHandler(actionEvent: ActionEvent?) {
//        if (0 < currentDiff) {
//            currentDiff--
//        }
//        scrollToDiff(true)
//    }
//
//    fun nextHandler(actionEvent: ActionEvent?) {
//        if ((diffList.size - 1) > currentDiff) {
//            currentDiff++
//        }
//        scrollToDiff(false)
//    }
//
//
//    private fun updateButtonState() {
//        prevBtn!!.isDisable = currentDiff <= 0
//        nextBtn!!.isDisable = ((diffList.size - 1)
//                <= currentDiff)
//    }
//
//
//    fun oldRevisionChange(actionEvent: ActionEvent?) {
//        var filaNameCandidate: String = scmItem.getShortName()
//
//        try {
//            if (scmItem.getAttribute().getStatus() != null) {
//                if (scmItem.getAttribute().getStatus() == ScmItem.Status.RENAMED) {
//                    filaNameCandidate = scmItem.getAttribute().getOldName()
//                }
//            }
//            oldFileName = com.az.gitember.service.Context.getGitRepoService()
//                .saveFile(oldRevisionsCmb!!.getValue().getRevisionFullName(), filaNameCandidate)
//        } catch (e: Exception) {
//            oldFileName = com.az.gitember.service.Context.getGitRepoService().createEmptyFile(scmItem.getShortName())
//        }
//        try {
//            this.oldText = Files.readString(Paths.get(oldFileName))
//        } catch (e: IOException) {
//            throw RuntimeException(e)
//        }
//        this.oldRawTxt = RawText(oldText.toByteArray(StandardCharsets.UTF_8))
//        updateDiffRepresentation()
//        oldRevisionsCmb!!.setDisable(true)
//    }
//
//    fun newRevisionChange(actionEvent: ActionEvent?) {
//        newFileName = try {
//            if (newSha == null) {
//                java.nio.file.Path.of(com.az.gitember.service.Context.getProjectFolder(), scmItem.getShortName())
//                    .toString()
//            } else {
//                com.az.gitember.service.Context.getGitRepoService()
//                    .saveFile(newRevisionsCmb!!.getValue().getRevisionFullName(), scmItem.getShortName())
//            }
//        } catch (e: Exception) {
//            com.az.gitember.service.Context.getGitRepoService().createEmptyFile(scmItem.getShortName())
//        }
//        try {
//            this.newText = Files.readString(Paths.get(newFileName))
//        } catch (e: IOException) {
//            throw RuntimeException(e)
//        }
//        this.newRawTxt = RawText(newText.toByteArray(StandardCharsets.UTF_8))
//        updateDiffRepresentation()
//        newRevisionsCmb!!.setDisable(true)
//    }
//
//
//    fun updateDiffRepresentation() {
//        if (oldRawTxt != null && newRawTxt != null) {
//            setText(oldCodeArea, oldText, oldFileName, true)
//            setText(newCodeArea, newText, newFileName, false)
//            //initCodePanels();
//            val diffAlgorithm: DiffAlgorithm = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM)
//            diffList.addAll(diffAlgorithm.diff<RawText>(RawTextComparator.WS_IGNORE_ALL, oldRawTxt, newRawTxt))
//            oldScrollPane.getVbar().setData(oldText, newText, diffList)
//            createPathElements()
//            scrollToFirstDiff()
//        }
//    }
//
//}