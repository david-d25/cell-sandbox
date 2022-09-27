package com.devexperts.openhack2022.cell_sandbox.game.updater

import com.devexperts.openhack2022.cell_sandbox.game.World
import com.devexperts.openhack2022.cell_sandbox.game.state.FoodState
import kotlin.math.min

class FoodUpdater: Updater<FoodState> {
    override fun update(target: FoodState, world: World, delta: Double): Set<FoodState> {
        var newMass = target.mass
        world.area.cells.forEach {
            if (it.center.distance(target.center) < it.radius + target.radius) {
                val massToEat = min(target.mass, world.settings.maxFoodAbsorbingSpeed)
                newMass -= massToEat * delta
            }
        }
        newMass -= 0.001 * delta
        return if (newMass < 1) emptySet() else setOf(target.copy(mass = newMass))
    }
}