package com.devexperts.openhack2022.cell_sandbox.game.state

import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import kotlin.math.pow

data class FoodState (val center: Vector2, val mass: Double) {
    val radius get() = (mass/Math.PI).pow(1/4)
}