package com.devexperts.openhack2022.cell_sandbox.game.updater

import com.devexperts.openhack2022.cell_sandbox.game.*
import kotlin.math.min

class CellNutritionUpdater : Updater {
    override fun update(world: World, oldArea: AreaState, newArea: AreaState, delta: Double) {
        val cellMaxFoodGain = world.settings.maxNutritionGainSpeed * delta
        val nutritionGainByCell = HashMap<Long, Double>()

        updateNutritionExchanges(world, newArea, nutritionGainByCell, delta)

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

    private fun updateNutritionExchanges(
        world: World,
        newArea: AreaState,
        nutritionGainByCell: Map<Long, Double>,
        delta: Double
    ) {
        val cellMaxFoodGain = world.settings.maxNutritionGainSpeed * delta

        newArea.cells.values.filter { it.connections.isNotEmpty() }.sortedBy { it.id }.forEach { cell ->

            cell.connections.keys.filter { it > cell.id }.forEach { partnerId ->
                val partner = newArea.cells[partnerId]!!
                val nutritionRatio = cell.mass / partner.mass
                val targetNutritionRatio = if (cell.genome.nutritionPriority + partner.genome.nutritionPriority != 0.0)
                    cell.genome.nutritionPriority / partner.genome.nutritionPriority else 1.0

                val maxCellConsumableMass = min(
                    cellMaxFoodGain - nutritionGainByCell.getOrDefault(cell.id, 0.0),
                    world.settings.maxCellMass - cell.mass
                )

                val maxPartnerConsumableMass = min(
                    cellMaxFoodGain - nutritionGainByCell.getOrDefault(partnerId, 0.0),
                    world.settings.maxCellMass - partner.mass
                )

                // Positive -> nutrition moves to this cell, negative -> ...to partner cell
                val nutritionTransitionFactor = (targetNutritionRatio - nutritionRatio) / (targetNutritionRatio + nutritionRatio)
                val nutritionTransitionAmount = (nutritionTransitionFactor * cellMaxFoodGain)
                    .coerceIn(-maxPartnerConsumableMass, maxCellConsumableMass) // Can't exceed max nutrition gain speed
                    .coerceIn(-cell.mass, partner.mass) // Can't get from other cell more that its mass

                cell.mass += nutritionTransitionAmount
                partner.mass -= nutritionTransitionAmount
            }
        }
    }

    private fun calculateLiveCost(cell: CellState): Double {
        return 0.005 * cell.mass
    }
}