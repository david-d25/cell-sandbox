package com.devexperts.openhack2022.cell_sandbox.geom

import kotlin.math.*

data class Vector2 (var x: Double, var y: Double) {
    constructor (x: Number, y: Number): this(x.toDouble(), y.toDouble())
    constructor (): this(0, 0)

    val length get() = sqrt(x.pow(2) + y.pow(2))

    operator fun plus(that: Vector2) =     Vector2(x + that.x, y + that.y)
    operator fun minus(that: Vector2) =    Vector2(x - that.x, y - that.y)
    operator fun times(factor: Number) =    Vector2(x*factor.toDouble(), y*factor.toDouble())
    operator fun div(factor: Number) =      Vector2(x/factor.toDouble(), y/factor.toDouble())
    operator fun unaryMinus() =             Vector2(-x, -y)

    fun dot(that: Vector2) = x*that.x + y*that.y
    fun to(that: Vector2) = that - this
    fun nearest(vararg vectors: Vector2) = vectors.reduce { a, b -> if (distance(a) > distance(b)) b else a }

    fun angle() = if (y > 0) 2*Math.PI - acos(x/length) else acos(x/length)

    fun distance(that: Vector2) = sqrt((x - that.x).pow(2) + (y - that.y).pow(2))

    fun rotate(angle: Double) = Vector2(x*cos(angle) - y*sin(angle), x*sin(angle) + y*cos(angle))

    fun unit() = Vector2(x/length, y/length)

    fun isNaN() = this.x.isNaN() || this.y.isNaN()

    companion object {
        fun unit(angle: Double) = Vector2(cos(angle), sin(angle))
    }
}