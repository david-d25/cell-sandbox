package com.devexperts.openhack2022.cell_sandbox.game

import com.devexperts.openhack2022.cell_sandbox.geom.Vector2

data class WorldSettings (
    var gravity: Vector2 = Vector2(0, 0.5),
    var viscosity: Double = 0.2,
    var radiation: Double = 0.1,
    var foodSpawnRate: Int = 100,
    var foodSpawnDelay: Long = 0,
    var foodMass: Double = 12.0,

    // Not for players
    var debugRender: Boolean = false,
    var maxFoodAbsorbingSpeed: Double = 4.0,
    var minCellMass: Double = 75.0,
)
