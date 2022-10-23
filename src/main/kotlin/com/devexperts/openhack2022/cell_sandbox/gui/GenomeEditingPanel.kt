package com.devexperts.openhack2022.cell_sandbox.gui

import com.devexperts.openhack2022.cell_sandbox.game.*
import com.devexperts.openhack2022.cell_sandbox.game.renderer.CellRenderer
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.VPos
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import kotlin.math.PI
import kotlin.math.pow

class GenomeEditingPanel(
    private val world: World,
): AnchorPane() {
    private val createGenomeFactory = {
        var counter = 1
        {
            Pair(Genome(
                type = CellType.PHAGOCYTE,
                cyanPigment = Math.random()*0.8,
                magentaPigment = Math.random()*0.8,
                yellowPigment = Math.random()*0.8,
                hardness = 0.5,
                splitMass = 300.0,
                splitAngle = 0.0,
                child1Angle = 0.0,
                child2Angle = 0.0,
                stickOnSplit = false,
                child1KeepConnections = true,
                child2KeepConnections = true
            ), "genome " + counter++)
        }
    }

    private val previewContainer = StackPane()
    private val editingFormScrollWrapper = ScrollPane()
    private val editingFormContainer = GridPane()

    private val genomeToEditLabel = Label("Genome: ").also { it.style = "-fx-font-weight: bold;" }

    private val genomeToEditSelector = GenomeSelector(world.genomeLibrary)
    private val selectedGenomeProperty = genomeToEditSelector.selectionProperty

    private val child1Preview = Canvas(60.0, 60.0)
    private val child2Preview = Canvas(60.0, 60.0)

    private val cellRenderer = CellRenderer()
    private val dummyWorld = World(WorldSettings())

    init {
        initPreviewContainer()
        initEditingFormScrollWrapper()
        initEditingFormContainer()
    }

    private fun initEditingFormContainer() {
        editingFormContainer.padding = Insets(10.0)
        editingFormContainer.hgap = 10.0
        editingFormContainer.vgap = 10.0
        editingFormContainer.columnConstraints.addAll(
            ColumnConstraints(-1.0, -1.0, -1.0, Priority.SOMETIMES, HPos.RIGHT, true),
            ColumnConstraints(-1.0, -1.0, -1.0, Priority.ALWAYS, HPos.LEFT, true)
        )

        val genomeFactory = createGenomeFactory()
        genomeToEditSelector.newGenomeFactory = genomeFactory
        editingFormContainer.addRow(editingFormContainer.rowCount, genomeToEditLabel, genomeToEditSelector)

        val typeInput = ComboBox<CellType>()
        typeInput.items.addAll(CellType.values())
        typeInput.valueProperty().addListener { _, _, value -> selectedGenomeProperty.value?.type = value }

        val cyanPigmentInput = Slider(0.0, 1.0, 0.0).apply {
            valueProperty().addListener { _, _, value ->
                selectedGenomeProperty.value?.cyanPigment = value.toDouble()
                updateChildrenPreviews()
            }
        }

        val magentaPigmentInput = Slider(0.0, 1.0, 0.0).apply {
            valueProperty().addListener { _, _, value ->
                selectedGenomeProperty.value?.magentaPigment = value.toDouble()
                updateChildrenPreviews()
            }
        }

        val yellowPigmentInput = Slider(0.0, 1.0, 0.0).apply {
            valueProperty().addListener { _, _, value ->
                selectedGenomeProperty.value?.yellowPigment = value.toDouble()
                updateChildrenPreviews()
            }
        }

        val hardnessInput = Slider(0.2, 1.0, 0.2).apply {
            valueProperty().addListener { _, _, value ->
                selectedGenomeProperty.value?.hardness = value.toDouble()
            }
        }

        val nutritionPriorityInput = Slider(0.0, 1.0, 0.5).apply {
            valueProperty().addListener { _, _, value ->
                selectedGenomeProperty.value?.nutritionPriority = value.toDouble()
            }
        }

        val splitMassInput = Slider(80.0, 360.0, 80.0).apply {
            valueProperty().addListener { _, _, value ->
                selectedGenomeProperty.value?.splitMass = value.toDouble()
            }
            majorTickUnit = 100.0
        }

        val splitAngleInput = Slider(0.0, 360.0, 0.0).apply {
            valueProperty().addListener { _, _, value ->
                selectedGenomeProperty.value?.splitAngle = Math.toRadians(value.toDouble())
            }
            majorTickUnit = 60.0
        }

        val stickOnSplitInput = CheckBox().apply {
            selectedProperty().addListener { _, _, value ->
                selectedGenomeProperty.value?.stickOnSplit = value
            }
        }

        val child1Input = GenomeSelector(world.genomeLibrary)
        val child2Input = GenomeSelector(world.genomeLibrary)

        setOf(child1Input, child2Input).forEach {
            it.newGenomeFactory = genomeFactory
            it.selectionProperty.addListener { _, _, value ->
                if (value != null) {
                    selectedGenomeProperty.value?.let { genome ->
                        if (it == child1Input)
                            genome.children = genome.children.copy(first = value)
                        else
                            genome.children = genome.children.copy(second = value)
                        updateChildrenPreviews()
                    }
                }
            }
        }

        val child1AngleInput = Slider(0.0, 360.0, 0.0).apply {
            valueProperty().addListener { _, _, value ->
                selectedGenomeProperty.value?.child1Angle = Math.toRadians(value.toDouble())
            }
            majorTickUnit = 60.0
        }

        val child2AngleInput = Slider(0.0, 360.0, 0.0).apply {
            valueProperty().addListener { _, _, value ->
                selectedGenomeProperty.value?.child2Angle = Math.toRadians(value.toDouble())
            }
            majorTickUnit = 60.0
        }

        val child1KeepConnectionsInput = CheckBox("Inherit parent connections").apply {
            selectedProperty().addListener { _, _, value ->
                selectedGenomeProperty.value?.child1KeepConnections = value
            }
        }
        val child2KeepConnectionsInput = CheckBox("Inherit parent connections").apply {
            selectedProperty().addListener { _, _, value ->
                selectedGenomeProperty.value?.child2KeepConnections = value
            }
        }

        setOf(
            cyanPigmentInput, magentaPigmentInput, yellowPigmentInput,
            hardnessInput, nutritionPriorityInput
        ).forEach {
            it.majorTickUnit = 0.5
            it.minorTickCount = 5
        }

        setOf(
            cyanPigmentInput, magentaPigmentInput, yellowPigmentInput,
            hardnessInput, splitMassInput, splitAngleInput,
            child1AngleInput, child2AngleInput, nutritionPriorityInput
        ).forEach {
            it.isShowTickMarks = true
            it.isShowTickLabels = true
        }

        selectedGenomeProperty.addListener { _, _, genome ->
            if (genome != null) {
                typeInput.value = genome.type
                cyanPigmentInput.value = genome.cyanPigment
                magentaPigmentInput.value = genome.magentaPigment
                yellowPigmentInput.value = genome.yellowPigment
                hardnessInput.value = genome.hardness
                splitMassInput.value = genome.splitMass
                splitAngleInput.value = Math.toDegrees(genome.splitAngle)
                stickOnSplitInput.isSelected = genome.stickOnSplit
                child1Input.selectionProperty.value = genome.children.first
                child2Input.selectionProperty.value = genome.children.second
                child1AngleInput.value = Math.toDegrees(genome.child1Angle)
                child2AngleInput.value = Math.toDegrees(genome.child2Angle)
                child1KeepConnectionsInput.isSelected = genome.child1KeepConnections
                child2KeepConnectionsInput.isSelected = genome.child2KeepConnections

                updateChildrenPreviews()
            }
        }

        editingFormContainer.addRow(editingFormContainer.rowCount, Label("Type:"), typeInput)

        val pigmentsLabel = Label("Pigments").apply { textFill = Color.GRAY }
        GridPane.setHalignment(pigmentsLabel, HPos.CENTER)
        editingFormContainer.add(pigmentsLabel, 0, editingFormContainer.rowCount, 2, 1)

        editingFormContainer.addRow(editingFormContainer.rowCount, Label("Cyan: "), cyanPigmentInput)
        editingFormContainer.addRow(editingFormContainer.rowCount, Label("Magenta: "), magentaPigmentInput)
        editingFormContainer.addRow(editingFormContainer.rowCount, Label("Yellow: "), yellowPigmentInput)

        val splitLabel = Label("Split").apply { textFill = Color.GRAY }
        GridPane.setHalignment(splitLabel, HPos.CENTER)
        editingFormContainer.add(splitLabel, 0, editingFormContainer.rowCount, 2, 1)
        editingFormContainer.addRow(editingFormContainer.rowCount, Label("Split mass: "), splitMassInput)
        editingFormContainer.addRow(editingFormContainer.rowCount, Label("Split angle: "), splitAngleInput)
        editingFormContainer.addRow(editingFormContainer.rowCount, Label("Stick on split: "), stickOnSplitInput)

        val child1Label = Label("Child 1").apply { textFill = Color.GRAY }
        GridPane.setHalignment(child1Label, HPos.CENTER)

        val child1Grid = GridPane()
        child1Grid.addRow(child1Grid.rowCount, Label("Type: "), child1Input)
        child1Grid.addRow(child1Grid.rowCount, Label("Angle: "), child1AngleInput)
        child1Grid.add(child1KeepConnectionsInput, 1, child1Grid.rowCount)

        editingFormContainer.add(child1Label, 0, editingFormContainer.rowCount, 2, 1)
        editingFormContainer.addRow(editingFormContainer.rowCount, child1Preview, child1Grid)

        val child2Grid = GridPane()
        child2Grid.addRow(child2Grid.rowCount, Label("Type: "), child2Input)
        child2Grid.addRow(child2Grid.rowCount, Label("Angle: "), child2AngleInput)
        child2Grid.add(child2KeepConnectionsInput, 1, child2Grid.rowCount)

        val child2Label = Label("Child 2").apply { textFill = Color.GRAY }
        GridPane.setHalignment(child2Label, HPos.CENTER)
        editingFormContainer.add(child2Label, 0, editingFormContainer.rowCount, 2, 1)
        editingFormContainer.addRow(editingFormContainer.rowCount, child2Preview, child2Grid)

        GridPane.setValignment(child1Preview, VPos.TOP)
        GridPane.setValignment(child2Preview, VPos.TOP)

        setOf(child1Grid, child2Grid).forEach {
            it.hgap = 5.0
            it.vgap = 5.0
            it.columnConstraints.addAll(
                ColumnConstraints(-1.0, -1.0, -1.0, Priority.SOMETIMES, HPos.RIGHT, true),
                ColumnConstraints(-1.0, -1.0, -1.0, Priority.ALWAYS, HPos.LEFT, true)
            )
        }

        val otherLabel = Label("Other").apply { textFill = Color.GRAY }
        GridPane.setHalignment(otherLabel, HPos.CENTER)

        editingFormContainer.add(otherLabel, 0, editingFormContainer.rowCount, 2, 1)
        editingFormContainer.addRow(editingFormContainer.rowCount, Label("Hardness: "), hardnessInput)
        editingFormContainer.addRow(editingFormContainer.rowCount, Label("Nutrition priority: "), nutritionPriorityInput)

        editingFormScrollWrapper.content = editingFormContainer
    }

    private fun initPreviewContainer() {
        children += previewContainer

        // TODO Create cell live preview

        setTopAnchor(editingFormScrollWrapper, 0.0)
        setLeftAnchor(editingFormScrollWrapper, 0.0)
        setRightAnchor(editingFormScrollWrapper, 0.0)
    }

    private fun initEditingFormScrollWrapper() {
        editingFormScrollWrapper.hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        editingFormScrollWrapper.vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        editingFormScrollWrapper.isFitToWidth = true
        children += editingFormScrollWrapper
        setBottomAnchor(editingFormScrollWrapper, 0.0)
        setLeftAnchor(editingFormScrollWrapper, 0.0)
        setRightAnchor(editingFormScrollWrapper, 0.0)
    }

    private fun updateChildrenPreviews() {
        val genome = selectedGenomeProperty.value
        if (genome != null) {
            val child1Dummy = CellState(
                Vector2(child1Preview.width/2, child1Preview.height/2),
                Vector2(), (child1Preview.width/2).pow(2)*PI*0.8, 0.0, 0.0, genome.children.first
            )
            val child2Dummy = CellState(
                Vector2(child2Preview.width/2, child2Preview.height/2),
                Vector2(), (child2Preview.width/2).pow(2)*PI*0.8, 0.0, 0.0, genome.children.second
            )
            child1Preview.graphicsContext2D.clearRect(0.0, 0.0, child1Preview.width, child1Preview.height)
            child2Preview.graphicsContext2D.clearRect(0.0, 0.0, child2Preview.width, child2Preview.height)
            cellRenderer.render(child1Dummy, dummyWorld, child1Preview.graphicsContext2D)
            cellRenderer.render(child2Dummy, dummyWorld, child2Preview.graphicsContext2D)
        }
    }
}