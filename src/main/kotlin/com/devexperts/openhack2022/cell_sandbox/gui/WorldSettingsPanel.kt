package com.devexperts.openhack2022.cell_sandbox.gui

import com.devexperts.openhack2022.cell_sandbox.game.World
import com.devexperts.openhack2022.cell_sandbox.game.WorldSettings
import javafx.beans.binding.Bindings
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.control.Slider
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.Font

class WorldSettingsPanel(private val settings: WorldSettings, world: World): VBox() {
    init {
        setMargin(this, Insets(10.0))
        setMinSize(250.0, 0.0)

        val gravityXSlider = Slider(-2.0, 2.0, 0.0)
        gravityXSlider.valueProperty().addListener { _, _, newValue -> settings.gravity.x = newValue.toDouble() }
        val gravityYSlider = Slider(-2.0, 2.0, 0.0)
        gravityYSlider.valueProperty().addListener { _, _, newValue -> settings.gravity.y = newValue.toDouble() }
        val viscositySlider = Slider(0.0, 0.9, 0.1)
        viscositySlider.valueProperty().addListener { _, _, newValue -> settings.viscosity = newValue.toDouble() }
        val radiationSlider = Slider(0.0, 0.9, 0.1)
        radiationSlider.valueProperty().addListener { _, _, newValue -> settings.radiation = newValue.toDouble() }
        val foodSpawnRateSlider = Slider(0.0, 100.0, 100.0)
        foodSpawnRateSlider.valueProperty().addListener { _, _, newValue -> settings.foodSpawnRate = newValue.toInt() }
        val foodSpawnDelaySlider = Slider(0.0, 10.0, 0.0)
        foodSpawnDelaySlider.valueProperty().addListener { _, _, newValue -> settings.foodSpawnDelay = (newValue.toDouble() * 1000).toLong() }
        val foodMassSlider = Slider(0.1, 50.0, 12.0)
        foodMassSlider.valueProperty().addListener { _, _, newValue -> settings.foodMass = newValue.toDouble() }

        setOf(gravityXSlider, gravityYSlider, viscositySlider, radiationSlider).forEach {
            it.isShowTickLabels = true
            it.isShowTickMarks = true
            it.majorTickUnit = 1.0
            it.minorTickCount = 5
            HBox.setHgrow(it, Priority.SOMETIMES)
        }

        val gravityXLabel = Label("Gravity X").also {
            it.textProperty().bind(Bindings.format("%.1f", gravityXSlider.valueProperty()))
        }
        val gravityYLabel = Label("Gravity Y").also {
            it.textProperty().bind(Bindings.format("%.1f", gravityYSlider.valueProperty()))
        }
        val viscosityLabel = Label("Viscosity").also {
            it.textProperty().bind(Bindings.format("%.1f", viscositySlider.valueProperty()))
        }
        val radiationLabel = Label("Radiation").also {
            it.textProperty().bind(Bindings.format("%.1f", radiationSlider.valueProperty()))
        }
        val foodSpawnRateLabel = Label("Food Spawn rate").also {
            it.textProperty().bind(Bindings.format("%.1f", foodSpawnRateSlider.valueProperty()))
        }
        val foodSpawnDelayLabel = Label("Food Spawn delay").also {
            it.textProperty().bind(Bindings.format("%.1f", foodSpawnDelaySlider.valueProperty()))
        }
        val foodMassLabel = Label("Food Spawn delay").also {
            it.textProperty().bind(Bindings.format("%.1f", foodMassSlider.valueProperty()))
        }

        setOf(gravityXLabel, gravityYLabel, viscosityLabel, radiationLabel, foodSpawnRateLabel, foodMassLabel).forEach {
            it.font = Font.font(16.0)
            it.minWidth = 32.0
            it.alignment = Pos.TOP_RIGHT
            setMargin(it, Insets(5.0))
        }

        val debugRenderCheckbox = CheckBox("Debug render").also {
            it.selectedProperty().addListener { _, _, newValue ->
                settings.debugRender = newValue
            }
        }

        val resetButton = Button("Reset").also {
            it.setOnAction {
                world.resetWorld()
            }
        }

        val pauseButton = Button("Pause").also { button ->
            button.setOnAction {
                button.text = if (settings.isWorldPaused) {
                    settings.isWorldPaused = false
                    "Pause"
                } else {
                    settings.isWorldPaused = true
                    "Resume"
                }
            }
        }

        setOf(
            HBox(Label("Gravity X"), gravityXSlider, gravityXLabel),
            HBox(Label("Gravity Y"), gravityYSlider, gravityYLabel),
            HBox(Label("Viscosity"), viscositySlider, viscosityLabel),
            HBox(Label("Radiation"), radiationSlider, radiationLabel),
            HBox(Label("Food spawn rate"), foodSpawnRateSlider, foodSpawnRateLabel),
            HBox(Label("Food Spawn delay"), foodSpawnDelaySlider, foodSpawnDelayLabel),
            HBox(Label("Food mass"), foodMassSlider, foodMassLabel),
            resetButton,
            pauseButton,
            Separator(),
            debugRenderCheckbox
        ).forEach {
            setMargin(it, Insets(5.0))
            children += it
        }

    }
}
