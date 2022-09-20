package com.devexperts.openhack2022.cell_sandbox

import com.devexperts.openhack2022.cell_sandbox.game.*
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import com.devexperts.openhack2022.cell_sandbox.gui.AreaView
import java.awt.Dimension
import javax.swing.JFrame

fun main() {
    val frame = JFrame("Cell Sandbox")
    frame.size = Dimension(800, 600)
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.isVisible = true

    val world = World(500.0, 300.0, Vector2(0, 10), 0.8, 0.1)
    val camera = Camera(Vector2(world.width/2, world.height/2), world.height*1.2)

    val areaView = AreaView(world, camera)

    frame.contentPane.add(areaView)

    // -------
    repeat(2) {
        val genome = Genome(
            CellType.PHAGOCYTE,
            Math.random(),
            Math.random(),
            Math.random(),
            0.5,
            300.0,
            Math.PI/6, 0.0, 0.0, Pair(null, null)
        )
        genome.children = Pair(genome, genome)
        world.cells += Cell(
            Vector2(Math.random()*world.width, Math.random()*50),
            Vector2(0, 0),
            120.0,
            0.0, genome
        )
    }
    repeat(2500) {
        world.food += Food(Vector2(Math.random()*world.width, Math.random()*world.height), 12.0)
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
            areaView.repaint()
            Thread.sleep(10)
        }
    }.start()
}
