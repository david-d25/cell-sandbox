package com.devexperts.openhack2022.cell_sandbox.game.renderer

import com.devexperts.openhack2022.cell_sandbox.game.World
import com.devexperts.openhack2022.cell_sandbox.game.state.FoodState
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Arc2D

class FoodRenderer: Renderer<FoodState> {
    override fun render(target: FoodState, world: World, graphics: Graphics2D) {
        graphics.color = Color(170, 104, 33, 128)
        graphics.fill(
            Arc2D.Double(
            target.center.x - target.radius,
            target.center.y - target.radius,
            target.radius*2,
            target.radius*2,
            0.0, 360.0,
            Arc2D.CHORD
        ))
    }
}