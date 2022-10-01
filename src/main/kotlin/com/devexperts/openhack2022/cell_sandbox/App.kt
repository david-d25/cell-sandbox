package com.devexperts.openhack2022.cell_sandbox

import com.devexperts.openhack2022.cell_sandbox.game.*
import com.devexperts.openhack2022.cell_sandbox.game.state.CellState
import com.devexperts.openhack2022.cell_sandbox.game.state.FoodState
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import com.devexperts.openhack2022.cell_sandbox.gui.AsideTabbedPanel
import com.devexperts.openhack2022.cell_sandbox.gui.WorldSettingsPanel
import com.devexperts.openhack2022.cell_sandbox.gui.WorldView
import java.awt.*
import javax.swing.JFrame
import javax.swing.UIManager

fun main() {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

    val frame = JFrame("Cell Sandbox")
    frame.size = Dimension(800, 600)
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

    val worldSettings = WorldSettings()
    val world = World(worldSettings)

    val camera = Camera(Vector2(world.area.width/2, world.area.height/2), world.area.height*1.2)

    val worldView = WorldView(world, camera)
    val asidePanel = AsideTabbedPanel()
    val worldSettingsPanel = WorldSettingsPanel(worldSettings)

    asidePanel.addTab("World", worldSettingsPanel)

    frame.layout = GridBagLayout()
    frame.contentPane.add(worldView, GridBagConstraints().also {
        it.weightx = 3.0
        it.weighty = 1.0
        it.fill = GridBagConstraints.BOTH
    })
    frame.contentPane.add(asidePanel, GridBagConstraints().also {
        it.gridx = 1
        it.weightx = 1.0
        it.weighty = 1.0
        it.fill = GridBagConstraints.BOTH
    })

    frame.revalidate()
    frame.isVisible = true

    // -------
    repeat(10) {
        val genome = Genome(
            CellType.PHAGOCYTE,
            Math.random(),
            Math.random(),
            Math.random(),
            0.5,
            300.0,
            Math.PI/6, 0.0, 0.0, false, Pair(null, null)
        )
        genome.children = Pair(genome, genome)
        world.area.cells += CellState(
            Vector2(Math.random()*world.area.width, Math.random()*50),
            Vector2(0, 0),
            220.0,
            0.0, genome
        )
    }
    repeat(1000) {
        world.area.food += FoodState(Vector2(Math.random()*world.area.width, Math.random()*world.area.height), 12.0)
    }
    // -------

    Thread {
        // TODO make fixed-delta system
        var oldTime = System.currentTimeMillis()
        while (true) {
            val newTime = System.currentTimeMillis()
            var delta = (newTime - oldTime).toDouble()/1000
            if (delta > 0.05) delta = 0.05
            world.update(delta * 10)
            oldTime = newTime
            Thread.sleep(1000/30)
        }
    }.also { it.isDaemon = true }.start()

    Thread {
        while (true) {
            worldView.repaint()
            Thread.sleep(1000/30)
        }
    }.also { it.isDaemon = true }.start()
}
