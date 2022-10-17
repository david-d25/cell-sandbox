package com.devexperts.openhack2022.cell_sandbox.game.updater

import com.devexperts.openhack2022.cell_sandbox.game.AreaState
import com.devexperts.openhack2022.cell_sandbox.game.World
import kotlin.math.min

class FoodUpdater: Updater {
    override fun update(world: World, oldArea: AreaState, newArea: AreaState, delta: Double) {
        newArea.food.values.forEach {
            var newMass = it.mass
            // TODO use a common World-supported interface to detect and process collisions
            world.area.cells.values.forEach { cell ->
                if (cell.center.distance(it.center) < cell.radius + it.radius) {
                    val massToEat = min(it.mass, world.settings.maxFoodAbsorbingSpeed)
                    newMass -= massToEat * delta
                }
            }
            newMass -= 0.001 * delta
            if (newMass < 1)
                world.remove(it)
            else
                it.mass = newMass
        }
    }
}