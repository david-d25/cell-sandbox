package com.devexperts.openhack2022.cell_sandbox.game

import com.devexperts.openhack2022.cell_sandbox.game.renderer.CellRenderer
import com.devexperts.openhack2022.cell_sandbox.game.renderer.FoodRenderer
import com.devexperts.openhack2022.cell_sandbox.game.updater.CellActionUpdater
import com.devexperts.openhack2022.cell_sandbox.game.updater.CellNutritionUpdater
import com.devexperts.openhack2022.cell_sandbox.game.updater.CellPhysicsUpdater
import com.devexperts.openhack2022.cell_sandbox.game.updater.FoodUpdater
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import java.util.concurrent.atomic.AtomicLong

class World (val settings: WorldSettings) {

    private val foodRenderer = FoodRenderer()
    private val cellRenderer = CellRenderer()

    private val updaters = listOf(
        CellPhysicsUpdater(),
        CellNutritionUpdater(),
        CellActionUpdater(),
        FoodUpdater()
    )

    private val idCounter = AtomicLong()
    private var foodGenerationLastTime = System.currentTimeMillis()

    @Volatile
    var area = AreaState(
        200.0,
        200.0,
        Vector2(0, 1),
        0.2,
        0.1
    )

    val genomeLibrary = mutableMapOf<Genome, String>()

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

        if (wo is CellState) {
            wo.connections.keys.forEach { id ->
                area.cells[id]!!.connections = area.cells[id]!!.connections.filterKeys { it != wo.id }
            }
        }
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
        // Do not update if the game is paused
        if (settings.isWorldPaused) return

        val oldArea = area
        area = area.deepCopy()
        area.gravity = settings.gravity
        area.viscosity = settings.viscosity
        area.radiation = settings.radiation

        updaters.forEach { it.update(this, oldArea, area, delta) }

        val randomNumber = Math.random() * 100 + 1
        val currentTime = System.currentTimeMillis()
        if (randomNumber <= settings.foodSpawnRate && currentTime - foodGenerationLastTime >= settings.foodSpawnDelay) {
            add(FoodState(Vector2(Math.random() * area.width, Math.random() * area.height), settings.foodMass))
            foodGenerationLastTime = currentTime
        }
    }

    @Synchronized
    fun resetWorld() {
        area.borders.clear()
        area.cells.clear()
        area.food.clear()
        fillWorld()
    }

    @Synchronized
    fun fillWorld() {
        add(BorderState(Vector2(0, 0), Vector2(area.width, 0)))
        add(BorderState(Vector2(area.width, 0), Vector2(area.width, area.height)))
        add(BorderState(Vector2(area.width, area.height), Vector2(0, area.height)))
        add(BorderState(Vector2(0, area.height), Vector2(0, 0)))

        repeat(1) {
            val genome = Genome(
                CellType.PHAGOCYTE,
                Math.random(),
                Math.random(),
                Math.random(),
                0.5,
                300.0,
                Math.PI/6, 0.0, 0.0, true, true, true
            )
            add(
                CellState(
                    Vector2(Math.random() * area.width, Math.random() * 50),
                    Vector2(0, 0),
                    220.0,
                    0.0,
                    0.0,
                    genome
                )
            )
        }
        repeat(settings.initialFoodDensity) {
            add(FoodState(Vector2(Math.random() * area.width, Math.random() * area.height), settings.foodMass))
        }
    }

    companion object {
        val BACKGROUND_COLOR: Color = Color.rgb(225, 225, 255)
    }
}
