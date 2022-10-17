package com.devexperts.openhack2022.cell_sandbox.game

import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow
import kotlin.math.sqrt

data class AreaState (
    var width: Double,
    var height: Double,
    var gravity: Vector2,
    var viscosity: Double,
    var radiation: Double,
    var food: ConcurrentHashMap<Long, FoodState> = ConcurrentHashMap(),
    var cells: ConcurrentHashMap<Long, CellState> = ConcurrentHashMap(),
    var borders: ConcurrentHashMap<Long, BorderState> = ConcurrentHashMap()
) {
    fun deepCopy() = copy(
        gravity = gravity.copy(),
        food = food.mapValuesTo(ConcurrentHashMap()) { it.value.deepCopy() },
        cells = cells.mapValuesTo(ConcurrentHashMap()) { it.value.deepCopy() },
        borders = borders.mapValuesTo(ConcurrentHashMap()) { it.value.deepCopy() },
    )
}

data class BorderState(
    val a: Vector2,
    val b: Vector2,
    override var id: Long = -1
): WorldObject {
    fun deepCopy() = copy(a = a.copy(), b = b.copy())
}

data class CellConnectionState (
    val angle: Double,
    var partnerId: Long
)

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

data class FoodState (var center: Vector2, var mass: Double, override var id: Long = -1): WorldObject {
    val radius get() = (mass/Math.PI).pow(1/4.0)

    fun deepCopy() = copy(center = center.copy())
}