package com.devexperts.openhack2022.cell_sandbox.game.state

import com.devexperts.openhack2022.cell_sandbox.game.WorldObject
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2

data class BorderState(
    val a: Vector2,
    val b: Vector2,
    override var id: Long = -1
): WorldObject {
    fun deepCopy() = copy(a = a.copy(), b = b.copy())
}