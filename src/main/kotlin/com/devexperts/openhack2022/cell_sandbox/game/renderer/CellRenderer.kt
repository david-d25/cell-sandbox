package com.devexperts.openhack2022.cell_sandbox.game.renderer

import com.devexperts.openhack2022.cell_sandbox.game.World
import com.devexperts.openhack2022.cell_sandbox.game.state.CellState
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import com.devexperts.openhack2022.cell_sandbox.geom.testCirclesIntersection
import com.devexperts.openhack2022.cell_sandbox.geom.testLineAndCircleIntersection
import com.devexperts.openhack2022.cell_sandbox.geom.testLinesIntersection
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.geom.Arc2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import kotlin.math.PI
import kotlin.math.sqrt


class CellRenderer: Renderer<CellState> {
    companion object {
        const val STROKE_WIDTH = 1f
    }
    override fun render(target: CellState, world: World, graphics: Graphics2D) {
        with(target) {
            graphics.stroke = BasicStroke(STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL)

            val rgb = Color(
                (1 - genome.cyanPigment).toFloat(),
                (1 - genome.magentaPigment).toFloat(),
                (1 - genome.yellowPigment).toFloat()
            )

            val obstacles = calculateObstacles(target, world)

            val cellShape = Path2D.Double()
            cellShape.append(Arc2D.Double(
                center.x - radius, center.y - radius,
                radius*2, radius*2,
                if (obstacles.isEmpty()) 0.0 else Math.toDegrees(center.to(obstacles.last().second).angle()),
                if (obstacles.isEmpty()) 360.0 else
                    if (center.to(obstacles.last().second).angle() > center.to(obstacles.first().first).angle())
                        360 - Math.toDegrees(center.to(obstacles.last().second).angle() - center.to(obstacles.first().first).angle())
                    else
                        Math.toDegrees(center.to(obstacles.first().first).angle() - center.to(obstacles.last().second).angle()),
                Arc2D.OPEN
            ), false)

            obstacles.forEachIndexed { index, obstacle ->

                cellShape.append(Line2D.Double(
                    obstacle.first.x, obstacle.first.y,
                    obstacle.second.x, obstacle.second.y
                ), true)

                if (index != obstacles.size - 1 && obstacle.second != obstacles[index + 1].first) {
                    val next = obstacles[index + 1]

                    cellShape.append(Arc2D.Double(
                        center.x - radius,
                        center.y - radius,
                        radius*2,
                        radius*2,
                        Math.toDegrees(center.to(obstacle.second).angle()),
                        Math.toDegrees(center.to(next.first).angle() - center.to(obstacle.second).angle()),
                        Arc2D.OPEN
                    ), true)
                }
            }

            cellShape.closePath()
            graphics.color = rgb.darker()
            graphics.fill(cellShape)
            graphics.color = rgb

            val oldTransform = graphics.transform
            graphics.transform(AffineTransform.getTranslateInstance(target.center.x, target.center.y))
            graphics.transform(AffineTransform.getScaleInstance(0.9, 0.9))
            graphics.transform(AffineTransform.getTranslateInstance(-target.center.x, -target.center.y))
            graphics.fill(cellShape)
            graphics.transform = oldTransform

            graphics.color = rgb.darker()
            graphics.fill(
                Arc2D.Double(
                    center.x - sqrt(radius), center.y - sqrt(radius),
                    sqrt(radius) *2, sqrt(radius) *2,
                    0.0, 360.0,
                    Arc2D.CHORD
                ))
        }
    }

    private fun calculateObstacles(target: CellState, world: World): List<Pair<Vector2, Vector2>> {
        val obstacles = mutableListOf<Pair<Vector2, Vector2>>()

        fun addObstacle(a: Vector2, b: Vector2) {
            val angleA = target.center.to(a).angle()
            val angleB = target.center.to(b).angle()
            val diff = angleB - angleA
            obstacles += if (diff < -PI || diff in 0.0..PI) Pair(a, b) else Pair(b, a)
        }

        for (border in world.area.borders) {
            val intersections = testLineAndCircleIntersection(target.center, target.radius, border.a, border.b)
            if (intersections.isNotEmpty()) {
                val a = intersections.first()
                val b = if (intersections.size == 2) intersections.last() else target.center.nearest(border.a, border.b)
                addObstacle(a, b)
            }
        }

        for (cell in world.area.cells) {
            if (cell === target) continue
            val intersections = testCirclesIntersection(target.center, target.radius, cell.center, cell.radius)
            if (intersections.size == 2)
                addObstacle(intersections.first(), intersections.last())
        }

        obstacles.sortWith { a, b ->
            if (target.center.to(a.first).angle() == target.center.to(b.first).angle()) 0 else
                if (target.center.to(a.first).angle() > target.center.to(b.first).angle()) 1 else -1
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
            if (target.center.to(b).angle() > target.center.to(a).angle()) {
                val intersection = testLinesIntersection(obstacles.last(), obstacles.first())
                if (intersection != null) {
                    obstacles[0] = obstacles[0].copy(first = intersection)
                    obstacles[obstacles.size - 1] = obstacles[obstacles.size - 1].copy(second = intersection)
                }
            }
        }

        return obstacles
    }
}