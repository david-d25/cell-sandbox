package com.devexperts.openhack2022.cell_sandbox.game

import com.devexperts.openhack2022.cell_sandbox.game.renderer.CellRenderer
import com.devexperts.openhack2022.cell_sandbox.game.renderer.FoodRenderer
import com.devexperts.openhack2022.cell_sandbox.game.state.AreaState
import com.devexperts.openhack2022.cell_sandbox.game.state.BorderState
import com.devexperts.openhack2022.cell_sandbox.game.state.CellState
import com.devexperts.openhack2022.cell_sandbox.game.state.FoodState
import com.devexperts.openhack2022.cell_sandbox.game.updater.CellUpdater
import com.devexperts.openhack2022.cell_sandbox.game.updater.FoodUpdater
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Rectangle2D
import java.util.concurrent.atomic.AtomicLong

class World (val settings: WorldSettings) {

    private val foodRenderer = FoodRenderer()
    private val cellRenderer = CellRenderer()

    private val updaters = listOf(FoodUpdater(), CellUpdater())

    private val idCounter = AtomicLong()

    @Volatile
    var area = AreaState(
        200.0,
        200.0,
        Vector2(0, 1),
        0.2,
        0.1
    )

    init {
        add(BorderState(Vector2(0, 0), Vector2(area.width, 0)))
        add(BorderState(Vector2(area.width, 0), Vector2(area.width, area.height)))
        add(BorderState(Vector2(area.width, area.height), Vector2(0, area.height)))
        add(BorderState(Vector2(0, area.height), Vector2(0, 0)))
    }

    fun newId() = idCounter.getAndIncrement()

    fun add(wo: WorldObject) {
        if (wo.id == -1L)
            wo.id = newId()

        if (wo is BorderState)
            area.borders[wo.id] = wo
        if (wo is FoodState)
            area.food[wo.id] = wo
        if (wo is CellState)
            area.cells[wo.id] = wo
    }

    fun remove(wo: WorldObject) {
        area.borders.remove(wo.id)
        area.food.remove(wo.id)
        area.cells.remove(wo.id)
    }

    @Synchronized
    fun render(graphics: Graphics2D) {
        Rectangle2D.Double(0.0, 0.0, area.width, area.height).also {
            graphics.color = Color.BLACK
            graphics.draw(it)
            graphics.color = BACKGROUND_COLOR
            graphics.fill(it)
        }

        area.food.values.forEach { foodRenderer.render(it, this, graphics) }
        area.cells.values.forEach { cellRenderer.render(it, this, graphics) }
    }

    @Synchronized
    fun update(delta: Double) {
        val oldArea = area
        area = area.deepCopy()
        area.gravity = settings.gravity
        area.viscosity = settings.viscosity
        area.radiation = settings.radiation
        updaters.forEach { it.update(this, oldArea, area, delta) }
        add(FoodState(Vector2(Math.random()*area.width, Math.random()*area.height), 12.0))
    }

    companion object {
        val BACKGROUND_COLOR = Color(225, 225, 255)
    }
}