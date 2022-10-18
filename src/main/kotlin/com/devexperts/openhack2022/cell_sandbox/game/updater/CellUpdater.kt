package com.devexperts.openhack2022.cell_sandbox.game.updater

import com.devexperts.openhack2022.cell_sandbox.game.World
import com.devexperts.openhack2022.cell_sandbox.game.state.AreaState
import com.devexperts.openhack2022.cell_sandbox.game.state.CellConnectionState
import com.devexperts.openhack2022.cell_sandbox.game.state.CellState
import com.devexperts.openhack2022.cell_sandbox.game.state.FoodState
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import com.devexperts.openhack2022.cell_sandbox.geom.projectPointOnLine
import com.devexperts.openhack2022.cell_sandbox.geom.testCirclesIntersection
import com.devexperts.openhack2022.cell_sandbox.geom.testLineAndCircleIntersection
import java.lang.Math.PI
import kotlin.math.*

class CellUpdater: Updater {

    companion object {
        const val CELL_STICKINESS_DEPTH = 3
    }

    override fun update(world: World, oldArea: AreaState, newArea: AreaState, delta: Double) {
        newArea.cells.values.forEach { cell ->
            val oldCell = if (oldArea.cells.contains(cell.id)) oldArea.cells[cell.id]!! else cell

            cell.connections = cell.connections.filterValues { !oldArea.cells.contains(it.partnerId) }

            cell.speed += world.area.gravity * delta

            for (border in oldArea.borders.values) {
                val intersections = testLineAndCircleIntersection(oldCell.center, oldCell.radius, border.a, border.b)
                if (intersections.isNotEmpty()) {
                    val projection = projectPointOnLine(oldCell.center, Pair(border.a, border.b))
                    val oppositeForce =
                        (projection to oldCell.center).unit() * (oldCell.radius - projection.distance(oldCell.center))
                    val depth = (1 - oldCell.center.distance(projection)/oldCell.radius).coerceIn(0.0, 1.0)
                    val hardnessCoefficient = (oldCell.genome.hardness*depth + 1).pow(oldCell.genome.hardness + 1)
                    cell.speed += oppositeForce * hardnessCoefficient * delta
                }
            }

            for (other in oldArea.cells.values) {
                if (other.id == cell.id) continue
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

                    val depth = (1 - oldCell.center.distance(pivot)/oldCell.radius).coerceIn(0.0, 1.0)

                    val massSum = oldCell.mass + other.mass
                    val thisMassCoefficient = oldCell.mass / massSum
                    val oppositeForce = (pivot to oldCell.center).unitSafe() * (oldCell.radius - pivot.distance(oldCell.center))
                    val hardnessCoefficient = (oldCell.genome.hardness*depth + 1).pow(oldCell.genome.hardness + 1)
                    cell.speed += (oppositeForce * hardnessCoefficient + (other.speed * thisMassCoefficient - oldCell.speed * (1 - thisMassCoefficient)) * oldCell.genome.hardness) * delta
                }
            }

            for (connection in oldCell.connections.values) {
                val partner = oldArea.cells[connection.partnerId]
                if (partner != null) {
                    val partnerConnection = partner.connections[cell.id]
                    if (partnerConnection != null) {
                        repeat(4) { stringId ->
                            val angleOffset = stringId* PI /24 - PI /12
                            val effectiveConnectionAngle = oldCell.angle + connection.angle + angleOffset
                            val effectiveConnectionForceOrigin = oldCell.center + Vector2.unit(effectiveConnectionAngle) * oldCell.radius - Vector2.unit(effectiveConnectionAngle) * CELL_STICKINESS_DEPTH
                            val effectiveConnectionForceDestination = partner.center + Vector2.unit(partner.angle + partnerConnection.angle - angleOffset) * partner.radius - Vector2.unit(partner.angle + partnerConnection.angle - angleOffset) * CELL_STICKINESS_DEPTH
                            val connectionForceDirection = (effectiveConnectionForceOrigin to effectiveConnectionForceDestination) / 4 * delta
                            applyImpulse(oldCell, cell, effectiveConnectionForceOrigin, connectionForceDirection)
                        }
                    }
                }
            }

            cell.center += cell.speed * delta
            cell.speed -= cell.speed * world.area.viscosity * delta

            cell.angle += cell.angularSpeed * delta
            cell.angularSpeed -= cell.angularSpeed * world.area.viscosity * delta

            oldArea.food.values.forEach {
                if (oldCell.center.distance(it.center) < oldCell.radius + it.radius) {
                    val massToEat = min(it.mass, world.settings.maxFoodAbsorbingSpeed * delta)
                    cell.mass += massToEat
                }
            }

            cell.mass -= calculateLiveCost(cell) * delta
            if (cell.mass < world.settings.minCellMass) {
                world.add(FoodState(cell.center, sqrt(cell.mass)))
                world.remove(cell)
            } else if (cell.mass >= cell.genome.splitMass) {
                split(world, cell, newArea)
            } else {
                cell.center.x = cell.center.x.coerceIn(1.0..world.area.width - 1)
                cell.center.y = cell.center.y.coerceIn(1.0..world.area.height - 1)

                if (cell.center.isNaN())
                    throw IllegalStateException("Cell has NaN position!")
            }
        }
    }

    private fun split(world: World, cell: CellState, newArea: AreaState) {
        val splitNormal = cell.angle + cell.genome.splitAngle
        val child1Center = cell.center + Vector2.unit(splitNormal - PI /2)
        val child2Center = cell.center + Vector2.unit(splitNormal + PI /2)
        val child1Angle = splitNormal + cell.genome.child1Angle
        val child2Angle = splitNormal + cell.genome.child2Angle
        val child1Genome = cell.genome.children.first!!.deepCopy()
        val child2Genome = cell.genome.children.second!!.deepCopy()
        child1Genome.applyRadiation(world, world.area.radiation)
        child2Genome.applyRadiation(world, world.area.radiation)

        val child1ConnectionAngle = -cell.genome.child1Angle + PI / 2
        val child2ConnectionAngle = -cell.genome.child2Angle - PI / 2

        val child1 = cell.copy(
            id = world.newId(),
            center = child1Center,
            speed = cell.speed,
            angle = child1Angle,
            mass = cell.mass/2,
            genome = child1Genome,
        )
        val child2 = cell.copy(
            id = world.newId(),
            center = child2Center,
            speed = cell.speed,
            angle = child2Angle,
            mass = cell.mass/2,
            genome = child2Genome
        )

        val existingConnections = cell.connections.values
        val child1Connections = mutableMapOf<Long, CellConnectionState>()
        val child2Connections = mutableMapOf<Long, CellConnectionState>()

        if (cell.genome.stickOnSplit) {
            child1Connections[child2.id] = CellConnectionState(child1ConnectionAngle, child2.id)
            child2Connections[child1.id] = CellConnectionState(child2ConnectionAngle, child1.id)
        }

        mutableMapOf(
            child1 to cell.genome.child1KeepConnections,
            child2 to cell.genome.child2KeepConnections
        ).filterValues { it }.map { it.key }.forEach inner@ { child ->
            existingConnections.forEach { connection ->
                val thisIsFirstChild = child == child1
                val partner = newArea.cells[connection.partnerId]!!
                val partnerConnection = partner.connections[cell.id]!!

                var connectionAngleRelative = cell.genome.splitAngle - connection.angle
                if (!thisIsFirstChild) connectionAngleRelative += PI
                connectionAngleRelative %= (2 * PI)

                val narrowRange = connectionAngleRelative in PI/15 .. PI-PI/15
                val broadRange = connectionAngleRelative in -PI/15 .. PI+PI/15
                val bothKeepConnections = cell.genome.child1KeepConnections && cell.genome.child2KeepConnections

                val childConnections = if (thisIsFirstChild) child1Connections else child2Connections
                val childGenomeAngle = if (thisIsFirstChild) cell.genome.child1Angle else cell.genome.child2Angle

                if (narrowRange || broadRange && !bothKeepConnections) { // Connect single
                    val partnerNewConnections = partner.connections
                        .plus(child.id to CellConnectionState(partnerConnection.angle, child.id))
                    childConnections[partner.id] = CellConnectionState(
                        (connection.angle - cell.genome.splitAngle - childGenomeAngle + 2*PI) % (2*PI),
                        partner.id
                    )
                    partner.connections = partnerNewConnections
                } else if (broadRange) { // Connect both with angle shift
                    var childShift = PI/6
                    if (!thisIsFirstChild)
                        childShift *= -1
                    if (abs(cell.genome.splitAngle - connection.angle) > PI/15)
                        childShift *= -1

                    val partnerNewConnections = partner.connections
                        .plus(child.id to CellConnectionState(
                            partnerConnection.angle + childShift,
                            child.id
                        ))
                    childConnections[partner.id] = CellConnectionState(
                        (connection.angle - cell.genome.splitAngle - childGenomeAngle + childShift + 2*PI) % (2*PI),
                        partner.id
                    )
                    partner.connections = partnerNewConnections
                }
            }
        }

        existingConnections.forEach { connection ->
            val partner = newArea.cells[connection.partnerId]!!
            partner.connections = partner.connections.filterKeys { it != cell.id }
        }

        child1.connections = child1Connections
        child2.connections = child2Connections

        world.remove(cell)
        world.add(child1)
        world.add(child2)
    }

    private fun applyImpulse(oldCell: CellState, cell: CellState, impulseOrigin: Vector2, impulseDirection: Vector2) {
        if (impulseDirection.length == 0.0)
            return
        val originToCenterAngle = (impulseOrigin to oldCell.center).angle()
        val directionRelativeAngle = originToCenterAngle - impulseDirection.angle()
        val impulseOriginDistance = oldCell.center.distance(impulseOrigin)
        val projectedDistance = sin(directionRelativeAngle) * impulseOriginDistance
        val translationImpactCoefficient = 1/((projectedDistance/oldCell.radius).pow(2) + 1)
        val rotationImpactCoefficient = 1 - translationImpactCoefficient
        cell.speed += impulseDirection * translationImpactCoefficient
        if (projectedDistance != 0.0)
            cell.angularSpeed -= atan(impulseDirection.length / projectedDistance) * rotationImpactCoefficient
    }

    private fun calculateLiveCost(cell: CellState): Double {
        return 0.005 * cell.mass
    }
}