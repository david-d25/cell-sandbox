package com.devexperts.openhack2022.cell_sandbox.game

import com.devexperts.openhack2022.cell_sandbox.geom.Vector2

class WorldSettings {
    var gravity = Vector2(0, 0.5)
    var viscosity = 0.2
    var radiation = 0.1
    var foodSpawnRate = 0

    var maxFoodAbsorbingSpeed = 5.0
    var minCellMass = 75.0
}