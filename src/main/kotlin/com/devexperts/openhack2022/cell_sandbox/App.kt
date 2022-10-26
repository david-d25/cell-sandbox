package com.devexperts.openhack2022.cell_sandbox

import com.devexperts.openhack2022.cell_sandbox.game.Camera
import com.devexperts.openhack2022.cell_sandbox.game.World
import com.devexperts.openhack2022.cell_sandbox.game.WorldSettings
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import com.devexperts.openhack2022.cell_sandbox.gui.GenomeEditingPanel
import com.devexperts.openhack2022.cell_sandbox.gui.WorldSettingsPanel
import com.devexperts.openhack2022.cell_sandbox.gui.WorldViewHudWrapper
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
        stage.scene = Scene(layout, 1000.0, 600.0)

        val worldSettings = WorldSettings()
        val world = World(worldSettings)
        val camera = Camera(Vector2(world.area.width/2, world.area.height/2), world.area.height*1.2)

        layout.children += SplitPane().apply {
            setDividerPositions(0.8)
            items.addAll(
                WorldViewHudWrapper(world, camera),
                TabPane(
                    Tab("World", WorldSettingsPanel(world)).apply { isClosable = false },
                    Tab("Genome", GenomeEditingPanel(world)).apply { isClosable = false }
                ).apply {
                    SplitPane.setResizableWithParent(this, false)
                    prefWidth = stage.scene.width / 5
                    minWidth = 340.0
                }
            )
        }

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
