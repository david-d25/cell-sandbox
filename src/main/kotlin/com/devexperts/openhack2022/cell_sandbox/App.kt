package com.devexperts.openhack2022.cell_sandbox

import com.devexperts.openhack2022.cell_sandbox.game.*
import com.devexperts.openhack2022.cell_sandbox.game.state.CellState
import com.devexperts.openhack2022.cell_sandbox.game.state.FoodState
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import com.devexperts.openhack2022.cell_sandbox.gui.GenomeEditingPanel
import com.devexperts.openhack2022.cell_sandbox.gui.WorldSettingsPanel
import com.devexperts.openhack2022.cell_sandbox.gui.WorldView
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.stage.Stage

class App: Application() {
    override fun start(stage: Stage) {
        stage.title = "Cell Sandbox"

        val layout = StackPane()
        stage.scene = Scene(layout, 800.0, 600.0)

        val worldSettings = WorldSettings()
        val world = World(worldSettings)

        val camera = Camera(Vector2(world.area.width/2, world.area.height/2), world.area.height*1.2)

        val worldView = WorldView(world, camera)

        val asidePanel = TabPane()
        val worldSettingsPanel = WorldSettingsPanel(worldSettings, world)
        val genomeEditingPanel = GenomeEditingPanel()

        val worldSettingsTab = Tab("World", worldSettingsPanel)
        val genomeEditingTab = Tab("Genome", genomeEditingPanel)

        worldSettingsTab.isClosable = false
        genomeEditingTab.isClosable = false

        asidePanel.tabs.addAll(worldSettingsTab, genomeEditingTab)

        val hBox = HBox(worldView, asidePanel)
        layout.children += hBox

        HBox.setHgrow(worldView, Priority.ALWAYS)

        world.fillWorld()

        Thread {
            // TODO make fixed-delta system
            var oldTime = System.currentTimeMillis()
            while (true) {
                val newTime = System.currentTimeMillis()
                var delta = (newTime - oldTime).toDouble()/1000
                if (delta > 0.05) delta = 0.05
                world.update(delta * 5)
                oldTime = newTime
                Thread.sleep(1000/60)
            }
        }.also { it.isDaemon = true }.start()

        stage.show()
    }
}
