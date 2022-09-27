package com.devexperts.openhack2022.cell_sandbox.game.state

import com.devexperts.openhack2022.cell_sandbox.geom.Vector2

data class AreaState (
    val width: Double,
    val height: Double,
    val gravity: Vector2,
    val viscosity: Double,
    val radiation: Double,
    val food: MutableSet<FoodState>,
    val cells: MutableSet<CellState>,
    val borders: MutableSet<BorderState>
)