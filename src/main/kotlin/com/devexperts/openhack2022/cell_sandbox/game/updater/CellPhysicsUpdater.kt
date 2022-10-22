package com.devexperts.openhack2022.cell_sandbox.game.updater

import com.devexperts.openhack2022.cell_sandbox.game.*
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import com.devexperts.openhack2022.cell_sandbox.geom.projectPointOnLine
import com.devexperts.openhack2022.cell_sandbox.geom.testCirclesIntersection
import com.devexperts.openhack2022.cell_sandbox.geom.testLineAndCircleIntersection
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sin

class CellPhysicsUpdater : Updater {
    companion object {
        const val CELL_STICKINESS_DEPTH = 3
    }

    override fun update(world: World, oldArea: AreaState, newArea: AreaState, delta: Double) {
        newArea.cells.values.forEach { cell ->
            val oldCell = if (oldArea.cells.contains(cell.id)) oldArea.cells[cell.id]!! else cell

            cell.connections = cell.connections.filterValues { !oldArea.cells.contains(it.partnerId) }

            cell.speed += world.area.gravity * delta

            for (border in oldArea.borders.values) {
                processBorderCollision(oldCell, border, cell, delta)
            }

            for (other in oldArea.cells.values) {
                processCellCollision(other, cell, oldCell, delta)
            }

            for (connection in oldCell.connections.values) {
                processConnectionPhysics(oldArea, connection, cell, oldCell, delta)
            }

            cell.center += cell.speed * delta
            cell.speed -= cell.speed * world.area.viscosity * delta

            cell.angle += cell.angularSpeed * delta
            cell.angularSpeed -= cell.angularSpeed * world.area.viscosity * delta
        }
    }

    private fun processBorderCollision(
        oldCell: CellState,
        border: BorderState,
        cell: CellState,
        delta: Double
    ) {
        val intersections = testLineAndCircleIntersection(oldCell.center, oldCell.radius, border.a, border.b)
        if (intersections.isNotEmpty()) {
            val projection = projectPointOnLine(oldCell.center, Pair(border.a, border.b))
            val oppositeForce =
                (projection to oldCell.center).unit() * (oldCell.radius - projection.distance(oldCell.center))
            val depth = (1 - oldCell.center.distance(projection) / oldCell.radius).coerceIn(0.0, 1.0)
            val hardnessCoefficient = (oldCell.genome.hardness * depth + 1).pow(oldCell.genome.hardness + 1)
            cell.speed += oppositeForce * hardnessCoefficient * delta
        }
    }

    private fun processCellCollision(
        other: CellState,
        cell: CellState,
        oldCell: CellState,
        delta: Double
    ) {
        if (other.id == cell.id)
            return

        if (cell.center.distance(other.center) <= cell.radius + other.radius) {
            val intersections = testCirclesIntersection(
                oldCell.center, oldCell.radius,
                other.center, other.radius
            )

            val pivot =
                if (intersections.isNotEmpty())
                    (intersections.first() + intersections.last()) / 2
                else
                    (oldCell.center + other.center) / 2

            val depth = (1 - oldCell.center.distance(pivot) / oldCell.radius).coerceIn(0.0, 1.0)

            val massSum = oldCell.mass + other.mass
            val thisMassCoefficient = oldCell.mass / massSum
            val oppositeForce = (pivot to oldCell.center).unitSafe() * (oldCell.radius - pivot.distance(oldCell.center))
            val hardnessCoefficient = (oldCell.genome.hardness * depth + 1).pow(oldCell.genome.hardness + 1)
            cell.speed += (
                    oppositeForce * hardnessCoefficient + (
                            other.speed * thisMassCoefficient - oldCell.speed * (1 - thisMassCoefficient)
                            ) * oldCell.genome.hardness
                    ) * delta
        }
    }

    private fun processConnectionPhysics(
        oldArea: AreaState,
        connection: CellConnectionState,
        cell: CellState,
        oldCell: CellState,
        delta: Double
    ) {
        val partner = oldArea.cells[connection.partnerId]
        if (partner != null) {
            val partnerConnection = partner.connections[cell.id]
            if (partnerConnection != null) {
                // 4 is picked as balance between physical stability and computational load
                repeat(4) { stringId ->
                    val angleOffset = stringId * Math.PI / 24 - Math.PI / 12
                    val effectiveConnectionAngle = oldCell.angle + connection.angle + angleOffset
                    val effectiveConnectionForceOrigin =
                        oldCell.center + Vector2.unit(effectiveConnectionAngle) * oldCell.radius - Vector2.unit(
                            effectiveConnectionAngle
                        ) * CELL_STICKINESS_DEPTH
                    val effectiveConnectionForceDestination = partner.center +
                            Vector2.unit(partner.angle + partnerConnection.angle - angleOffset) * partner.radius -
                            Vector2.unit(partner.angle + partnerConnection.angle - angleOffset) * CELL_STICKINESS_DEPTH
                    val connectionForceDirection =
                        (effectiveConnectionForceOrigin to effectiveConnectionForceDestination) / 4 * delta
                    applyImpulse(oldCell, cell, effectiveConnectionForceOrigin, connectionForceDirection)
                }
            }
        }
    }

    private fun applyImpulse(oldCell: CellState, cell: CellState, impulseOrigin: Vector2, impulseDirection: Vector2) {
        if (impulseDirection.length == 0.0)
            return
        val originToCenterAngle = (impulseOrigin to oldCell.center).angle()
        val directionRelativeAngle = originToCenterAngle - impulseDirection.angle()
        val impulseOriginDistance = oldCell.center.distance(impulseOrigin)
        val projectedDistance = sin(directionRelativeAngle) * impulseOriginDistance
        val translationImpactCoefficient = 1 / ((projectedDistance / oldCell.radius).pow(2) + 1)
        val rotationImpactCoefficient = 1 - translationImpactCoefficient
        cell.speed += impulseDirection * translationImpactCoefficient
        if (projectedDistance != 0.0)
            cell.angularSpeed -= atan(impulseDirection.length / projectedDistance) * rotationImpactCoefficient
    }
}