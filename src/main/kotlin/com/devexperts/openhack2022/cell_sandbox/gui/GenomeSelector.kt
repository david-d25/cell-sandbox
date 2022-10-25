package com.devexperts.openhack2022.cell_sandbox.gui

import com.devexperts.openhack2022.cell_sandbox.game.CellState
import com.devexperts.openhack2022.cell_sandbox.game.Genome
import com.devexperts.openhack2022.cell_sandbox.game.World
import com.devexperts.openhack2022.cell_sandbox.game.WorldSettings
import com.devexperts.openhack2022.cell_sandbox.game.renderer.CellRenderer
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import javafx.application.Platform
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.control.ToggleButton
import javafx.scene.input.KeyCode
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Polygon
import javafx.scene.transform.Affine
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle

class GenomeSelector(
    private val library: MutableMap<Genome, String>
): Region() {
    val selectionProperty: Property<Genome?> = SimpleObjectProperty<Genome?>()
    var newGenomeFactory: (() -> Pair<Genome, String>)? = null

    private val textInput = TextField()
    private val dropdownButton = ToggleButton()
    private val mainBox = HBox(textInput, dropdownButton)
    private val dropdownIsOpenProperty = SimpleBooleanProperty(false)

    private val dropdownButtonImage = Polygon(0.0, 0.0, 1.0, 0.0, 0.5, 1.0)

    private val dropdownWindow = Stage()
    private val dropdownWrapper = ScrollPane()
    private val dropdown = VBox()

    init {
        mainBox.prefWidthProperty().bind(widthProperty())

        initDropdownWindow()
        initDropdownButton()
        initDropdownWrapper()
        initTextInput()
        initDropdown()

        children += mainBox
    }

    private fun updateGenomesList() {
        dropdown.children.clear()
        dropdown.prefWidthProperty().bind(dropdownWrapper.widthProperty())

        for ((genome, name) in library)
            dropdown.children += GenomeSelectorItem(genome, name)

        if (newGenomeFactory != null)
            dropdown.children += NewGenomeButtonItem()
    }

    private fun initDropdown() {
        dropdown.padding = Insets(10.0)

        dropdownIsOpenProperty.addListener { _, _, value ->
            if (value)
                updateGenomesList()
        }
    }

    private fun initTextInput() {
        textInput.minWidth = 0.0
        textInput.style = "-fx-background-radius: 4 0 0 4;"
        selectionProperty.addListener { _, _, value ->
            if (value != null)
                textInput.text = library[value] ?: "(unnamed)"
        }
        textInput.setOnKeyPressed {
            if (selectionProperty.value != null) {
                val selection = selectionProperty.value!!

                if (it.code == KeyCode.ESCAPE) {
                    textInput.text = library[selection] ?: ""
                    textInput.positionCaret(textInput.length)
                }

                else if (it.code == KeyCode.ENTER) {
                    if (textInput.text.isNotBlank()) {
                        library[selection] = textInput.text
                        textInput.parent.requestFocus()
                    } else {
                        textInput.text = library[selection] ?: ""
                        textInput.positionCaret(textInput.length)
                    }
                }
            }
        }
        textInput.focusedProperty().addListener { _, _, focused ->
            if (focused) {
                Platform.runLater { textInput.selectAll() }
            } else {
                if (textInput.text.isNotBlank())
                    if (selectionProperty.value != null)
                        library[selectionProperty.value!!] = textInput.text
                else {
                    textInput.text = library[selectionProperty.value] ?: ""
                    textInput.positionCaret(textInput.length)
                }
            }
        }
    }

    private fun initDropdownButton() {
        dropdownButtonImage.transforms += Affine.scale(12.0, 12.0)
        dropdownButtonImage.transforms += Affine.translate(-0.5, -0.5)
        dropdownButtonImage.fill = Color.DARKGREY

        dropdownButton.graphic = dropdownButtonImage
        dropdownButton.minWidthProperty().bind(dropdownButton.heightProperty())
        dropdownButton.prefHeightProperty().bind(textInput.heightProperty())
        dropdownButton.selectedProperty().bindBidirectional(dropdownIsOpenProperty)
        dropdownButton.style = "-fx-background-radius: 0 4 4 0;"
    }

    private fun initDropdownWindow() {
        dropdownIsOpenProperty.addListener { _, _, value ->
            if (value) {
                dropdownWindow.show()
                val bounds = localToScreen(layoutBounds)
                dropdownWindow.x = bounds.maxX - dropdownWindow.width
                dropdownWindow.y = bounds.maxY
                dropdownWrapper.requestFocus()
            }
            else {
                dropdownWindow.hide()
                scene.window.requestFocus()
                textInput.requestFocus()
            }
        }

        selectionProperty.addListener { _, _, _ ->
            dropdownIsOpenProperty.value = false
        }

        with (dropdownWindow) {
            initModality(Modality.NONE)
            initStyle(StageStyle.UNDECORATED)
            scene = Scene(dropdownWrapper)
            isResizable = false
            focusedProperty().addListener { _, _, value ->
                if (!value)
                    dropdownIsOpenProperty.value = false
            }
            sceneProperty().addListener { _, _, value ->
                initOwner(value.window)
            }
            scene.setOnKeyPressed {
                if (it.code == KeyCode.ESCAPE)
                    dropdownIsOpenProperty.value = false
            }
        }
    }

    private fun initDropdownWrapper() {
        with (dropdownWrapper) {
            visibleProperty().bind(dropdownIsOpenProperty)
            content = dropdown
            prefHeight = 400.0
            prefWidth = 350.0
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            style = "-fx-focus-color: transparent;"
            isManaged = false
            border = Border.stroke(Color.GRAY)
        }
    }

    inner class GenomeSelectorItem(genome: Genome, text: String): HBox() {
        private val size = 30.0
        private val renderer = CellRenderer()
        private val cellDummy = CellState(Vector2(size/2, size/2), Vector2(), 300.0, 0.0, 0.0, genome)

        init {
            styleClass.add("item")
            alignment = Pos.CENTER_LEFT
            isFocusTraversable = true

            onMouseEntered = EventHandler { background = Background.fill(Color(0.0, 0.0, 0.0, 0.2)) }
            onMouseExited = EventHandler { background = Background.EMPTY }

            val icon = Canvas(size, size)
            val worldDummy = World(WorldSettings()).apply { add(cellDummy) }
            renderer.render(worldDummy, icon.graphicsContext2D)

            val name = Label(text)

            setOnMouseClicked {
                selectionProperty.value = genome
            }

            children += icon
            children += name
        }


    }

    inner class NewGenomeButtonItem: HBox() {
        init {
            styleClass.add("item")
            alignment = Pos.CENTER_LEFT
            isFocusTraversable = true

            onMouseEntered = EventHandler { background = Background.fill(Color(0.0, 0.0, 0.0, 0.2)) }
            onMouseExited = EventHandler { background = Background.EMPTY }

            val icon = Canvas(30.0, 30.0)
            icon.graphicsContext2D.let {
                it.stroke = Color.DARKGREEN
                it.lineWidth = 4.0
                it.strokeLine(8.0, 15.0, 22.0, 15.0)
                it.strokeLine(15.0, 8.0, 15.0, 22.0)
            }

            val name = Label("New genome...")

            setOnMouseClicked {
                val pair = newGenomeFactory!!()
                library[pair.first] = pair.second
                selectionProperty.value = pair.first
            }

            children += icon
            children += name
        }
    }
}