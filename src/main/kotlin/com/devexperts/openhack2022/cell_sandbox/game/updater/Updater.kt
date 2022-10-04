package com.devexperts.openhack2022.cell_sandbox.game.updater

import com.devexperts.openhack2022.cell_sandbox.game.World
import com.devexperts.openhack2022.cell_sandbox.game.state.AreaState

interface Updater {
    fun update(world: World, oldArea: AreaState, newArea: AreaState, delta: Double)
}