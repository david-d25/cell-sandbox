package com.devexperts.openhack2022.cell_sandbox.game

import com.devexperts.openhack2022.cell_sandbox.game.renderer.CellRenderer
import com.devexperts.openhack2022.cell_sandbox.game.renderer.FoodRenderer
import com.devexperts.openhack2022.cell_sandbox.game.state.AreaState
import com.devexperts.openhack2022.cell_sandbox.game.state.BorderState
import com.devexperts.openhack2022.cell_sandbox.game.updater.CellUpdater
import com.devexperts.openhack2022.cell_sandbox.game.updater.FoodUpdater
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Rectangle2D

class World (val settings: WorldSettings) {

    private val foodRenderer = FoodRenderer()
    private val cellRenderer = CellRenderer()

    private val foodUpdater = FoodUpdater()
    private val cellUpdater = CellUpdater()

    @Volatile
    var area = AreaState(
        200.0,
        100.0,
        Vector2(0, 10),
        0.3,
        0.1,
        mutableSetOf(),
        mutableSetOf(),
        mutableSetOf()
    )

    init {
        area = area.copy(
            borders = mutableSetOf(
                BorderState(Vector2(0, 0), Vector2(area.width, 0)),
                BorderState(Vector2(area.width, 0), Vector2(area.width, area.height)),
                BorderState(Vector2(area.width, area.height), Vector2(0, area.height)),
                BorderState(Vector2(0, area.height), Vector2(0, 0))
            )
        )
    }

    @Synchronized
    fun render(graphics: Graphics2D) {
        Rectangle2D.Double(0.0, 0.0, area.width, area.height).also {
            graphics.color = Color.BLACK
            graphics.draw(it)
            graphics.color = BACKGROUND_COLOR
            graphics.fill(it)
        }

        area.food.forEach { foodRenderer.render(it, this, graphics) }
        area.cells.forEach { cellRenderer.render(it, this, graphics) }
    }

    @Synchronized
    fun update(delta: Double) {
        // TODO some settings should be updated from settings
        val newArea = AreaState(
            width = area.width,
            height = area.height,
            gravity = area.gravity,
            viscosity = area.viscosity,
            radiation = area.radiation,
            food = area.food.flatMapTo(HashSet()) { foodUpdater.update(it, this, delta) },
            cells = area.cells.flatMapTo(HashSet()) { cellUpdater.update(it, this, delta) },
            borders = area.borders
        )
        area = newArea
    }

    companion object {
        val BACKGROUND_COLOR = Color(225, 225, 255)
    }
}