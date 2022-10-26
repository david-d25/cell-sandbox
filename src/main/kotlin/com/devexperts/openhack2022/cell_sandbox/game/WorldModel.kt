package com.devexperts.openhack2022.cell_sandbox.game

import com.devexperts.openhack2022.cell_sandbox.geom.BoxBoundary
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2

interface WorldObject {
    var id: Long
    var boundary: BoxBoundary
}
data class WorldSettings (
    var gravity: Vector2 = Vector2(0, 0.5),
    var viscosity: Double = 0.2,
    var radiation: Double = 0.1,
    var foodSpawnRate: Int = 100,
    var foodSpawnDelay: Long = 0,
    var foodMass: Double = 12.0,
    var isWorldPaused: Boolean = false,
    var initialFoodDensity: Int = 1000,

    // Not for players
    var debugRender: Boolean = false,
    var maxNutritionGainSpeed: Double = 56.0,
    var minCellMass: Double = 75.0,
    var maxCellMass: Double = 500.0,
)

class Camera (var center: Vector2, var height: Double)