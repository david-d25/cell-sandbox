package com.devexperts.openhack2022.cell_sandbox.game.updater

import com.devexperts.openhack2022.cell_sandbox.game.AreaState
import com.devexperts.openhack2022.cell_sandbox.game.World

class FoodUpdater: Updater {
    override fun update(world: World, oldArea: AreaState, newArea: AreaState, delta: Double) {
        newArea.food.values.forEach {
            it.mass -= 0.1 * delta
            if (it.mass < 0.1)
                world.remove(it)
        }
    }
}