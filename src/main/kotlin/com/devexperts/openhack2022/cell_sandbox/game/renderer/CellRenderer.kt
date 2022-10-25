package com.devexperts.openhack2022.cell_sandbox.game.renderer

import com.devexperts.openhack2022.cell_sandbox.game.CellState
import com.devexperts.openhack2022.cell_sandbox.game.CellType
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
import java.lang.Math.toDegrees
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.IntStream
import kotlin.math.*

class CellRenderer: Renderer<CellState> {
    companion object {
        const val STROKE_WIDTH = 1.0
    }

    private val obstaclesByCellId = ConcurrentHashMap<Long, List<Pair<Vector2, Vector2>>>()

    override fun render(world: World, context: GraphicsContext) {
        context.save()

        val cellsList = world.area.cells.values.toList()
        IntStream.range(0, world.area.cells.size)
            .parallel()
            .forEach { index ->
                val cell = cellsList[index]
                obstaclesByCellId[cell.id] = calculateObstacles(cell, world)
            }

        world.area.cells.values.forEach { cell ->
            drawCellBody(context, cell, obstaclesByCellId[cell.id]!!)
            drawNucleus(context, cell)

            if (cell.genome.type == CellType.FLAGELLOCYTE)
                drawFlagellum(context, cell)
        }

        world.area.cells.values.filter { it.connections.isNotEmpty() }.forEach { cell ->
            drawConnections(context, cell, world)
        }

        if (world.settings.debugRender) {
            world.area.cells.values.forEach { cell ->
                renderDebugInfo(cell, context, obstaclesByCellId[cell.id]!!)
            }
        }

        obstaclesByCellId.clear()
        context.restore()
    }

    private fun drawConnections(
        context: GraphicsContext,
        cell: CellState,
        world: World
    ) {

        for ((partnerId, connection) in cell.connections) {
            if (partnerId <= cell.id)
                continue

            val partner = world.area.cells[partnerId]
            if (partner != null) {
                val otherConnection = partner.connections[cell.id]
                if (otherConnection != null) {

                    repeat(4) { stringId ->
                        val angleOffset = stringId * Math.PI / 24 - 1.5 * Math.PI / 24

                        val lineStart = cell.center + Vector2.unit(cell.angle + connection.angle + angleOffset) * cell.radius * 0.5
                        val lineEnd = partner.center + Vector2.unit(partner.angle + otherConnection.angle - angleOffset) * partner.radius * 0.5

                        context.fill = LinearGradient(
                            lineStart.x, lineStart.y,
                            lineEnd.x, lineEnd.y,
                            false,
                            CycleMethod.NO_CYCLE,
                            Stop(0.0, Color.TRANSPARENT),
                            Stop(0.5, getCellWallColor(cell).interpolate(getCellWallColor(partner).darker(), 0.5)),
                            Stop(1.0, Color.TRANSPARENT)
                        )

                        val cellControlPointForward = cell.center + Vector2.unit(cell.angle + connection.angle + angleOffset*2 + PI/45) * cell.radius * 0.7
                        val partnerControlPointForward = partner.center + Vector2.unit(partner.angle + otherConnection.angle - angleOffset*2 - PI/45) * partner.radius * 0.7
                        val cellControlPointBackward = cell.center + Vector2.unit(cell.angle + connection.angle + angleOffset*2 - PI/45) * cell.radius * 0.7
                        val partnerControlPointBackward = partner.center + Vector2.unit(partner.angle + otherConnection.angle - angleOffset*2 + PI/45) * partner.radius * 0.7

                        context.beginPath()
                        context.moveTo(lineStart.x, lineStart.y)
                        context.bezierCurveTo(cellControlPointForward.x, cellControlPointForward.y, partnerControlPointForward.x, partnerControlPointForward.y, lineEnd.x, lineEnd.y)
                        context.bezierCurveTo(partnerControlPointBackward.x, partnerControlPointBackward.y, cellControlPointBackward.x, cellControlPointBackward.y, lineStart.x, lineStart.y)
                        context.closePath()
                        context.fill()
                    }
                }
            }
        }
    }

    private fun drawFlagellum(context: GraphicsContext, cell: CellState) {
        context.save()

        val areaToScreenTransform = context.transform
        if (areaToScreenTransform.determinant() == 0.0)
            return

        val granularity = 2 * cell.radius * areaToScreenTransform.mxx

        context.fill = getCellWallColor(cell)
        context.beginPath()

        context.translate(cell.center.x, cell.center.y)
        context.rotate(toDegrees(cell.angle + PI))
        context.scale(cell.radius, cell.radius)
        context.translate(cos(asin(0.3)), 0.0)

        val time = cell.age * cell.genome.flagellumForce

        var x = 0.0

        while (x < 2.0) {
            context.lineTo(x, 0.3 * x * sin(5 * x - time) / (x + 1) - 0.3 + 0.15 * x)
            x += 2.0 / granularity
        }

        while (x >= 0.0) {
            context.lineTo(x, 0.3 * x * sin(5 * x - time) / (x + 1) + 0.3 - 0.15 * x)
            x -= 2.0 / granularity
        }

        context.arc(-cos(asin(0.3)), 0.0, 1.0, 1.0, -toDegrees(asin(0.3)), 2 * toDegrees(asin(0.3)))

        context.closePath()
        context.fill()

        context.restore()
    }

    private fun drawNucleus(context: GraphicsContext, cell: CellState) {
        context.fill = getCellWallColor(cell)
        context.fillArc(
            cell.center.x - sqrt(cell.radius), cell.center.y - sqrt(cell.radius),
            sqrt(cell.radius)*2, sqrt(cell.radius)*2,
            0.0, 360.0, ArcType.CHORD
        )
    }

    private fun drawCellBody(
        context: GraphicsContext,
        cell: CellState,
        obstacles: List<Pair<Vector2, Vector2>>
    ) {
        context.apply {
            lineWidth = STROKE_WIDTH
            fill = getCellCytoplasmColor(cell)
            stroke = getCellWallColor(cell)
            lineCap = StrokeLineCap.ROUND
            lineJoin = StrokeLineJoin.ROUND
        }

        listOf(
            obstacles.map {
                Pair(
                    it.first + (it.first to cell.center).unit() * STROKE_WIDTH,
                    it.second + (it.second to cell.center).unit() * STROKE_WIDTH
                )
            },
            obstacles.map {
                Pair(
                    it.first + (it.first to cell.center).unit() * STROKE_WIDTH/2,
                    it.second + (it.second to cell.center).unit() * STROKE_WIDTH/2
                )
            }
        ).forEachIndexed { listIndex, currentObstacles ->
            context.beginPath()

            val visualRadius = if (listIndex == 0)
                cell.radius - STROKE_WIDTH
            else
                cell.radius - STROKE_WIDTH/2

            if (currentObstacles.isEmpty()) {
                context.arc(cell.center.x, cell.center.y, visualRadius, visualRadius, 0.0, 360.0)
            } else if (currentObstacles.last().second != currentObstacles.first().first) {
                context.arc(
                    cell.center.x, cell.center.y,
                    visualRadius, visualRadius,
                    toDegrees((cell.center to currentObstacles.last().second).angle()),
                    toDegrees(
                        (cell.center to currentObstacles.first().first)
                            .positiveAngleDiff(cell.center to currentObstacles.last().second)
                    ),
                )
            }

            currentObstacles.forEachIndexed { index, obstacle ->

                context.lineTo(obstacle.first.x, obstacle.first.y)
                context.lineTo(obstacle.second.x, obstacle.second.y)

                if (index != currentObstacles.size - 1 && obstacle.second != currentObstacles[index + 1].first) {
                    val next = currentObstacles[index + 1]

                    context.arc(
                        cell.center.x, cell.center.y,
                        visualRadius, visualRadius,
                        toDegrees((cell.center to obstacle.second).angle()),
                        toDegrees((cell.center to next.first).angle() - (cell.center to obstacle.second).angle())
                    )
                }
            }

            context.closePath()
            if (listIndex == 0)
                context.fill()
            else
                context.stroke()
        }
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
        context.setLineDashes(0.5, 0.5)
        val child1Pointer = target.center + Vector2.unit(target.angle + target.genome.splitAngle) * target.radius * 0.8
        val splitLinePoint1 = target.center + Vector2.unit(
            target.angle + target.genome.splitAngle + PI/2
        ) * target.radius * 0.5
        val splitLinePoint2 = target.center + Vector2.unit(
            target.angle + target.genome.splitAngle - PI/2
        ) * target.radius * 0.5
        context.strokeLine(target.center.x, target.center.y, child1Pointer.x, child1Pointer.y)
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

        // Speed
        context.stroke = Color.YELLOW
        context.strokeLine(
            target.center.x, target.center.y,
            target.center.x + target.speed.x, target.center.y + target.speed.y
        )

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
            if (intersection != null &&
                obstaclePointB.shortestAngularTurn(nextPointA) == CLOCKWISE && obstacle.first != intersection &&
                next.second != intersection
            ) {
                obstacles[index] = obstacle.copy(second = intersection)
                obstacles[(index + 1) % obstacles.size] = next.copy(first = intersection)
            }
        }

        return obstacles
    }

    private fun getCellCytoplasmColor(cell: CellState) = Color(
        1 - cell.genome.cyanPigment,
        1 - cell.genome.magentaPigment,
        1 - cell.genome.yellowPigment,
        (cell.genome.cyanPigment + cell.genome.magentaPigment + cell.genome.yellowPigment)/3
    )

    private fun getCellWallColor(cell: CellState) = getCellCytoplasmColor(cell).interpolate(Color.BLACK, 0.4)
}