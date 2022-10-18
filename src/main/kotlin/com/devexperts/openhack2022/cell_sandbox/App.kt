package com.devexperts.openhack2022.cell_sandbox

import com.devexperts.openhack2022.cell_sandbox.game.Camera
import com.devexperts.openhack2022.cell_sandbox.game.World
import com.devexperts.openhack2022.cell_sandbox.game.WorldSettings
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import com.devexperts.openhack2022.cell_sandbox.gui.GenomeEditingPanel
import com.devexperts.openhack2022.cell_sandbox.gui.WorldSettingsPanel
import com.devexperts.openhack2022.cell_sandbox.gui.WorldView
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.SplitPane
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
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
        val worldSettingsPanel = WorldSettingsPanel(world)
        val genomeEditingPanel = GenomeEditingPanel(world)

        val worldSettingsTab = Tab("World", worldSettingsPanel)
        val genomeEditingTab = Tab("Genome", genomeEditingPanel)

        worldSettingsTab.isClosable = false
        genomeEditingTab.isClosable = false

        asidePanel.tabs.addAll(worldSettingsTab, genomeEditingTab)
        asidePanel.prefWidth = stage.scene.width / 5
        asidePanel.minWidth = 320.0

        val mainBox = SplitPane(worldView, asidePanel)
        mainBox.setDividerPositions(0.8)
        SplitPane.setResizableWithParent(asidePanel, false)

        layout.children += mainBox

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
