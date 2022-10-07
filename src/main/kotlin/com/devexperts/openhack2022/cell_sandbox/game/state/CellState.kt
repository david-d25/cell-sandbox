package com.devexperts.openhack2022.cell_sandbox.game.state

import com.devexperts.openhack2022.cell_sandbox.game.Genome
import com.devexperts.openhack2022.cell_sandbox.game.WorldObject
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

data class CellState (
    var center: Vector2,
    var speed: Vector2,
    var mass: Double,
    var angle: Double,
    var angularSpeed: Double,
    var genome: Genome,
    var connections: Map<Long, CellConnectionState> = emptyMap(),
    override var id: Long = -1
): WorldObject {

    val radius get() = sqrt(mass/Math.PI)

    fun deepCopy() = copy(
        center = center.copy(),
        speed = speed.copy(),
        genome = genome.deepCopy(),
        connections = ConcurrentHashMap(connections.mapValues { it.value.copy() })
    )

    companion object {
        const val MIN_MASS = 100
    }
}