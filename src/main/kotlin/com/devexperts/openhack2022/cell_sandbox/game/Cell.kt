package com.devexperts.openhack2022.cell_sandbox.game

import com.devexperts.openhack2022.cell_sandbox.util.projectPointOnLine
import com.devexperts.openhack2022.cell_sandbox.util.testCirclesIntersection
import com.devexperts.openhack2022.cell_sandbox.util.testLineAndCircleIntersection
import com.devexperts.openhack2022.cell_sandbox.util.testLinesIntersection
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Arc2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

class Cell (
    var center: Vector2,
    var speed: Vector2,
    var mass: Double,
    var angle: Double,
    val genome: Genome
) {
    companion object {
        const val COLLISION_FACTOR = 100
    }

    val radius get() = sqrt(mass/Math.PI)

    fun render(world: World, graphics: Graphics2D) {
        graphics.stroke = BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER)

        val rgb = Color(
            (1 - genome.cyanPigment).toFloat(),
            (1 - genome.magentaPigment).toFloat(),
            (1 - genome.yellowPigment).toFloat()
        )

        val obstacles = mutableListOf<Pair<Vector2, Vector2>>()

        fun addObstacle(a: Vector2, b: Vector2) {
            val angleA = center.to(a).angle()
            val angleB = center.to(b).angle()
            val diff = angleB - angleA
            obstacles += if (diff < -PI || diff in 0.0..PI) Pair(a, b) else Pair(b, a)
        }

        for (wall in world.walls) {
            val intersections = testLineAndCircleIntersection(center, this.radius, wall.first, wall.second)
            if (intersections.isNotEmpty()) {
                val a = intersections.first()
                val b = if (intersections.size == 2) intersections.last() else center.nearest(wall.first, wall.second)
                addObstacle(a, b)
            }
        }

        for (cell in world.cells) {
            if (cell == this) continue
            val intersections = testCirclesIntersection(center, radius, cell.center, cell.radius)
            if (intersections.size == 2)
                addObstacle(intersections.first(), intersections.last())
        }

        obstacles.sortWith { a, b ->
            if (center.to(a.first).angle() == center.to(b.first).angle()) 0 else
            if (center.to(a.first).angle() > center.to(b.first).angle()) 1 else -1
        }

        obstacles.forEachIndexed { index, obstacle ->
            if (index != obstacles.size - 1) {
                val next = obstacles[index + 1]
                val intersection = testLinesIntersection(obstacle, next)
                if (intersection != null) {
                    obstacles[index] = obstacle.copy(second = intersection)
                    obstacles[index + 1] = next.copy(first = intersection)
                }
            }
        }

        if (obstacles.size > 1) {
            val a = obstacles.first().first
            val b = obstacles.last().first
            if (center.to(b).angle() > center.to(a).angle()) {
                val intersection = testLinesIntersection(obstacles.last(), obstacles.first())
                if (intersection != null) {
                    obstacles[0] = obstacles[0].copy(first = intersection)
                    obstacles[obstacles.size - 1] = obstacles[obstacles.size - 1].copy(second = intersection)
                }
            }
        }

        Arc2D.Double(
            center.x - radius,
            center.y - radius,
            radius*2,
            radius*2,
            if (obstacles.isEmpty()) 0.0 else Math.toDegrees(center.to(obstacles.last().second).angle()),
            if (obstacles.isEmpty()) 360.0 else if (center.to(obstacles.last().second).angle() > center.to(obstacles.first().first).angle()) 360 - Math.toDegrees(center.to(obstacles.last().second).angle() - center.to(obstacles.first().first).angle()) else Math.toDegrees(center.to(obstacles.first().first).angle() - center.to(obstacles.last().second).angle()),
            Arc2D.PIE
        ).also {
            graphics.color = rgb
//            graphics.color = rgb.darker().darker()
            graphics.fill(it)
            graphics.color = rgb.darker()
//            graphics.color = rgb.darker().darker().darker()
            it.arcType = Arc2D.OPEN
            graphics.draw(it)
        }

        obstacles.forEachIndexed { index, obstacle ->
            val triangle = Path2D.Double().also {
                it.moveTo(center.x, center.y)
                it.lineTo(obstacle.first.x, obstacle.first.y)
                it.lineTo(obstacle.second.x, obstacle.second.y)
                it.closePath()
            }

            graphics.color = rgb
            graphics.fill(triangle)

            val obstacleShape = Line2D.Double(obstacle.first.x, obstacle.first.y, obstacle.second.x, obstacle.second.y)

            graphics.color = rgb.darker()
            graphics.draw(obstacleShape)

            if (index != obstacles.size - 1) {
                val next = obstacles[index + 1]

                val shape = Arc2D.Double(
                    center.x - radius,
                    center.y - radius,
                    radius*2,
                    radius*2,
                    Math.toDegrees(center.to(obstacle.second).angle()),
                    Math.toDegrees(center.to(next.first).angle() - center.to(obstacle.second).angle()),
                    Arc2D.PIE
                )

                graphics.color = rgb
                graphics.fill(shape)
                graphics.color = rgb.darker()
                graphics.draw(shape.also { it.arcType = Arc2D.OPEN })
            }
        }

        graphics.color = rgb.darker()
        graphics.fill(Arc2D.Double(
            center.x - sqrt(radius), center.y - sqrt(radius),
            sqrt(radius)*2, sqrt(radius)*2,
            0.0, 360.0,
            Arc2D.CHORD
        ))
    }

    fun update(world: World, delta: Double) {
        speed += world.gravity * delta

        for (cell in world.cells) {
            if (cell == this) continue
            val intersections = testCirclesIntersection(center, radius, cell.center, cell.radius)
            if (intersections.size == 2) {
                val projection = (intersections.first() + intersections.last())/2
                val depth = 1 - center.distance(projection)/radius
                val oppositeForce = projection.to(center)
                val hardnessCoefficient = (-1/(depth/2 - 1) - 1).pow(1/genome.hardness)
                val dumpingForce = -speed
                speed += (oppositeForce * COLLISION_FACTOR * hardnessCoefficient + dumpingForce) * delta
            }
        }

        for (wall in world.walls) {
            val intersections = testLineAndCircleIntersection(center, radius, wall.first, wall.second)
            if (intersections.isNotEmpty()) {
                val projection = projectPointOnLine(center, wall)
                val depth = 1 - projection.to(center).length/radius
                val oppositeForce = projection.to(center)
                val hardnessCoefficient = (-1/(depth/2 - 1) - 1).pow(1/genome.hardness)
                val speedCompensation = speed.length * depth * (-1/(genome.hardness + 1) + 1) + 1
                speed += oppositeForce * hardnessCoefficient * speedCompensation * COLLISION_FACTOR * delta
            }
        }

        val viscosityForce = -this.speed * world.viscosity
        this.speed += viscosityForce * delta

        center += speed * delta

        world.food.forEach {
            if (this.center.distance(it.center) < radius + it.radius) {
                mass += it.mass
                world.food.remove(it)
            }
        }

        if (mass > genome.splitMass)
            split(world)

        center.x = center.x.coerceIn(0.0..world.width)
        center.y = center.y.coerceIn(0.0..world.height)
    }

    private fun split(world: World) {
        world.cells.remove(this)

        val child1 = Cell(center, speed, mass/2, angle + genome.child1Angle, genome.child1Genome!!)
        val child2 = Cell(center, speed, mass/2, angle + genome.child2Angle, genome.child2Genome!!)

        child1.center += Vector2(1, 0).rotate(angle + genome.splitAngle)
        child1.center += Vector2(1, 0).rotate(angle - genome.splitAngle)

        world.cells.add(child1)
        world.cells.add(child2)
    }

    private fun kill(world: World) {
        world.cells.remove(this)
        world.food.add(Food(center, mass/2))
    }
}