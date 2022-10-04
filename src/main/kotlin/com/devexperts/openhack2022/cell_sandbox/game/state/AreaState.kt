package com.devexperts.openhack2022.cell_sandbox.game.state

import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import java.util.concurrent.ConcurrentHashMap

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