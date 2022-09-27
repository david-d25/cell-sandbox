package com.devexperts.openhack2022.cell_sandbox.game.renderer

import com.devexperts.openhack2022.cell_sandbox.game.World
import java.awt.Graphics2D

interface Renderer<T> {
    fun render(target: T, world: World, graphics: Graphics2D)
}