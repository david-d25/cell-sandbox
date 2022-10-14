package com.devexperts.openhack2022.cell_sandbox.game.state

import com.devexperts.openhack2022.cell_sandbox.game.WorldObject
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import kotlin.math.pow

data class FoodState (var center: Vector2, var mass: Double, override var id: Long = -1): WorldObject {
    val radius get() = (mass/Math.PI).pow(0.25)

    fun deepCopy() = copy(center = center.copy())
}