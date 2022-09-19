package com.devexperts.openhack2022.cell_sandbox.game

import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Rectangle2D
import java.util.concurrent.ConcurrentLinkedDeque

class World (
    val width: Double,
    val height: Double,
    val gravity: Vector2 = Vector2(0, 0),
    val viscosity: Double = 0.0,
    val radiation: Double = 0.0
) {
    companion object {
        val BACKGROUND_COLOR = Color(225, 225, 255)
    }

    val food: ConcurrentLinkedDeque<Food> = ConcurrentLinkedDeque()
    val cells: ConcurrentLinkedDeque<Cell> = ConcurrentLinkedDeque()

    val walls = arrayOf(
        Pair(Vector2(0, 0), Vector2(width, 0)),
        Pair(Vector2(width, 0), Vector2(width, height)),
        Pair(Vector2(width, height), Vector2(0, height)),
        Pair(Vector2(0, height), Vector2(0, 0))
    )

    fun render(graphics: Graphics2D) {
        Rectangle2D.Double(0.0, 0.0, width, height).also {
            graphics.color = Color.BLACK
            graphics.draw(it)
            graphics.color = BACKGROUND_COLOR
            graphics.fill(it)
        }

        food.forEach { it.render(this, graphics) }
        cells.forEach { it.render(this, graphics) }
    }

    fun update(delta: Double) {
        repeat(10) {
            food += Food(Vector2(Math.random() * width, Math.random() * height), 25.0)
        }
        food.forEach { it.update(this, delta) }
        cells.forEach { it.update(this, delta) }
    }
}