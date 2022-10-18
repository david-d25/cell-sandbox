package com.devexperts.openhack2022.cell_sandbox.game.updater

import com.devexperts.openhack2022.cell_sandbox.game.AreaState
import com.devexperts.openhack2022.cell_sandbox.game.World

interface Updater {
    fun update(world: World, oldArea: AreaState, newArea: AreaState, delta: Double)
}