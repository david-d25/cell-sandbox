package com.devexperts.openhack2022.cell_sandbox.game

import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Arc2D
import kotlin.math.pow

class Food (var center: Vector2, var mass: Double) {
    val radius get() = (mass/Math.PI).pow(1/4)

    fun render(world: World, graphics: Graphics2D) {
        graphics.color = Color(170, 104, 33, 128)
        graphics.fill(Arc2D.Double(
            center.x - radius,
            center.y - radius,
            radius*2,
            radius*2,
            0.0, 360.0,
            Arc2D.CHORD
        ))
    }

    fun update(world: World, delta: Double) {
        mass -= 0.001*delta
        if (mass < 1)
            world.food -= this
    }
}