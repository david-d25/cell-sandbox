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
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
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
    fun render(context: GraphicsContext) {
        context.fill = Color.BLACK
        context.strokeRect(0.0, 0.0, area.width, area.height)
        context.fill = BACKGROUND_COLOR
        context.fillRect(0.0, 0.0, area.width, area.height)

        area.food.values.forEach { foodRenderer.render(it, this, context) }
        area.cells.values.forEach { cellRenderer.render(it, this, context) }
    }

    @Synchronized
    fun update(delta: Double) {
        val oldArea = area
        area = area.deepCopy()
        area.gravity = settings.gravity
        area.viscosity = settings.viscosity
        area.radiation = settings.radiation
        // TODO this has to be replaced with a parallel and stable implementation in future
        updaters.forEach { it.update(this, oldArea, area, delta) }

        val randomNumber = Math.random() * 100 + 1
        if (randomNumber <= settings.foodSpawnRate) {
            add(FoodState(Vector2(Math.random() * area.width, Math.random() * area.height), 12.0))
        }
    }

    companion object {
        val BACKGROUND_COLOR: Color = Color.rgb(225, 225, 255)
    }
}
