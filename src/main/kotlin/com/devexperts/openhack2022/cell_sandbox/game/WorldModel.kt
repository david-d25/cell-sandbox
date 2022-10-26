package com.devexperts.openhack2022.cell_sandbox.game

import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import javafx.scene.transform.Affine

interface WorldObject {
    var id: Long
}
data class WorldSettings (
    var gravity: Vector2 = Vector2(0, 0.5),
    var viscosity: Double = 0.2,
    var radiation: Double = 0.0,
    var foodSpawnRate: Int = 100,
    var foodSpawnDelay: Long = 0,
    var foodMass: Double = 40.0,
    var isWorldPaused: Boolean = false,
    var initialFoodDensity: Int = 1000,

    // Not for players
    var debugRender: Boolean = false,
    var maxNutritionGainSpeed: Double = 56.0,
    var minCellMass: Double = 100.0,
    var maxCellMass: Double = 500.0,
    var showFps: Boolean = true,
    var fpsUpdateIntervalMs: Long = 500,

    var flagellocyteFlagellumForceCost: Double = 0.1,
    val flagellocyteFlagellumMinForce: Double = 0.0,
    val flagellocyteFlagellumMaxForce: Double = 10.0
)

class Camera (center: Vector2, height: Double) {
    var center = center
        set(value) { field = value; updateTransform() }
    var height = height
        set(value) { field = value; updateTransform() }
    var viewportWidth = 1.0
        set(value) { field = value; updateTransform() }
    var viewportHeight = 1.0
        set(value) { field = value; updateTransform() }

    var transform = Affine()
        private set

    private fun updateTransform() {
        val scale = viewportHeight/height
        transform = Affine(
            scale, 0.0, viewportWidth/2 - scale*center.x,
            0.0, scale, viewportHeight/2 - scale*center.y
        )
    }

    // From area space to screen space
    fun project(point: Vector2): Vector2 {
        val result = transform.transform(point.x, point.y)
        return Vector2(result.x, result.y)
    }

    // From screen space to area space
    fun unproject(point: Vector2): Vector2 {
        val result = transform.inverseTransform(point.x, point.y)
        return Vector2(result.x, result.y)
    }
}