package com.devexperts.openhack2022.cell_sandbox

import com.devexperts.openhack2022.cell_sandbox.game.*
import com.devexperts.openhack2022.cell_sandbox.game.state.CellState
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import com.devexperts.openhack2022.cell_sandbox.gui.WorldView
import java.awt.Dimension
import javax.swing.JFrame

fun main() {
    val frame = JFrame("Cell Sandbox")
    frame.size = Dimension(800, 600)
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

    val worldSettings = WorldSettings()
    val world = World(worldSettings)

    val camera = Camera(Vector2(world.area.width/2, world.area.height/2), world.area.height*1.2)

    val worldView = WorldView(world, camera)

    frame.contentPane.add(worldView)
    frame.revalidate()
    frame.isVisible = true

    // -------
    repeat(50) {
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
//        world.area.food += FoodState(Vector2(Math.random()*world.area.width, Math.random()*world.area.height), 12.0)
    }
    // -------

    Thread {
        // TODO make fixed-delta system
        var oldTime = System.currentTimeMillis()
        while (true) {
            val newTime = System.currentTimeMillis()
            var delta = (newTime - oldTime).toDouble()/1000
            if (delta > 0.1) delta = 0.1
            world.update(delta)
            oldTime = newTime
            Thread.sleep(1000/60)
        }
    }.also { it.isDaemon = true }.start()

    Thread {
        while (true) {
            worldView.repaint()
            Thread.sleep(1000/30)
        }
    }.also { it.isDaemon = true }.start()
}
