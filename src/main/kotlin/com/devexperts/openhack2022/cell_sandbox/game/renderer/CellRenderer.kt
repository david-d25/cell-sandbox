package com.devexperts.openhack2022.cell_sandbox.game.renderer

import com.devexperts.openhack2022.cell_sandbox.game.CellState
import com.devexperts.openhack2022.cell_sandbox.game.World
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import com.devexperts.openhack2022.cell_sandbox.geom.testCirclesIntersection
import com.devexperts.openhack2022.cell_sandbox.geom.testLineAndCircleIntersection
import com.devexperts.openhack2022.cell_sandbox.geom.testLinesIntersection
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.shape.*
import kotlin.math.PI
import kotlin.math.sqrt

class CellRenderer: Renderer<CellState> {
    companion object {
        const val STROKE_WIDTH = 1.0
    }
    override fun render(target: CellState, world: World, context: GraphicsContext) {
        context.save()
        with(target) {

            val rgb = Color(
                1 - genome.cyanPigment,
                1 - genome.magentaPigment,
                1 - genome.yellowPigment,
                1.0
            )

            context.apply {
                lineWidth = STROKE_WIDTH
                fill = rgb
                stroke = rgb.darker()
                lineCap = StrokeLineCap.ROUND
                lineJoin = StrokeLineJoin.ROUND
            }

            val obstacles = calculateObstacles(target, world)

            context.beginPath()

            context.arc(
                center.x, center.y,
                radius, radius,
                if (obstacles.isEmpty()) 0.0 else Math.toDegrees((center to obstacles.last().second).angle()),
                if (obstacles.isEmpty()) 360.0 else
                    if ((center to obstacles.last().second).angle() > (center to obstacles.first().first).angle())
                        360 - Math.toDegrees((center to obstacles.last().second).angle() - (center to obstacles.first().first).angle())
                    else
                        Math.toDegrees((center to obstacles.first().first).angle() - (center to obstacles.last().second).angle()),
            )

            obstacles.forEachIndexed { index, obstacle ->

                context.lineTo(obstacle.second.x, obstacle.second.y)

                if (index != obstacles.size - 1 && obstacle.second != obstacles[index + 1].first) {
                    val next = obstacles[index + 1]

                    context.arc(
                        center.x, center.y,
                        radius, radius,
                        Math.toDegrees((center to obstacle.second).angle()),
                        Math.toDegrees((center to next.first).angle() - (center to obstacle.second).angle())
                    )
                }
            }

            context.closePath()
            context.fill()
            context.stroke()

            // Nucleus
            context.fill = rgb.darker()
            context.fillArc(
                center.x - sqrt(radius), center.y - sqrt(radius),
                sqrt(radius)*2, sqrt(radius)*2,
                0.0, 360.0, ArcType.CHORD
            )

            // Connections
            drawConnections(context, target, world)

            if (world.settings.debugRender)
                renderDebugInfo(target, context)
        }
        context.restore()
    }

    private fun drawConnections(
        context: GraphicsContext,
        target: CellState,
        world: World
    ) {
        context.stroke = Color.rgb(0, 0, 0, 0.4)
        for ((partnerId, connection) in target.connections) {
            val partner = world.area.cells[partnerId]
            if (partner != null) {
                val lineStart = target.center + Vector2.unit(target.angle + connection.angle) * target.radius * 0.5
                val otherConnection = partner.connections[target.id]
                if (otherConnection != null) {
                    val otherSurfacePoint =
                        partner.center + Vector2.unit(partner.angle + otherConnection.angle) * partner.radius * 0.5
                    context.strokeLine(
                        lineStart.x, lineStart.y, otherSurfacePoint.x, otherSurfacePoint.y
                    )
                }
            }
        }
    }

    private fun getSurfacePointByAngle(
        target: CellState,
        angle: Double,
        obstacles: List<Pair<Vector2, Vector2>>
    ): Vector2 {
        val start = target.center
        val end = target.center + Vector2.unit(angle) * target.radius
        obstacles.forEach {
            val intersection = testLinesIntersection(it, Pair(start, end))
            if (intersection != null)
                return intersection
        }
        return end
    }

    private fun renderDebugInfo(target: CellState, context: GraphicsContext) {
        context.save()
        // Angle
        context.stroke = Color.GREEN
        context.lineWidth = 0.5
        val targetAnglePoint = target.center + Vector2(1, 0).rotate(target.angle) * target.radius
        context.strokeLine(target.center.x, target.center.y, targetAnglePoint.x, targetAnglePoint.y)

        // Split line
        context.stroke = Color.MAGENTA
        context.setLineDashes(1.0, 1.0)
        val splitLinePoint1 = target.center
        val splitLinePoint2 = target.center + Vector2(1, 0).rotate(target.angle + target.genome.splitAngle) * target.radius
        context.strokeLine(splitLinePoint1.x, splitLinePoint1.y, splitLinePoint2.x, splitLinePoint2.y)
        context.restore()
    }

    private fun calculateObstacles(target: CellState, world: World): List<Pair<Vector2, Vector2>> {
        val obstacles = mutableListOf<Pair<Vector2, Vector2>>()

        fun addObstacle(a: Vector2, b: Vector2) {
            val angleA = (target.center to a).angle()
            val angleB = (target.center to b).angle()
            val diff = angleB - angleA
            obstacles += if (diff < -PI || diff in 0.0..PI) Pair(a, b) else Pair(b, a)
        }

        for (border in world.area.borders.values) {
            val intersections = testLineAndCircleIntersection(target.center, target.radius, border.a, border.b)
            if (intersections.isNotEmpty()) {
                val a = intersections.first()
                val b = if (intersections.size == 2) intersections.last() else target.center.nearest(border.a, border.b)
                addObstacle(a, b)
            }
        }

        for (cell in world.area.cells.values) {
            if (cell === target) continue
            val intersections = testCirclesIntersection(target.center, target.radius, cell.center, cell.radius)
            if (intersections.size == 2)
                addObstacle(intersections.first(), intersections.last())
        }

        obstacles.sortWith { a, b ->
            if ((target.center to a.first).angle() == (target.center to b.first).angle()) 0 else
                if ((target.center to a.first).angle() > (target.center to b.first).angle()) 1 else -1
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
            if ((target.center to b).angle() > (target.center to a).angle()) {
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