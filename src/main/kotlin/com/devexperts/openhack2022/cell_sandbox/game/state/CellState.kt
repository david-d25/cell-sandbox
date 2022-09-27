package com.devexperts.openhack2022.cell_sandbox.game.state

import com.devexperts.openhack2022.cell_sandbox.game.Genome
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import kotlin.math.sqrt

data class CellState (
    val center: Vector2,
    val speed: Vector2,
    val mass: Double,
    val angle: Double,
    val genome: Genome
) {
    val radius get() = sqrt(mass/Math.PI)
}