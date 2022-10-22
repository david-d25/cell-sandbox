package com.devexperts.openhack2022.cell_sandbox.game.updater

import com.devexperts.openhack2022.cell_sandbox.game.*
import kotlin.math.min

class CellNutritionUpdater : Updater {
    override fun update(world: World, oldArea: AreaState, newArea: AreaState, delta: Double) {
        val cellMaxFoodGain = world.settings.maxNutritionGainSpeed * delta
        val nutritionGainByCell = HashMap<Long, Double>()

        newArea.cells.values.forEach { cell ->
            val oldCell = if (oldArea.cells.contains(cell.id)) oldArea.cells[cell.id]!! else cell

            val liveCost = calculateLiveCost(cell) * delta
            cell.mass -= liveCost
            nutritionGainByCell[cell.id] = -liveCost

            if (cell.genome.type == CellType.PHAGOCYTE) {
                var maxConsumableMass = min(
                    cellMaxFoodGain - nutritionGainByCell[cell.id]!!,
                    world.settings.maxCellMass - cell.mass
                )

                oldArea.food.values.forEach { food ->
                    if (oldCell.center.distance(food.center) < oldCell.radius + food.radius) {
                        val massToEat = food.mass.coerceIn(0.0, maxConsumableMass)
                        cell.mass += massToEat
                        newArea.food[food.id]!!.mass -= massToEat
                        maxConsumableMass -= massToEat
                        nutritionGainByCell.compute(cell.id) { _, v -> v?.plus(massToEat) }
                    }
                }
            }
        }
    }

    private fun calculateLiveCost(cell: CellState): Double {
        return 0.005 * cell.mass
    }
}