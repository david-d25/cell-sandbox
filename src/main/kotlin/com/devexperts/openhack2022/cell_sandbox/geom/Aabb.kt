package com.devexperts.openhack2022.cell_sandbox.geom

/**
 * Represents an Axis-Aligned Bounding Box.
 */
data class Aabb
/**
 * @param min upper-left corner of the box
 * @param max lower-right corner of the box
 */
constructor(
    val min: Vector2,
    val max: Vector2
) {
    constructor(minX: Number, minY: Number, maxX: Number, maxY: Number): this(Vector2(minX, minY), Vector2(maxX, maxY))

    val center: Vector2 get() = Vector2((min.x + max.x)/2, (min.y + max.y)/2)
    val width: Double get() = max.x - min.x
    val height: Double get() = max.y - min.y
}