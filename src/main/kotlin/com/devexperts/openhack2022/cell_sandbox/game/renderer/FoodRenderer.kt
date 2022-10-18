package com.devexperts.openhack2022.cell_sandbox.game.renderer

import com.devexperts.openhack2022.cell_sandbox.game.FoodState
import com.devexperts.openhack2022.cell_sandbox.game.World
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.shape.ArcType

class FoodRenderer: Renderer<FoodState> {
    override fun render(target: FoodState, world: World, context: GraphicsContext) {
        context.save()
        context.fill = Color.rgb(170, 104, 33, 0.5)
        context.fillArc(
            target.center.x - target.radius,
            target.center.y - target.radius,
            target.radius*2,
            target.radius*2,
            0.0, 360.0,
            ArcType.CHORD
        )
        context.restore()
    }
}