package com.devexperts.openhack2022.cell_sandbox.gui

import com.devexperts.openhack2022.cell_sandbox.game.Camera
import com.devexperts.openhack2022.cell_sandbox.game.CellState
import com.devexperts.openhack2022.cell_sandbox.game.World
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.SVGPath

class WorldViewHudWrapper(private val world: World, private val camera: Camera): AnchorPane() {
    private lateinit var worldView: WorldView
    private lateinit var toolsPanel: VBox
    private lateinit var toolParametersPanel: HBox

    private val defaultToggleGroup = ToggleGroup()
    private val availableTools = listOf(SelectorTool(), AddCellTool(), RemoveCellTool())
    private var currentTool = availableTools.first()

    init {
        initMainComponents()
        initTools()
        initListeners()

        toolsPanel.style = "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;"
    }

    private fun initListeners() {
        worldView.setOnMouseClicked {
            if (it.isStillSincePress)
                currentTool.onAreaClick(camera.unproject(Vector2(it.x, it.y)))
        }
    }

    private fun initTools() {
        availableTools.forEach { tool ->
            toolsPanel.children.add(ToggleButton().apply {
                minWidth = 40.0
                minHeight = 40.0

                toggleGroup = defaultToggleGroup
                setOnMouseClicked {
                    currentTool = tool
                    isSelected = true
                    toolParametersPanel.children.clear()
                    toolParametersPanel.children.addAll(tool.getParameters())
                }

                graphic = tool.getIcon()
            })
        }
        defaultToggleGroup.toggles.first().isSelected = true
    }

    private fun initMainComponents() {
        worldView = WorldView(world, camera).apply {
            setLeftAnchor(this, 0.0)
            setRightAnchor(this, 0.0)
            setTopAnchor(this, 0.0)
            setBottomAnchor(this, 0.0)
        }

        toolsPanel = VBox(5.0).apply {
            setLeftAnchor(this, 5.0)
            setTopAnchor(this, 50.0)
            setBottomAnchor(this, 0.0)
            alignment = Pos.TOP_CENTER
        }

        toolParametersPanel = HBox(5.0).apply {
            setLeftAnchor(this, 60.0)
            setTopAnchor(this, 5.0)
            setRightAnchor(this, 0.0)
            alignment = Pos.CENTER_LEFT
        }

        children += worldView
        children += toolsPanel
        children += toolParametersPanel
    }

    inner class SelectorTool : Tool {
        override fun onAreaClick(areaPoint: Vector2) {
            if (world.area.cells.size == 0)
                return

            val target = world.area.cells.values.minBy { areaPoint.distance(it.center) }
            if (areaPoint.distance(target.center) < target.radius)
                world.selectedCellIdProperty.set(target.id)
            else
                world.selectedCellIdProperty.set(-1)
        }

        override fun getIcon() = SVGPath().apply {
            content = "M 1 1 l 15 5 l -3.5 3.5 l 5 5 l -3 3 l -5 -5 l -3.5 3.5 z"
            stroke = Color.BLACK
            fill = Color.WHITE
        }

        override fun getParameters() = emptyList<Node>()
    }

    inner class AddCellTool : Tool {
        private val selector = GenomeSelector(world.genomeLibrary).apply {
            minWidth = 300.0
        }

        override fun onAreaClick(areaPoint: Vector2) {
            selector.selectionProperty.value?.let { genome ->
                world.add(
                    CellState(
                        areaPoint,
                        Vector2(),
                        world.settings.maxCellMass * 0.75,
                        0.0,
                        0.0,
                        genome.deepCopy()
                    )
                )
            }
        }

        override fun getIcon(): Node { // This renders incorrectly
            return AnchorPane(
                Circle(7.0, 8.0, 8.0).apply {
                    stroke = Color.grayRgb(170)
                    fill = Color.grayRgb(230)
                    strokeWidth = 2.0
                    setLeftAnchor(this, 0.0)
                    setTopAnchor(this, 2.0)
                },
                Circle(8.0, 8.0, 3.0, Color.grayRgb(170)).apply {
                    setLeftAnchor(this, 6.0)
                    setTopAnchor(this, 8.0)
                },
                SVGPath().apply {
                    content = "M 10 8 h 4 v 4 h 4 v 4 h -4 v 4 h -4 v -4 h -4 v -4 h 4 z"
                    fill = Color.rgb(11, 191, 8)
                    setRightAnchor(this, 0.0)
                    setBottomAnchor(this, 2.0)
                }
            ).apply {
                padding = Insets(2.0)
            }
        }

        override fun getParameters() = listOf(selector)
    }

    inner class RemoveCellTool : Tool {
        override fun onAreaClick(areaPoint: Vector2) {
            if (world.area.cells.size == 0)
                return

            val target = world.area.cells.values.minBy { areaPoint.distance(it.center) }
            if (areaPoint.distance(target.center) < target.radius)
                world.remove(target)
        }

        override fun getIcon(): Node {
            return AnchorPane(
                Circle(7.0, 8.0, 8.0).apply {
                    stroke = Color.grayRgb(170)
                    fill = Color.grayRgb(230)
                    strokeWidth = 2.0
                    setLeftAnchor(this, 0.0)
                    setTopAnchor(this, 4.0)
                },
                Circle(8.0, 8.0, 3.0, Color.grayRgb(170)).apply {
                    setLeftAnchor(this, 6.0)
                    setTopAnchor(this, 10.0)
                },
                SVGPath().apply {
                    content = "M 6 12 h 12 v 4 h -12"
                    fill = Color.RED
                    setRightAnchor(this, 0.0)
                    setBottomAnchor(this, 4.0)
                }
            ).apply {
                padding = Insets(2.0)
            }
        }

        override fun getParameters() = emptyList<Node>()
    }

    internal interface Tool {
        fun onAreaClick(areaPoint: Vector2)
        fun getIcon(): Node
        fun getParameters(): List<Node>
    }
}