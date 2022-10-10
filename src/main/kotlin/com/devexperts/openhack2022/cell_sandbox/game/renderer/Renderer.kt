package com.devexperts.openhack2022.cell_sandbox.game.renderer

import com.devexperts.openhack2022.cell_sandbox.game.World
import javafx.scene.canvas.GraphicsContext

interface Renderer<T> {
    fun render(target: T, world: World, context: GraphicsContext)
}