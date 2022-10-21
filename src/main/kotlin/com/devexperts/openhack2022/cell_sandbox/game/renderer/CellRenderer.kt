package com.devexperts.openhack2022.cell_sandbox.game.renderer

import com.devexperts.openhack2022.cell_sandbox.game.CellState
import com.devexperts.openhack2022.cell_sandbox.game.World
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2.RotationDirection.CLOCKWISE
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2.RotationDirection.COUNTERCLOCKWISE
import com.devexperts.openhack2022.cell_sandbox.geom.testCirclesIntersection
import com.devexperts.openhack2022.cell_sandbox.geom.testLineAndCircleIntersection
import com.devexperts.openhack2022.cell_sandbox.geom.testLinesIntersection
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop
import javafx.scene.shape.ArcType
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import kotlin.math.sqrt

class CellRenderer: Renderer<CellState> {
    companion object {
        const val STROKE_WIDTH = 1.0
    }
    override fun render(target: CellState, world: World, context: GraphicsContext) {
        context.save()
        with(target) {

            val visualRadius = radius - STROKE_WIDTH/2

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

            val obstacles = calculateObstacles(target, world).map {
                Pair(
                    it.first + (it.first to target.center).unit() * STROKE_WIDTH/2,
                    it.second + (it.second to target.center).unit() * STROKE_WIDTH/2
                )
            }

            context.beginPath()

            if (obstacles.isEmpty()) {
                context.arc(center.x, center.y, visualRadius, visualRadius, 0.0, 360.0)
            } else if (obstacles.last().second != obstacles.first().first) {
                context.arc(
                    center.x, center.y,
                    visualRadius, visualRadius,
                    Math.toDegrees((center to obstacles.last().second).angle()),
                    Math.toDegrees((center to obstacles.first().first).positiveAngleDiff(center to obstacles.last().second)),
                )
            }

            obstacles.forEachIndexed { index, obstacle ->

                context.lineTo(obstacle.first.x, obstacle.first.y)
                context.lineTo(obstacle.second.x, obstacle.second.y)

                if (index != obstacles.size - 1 && obstacle.second != obstacles[index + 1].first) {
                    val next = obstacles[index + 1]

                    context.arc(
                        center.x, center.y,
                        visualRadius, visualRadius,
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
                renderDebugInfo(target, context, obstacles)
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

    private fun renderDebugInfo(target: CellState, context: GraphicsContext, obstacles: List<Pair<Vector2, Vector2>>) {
        context.save()
        // Angle
        context.lineCap = StrokeLineCap.BUTT
        context.stroke = Color.GREEN
        context.lineWidth = 0.25
        val targetAnglePoint = target.center + Vector2(1, 0).rotate(target.angle) * target.radius
        context.strokeLine(target.center.x, target.center.y, targetAnglePoint.x, targetAnglePoint.y)

        // Split line
        context.stroke = Color.MAGENTA
        context.setLineDashes(1.0, 1.0)
        val splitLinePoint1 = target.center
        val splitLinePoint2 = target.center + Vector2(1, 0)
            .rotate(target.angle + target.genome.splitAngle) * target.radius
        context.strokeLine(splitLinePoint1.x, splitLinePoint1.y, splitLinePoint2.x, splitLinePoint2.y)

        // Obstacles
        context.setLineDashes()
        obstacles.forEach {
            val optimized = Pair(
                it.first + (it.first to target.center)*0.1,
                it.second + (it.second to target.center)*0.1
            )
            context.stroke = LinearGradient(
                optimized.first.x, optimized.first.y,
                optimized.second.x, optimized.second.y,
                false, CycleMethod.REPEAT,
                Stop(0.0, Color.RED),
                Stop(1.0, Color.ORANGE)
            )
            context.strokeLine(optimized.first.x, optimized.first.y, optimized.second.x, optimized.second.y)
        }

        // ID
        context.stroke = Color.BLACK
        context.fill = Color.WHITE
        context.font = Font.font(target.radius/5)
        context.textAlign = TextAlignment.CENTER
        context.strokeText(target.id.toString(), target.center.x, target.center.y)
        context.fillText(target.id.toString(), target.center.x, target.center.y)
        context.restore()
    }

    private fun calculateObstacles(target: CellState, world: World): List<Pair<Vector2, Vector2>> {
        val obstacles = mutableListOf<Pair<Vector2, Vector2>>()

        fun addObstacle(a: Vector2, b: Vector2) {
            obstacles += if ((target.center to a).shortestAngularTurn(target.center to b) == COUNTERCLOCKWISE)
                Pair(a, b)
            else
                Pair(b, a)
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
            if (cell.id == target.id) continue
            val intersections = testCirclesIntersection(target.center, target.radius, cell.center, cell.radius)
            if (intersections.size == 2)
                addObstacle(intersections.first(), intersections.last())
        }

        obstacles.sortWith { a, b ->
            val first = (target.center to a.first).angle()
            val second = (target.center to b.first).angle()
            if (first == second) 0 else if (first > second) 1 else -1
        }

        val obstaclesToRemove = mutableListOf<Pair<Vector2, Vector2>>()
        do {
            obstaclesToRemove.clear()
            obstacles.forEachIndexed { index, obstacle ->
                val next = obstacles[(index + 1) % obstacles.size]
                val obstaclePointA = target.center to obstacle.first
                val obstaclePointB = target.center to obstacle.second
                val nextPointA = target.center to next.first
                val nextPointB = target.center to next.second

                if (obstaclePointA.shortestAngularTurn(nextPointA) == COUNTERCLOCKWISE &&
                    obstaclePointA.shortestAngularTurn(nextPointB) == COUNTERCLOCKWISE &&
                    obstaclePointB.shortestAngularTurn(nextPointB) == CLOCKWISE
                ) {
                    obstaclesToRemove.add(obstacles[(index + 1) % obstacles.size])
                }
            }
            obstacles.removeAll(obstaclesToRemove)
        } while (obstaclesToRemove.isNotEmpty())

        obstacles.forEachIndexed { index, obstacle ->
            val next = obstacles[(index + 1) % obstacles.size]
            val obstaclePointB = target.center to obstacle.second
            val nextPointA = target.center to next.first

            val intersection = testLinesIntersection(obstacle, next)
            if (intersection != null) {
                if (obstaclePointB.shortestAngularTurn(nextPointA) == CLOCKWISE) {
                    if (obstacle.first != intersection && next.second != intersection) {
                        obstacles[index] = obstacle.copy(second = intersection)
                        obstacles[(index + 1) % obstacles.size] = next.copy(first = intersection)
                    }
                }
            }
        }

        return obstacles
    }
}