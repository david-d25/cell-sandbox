package com.devexperts.openhack2022.cell_sandbox.game.updater

import com.devexperts.openhack2022.cell_sandbox.game.World

interface Updater<T> {
    fun update(target: T, world: World, delta: Double): Set<T>
}