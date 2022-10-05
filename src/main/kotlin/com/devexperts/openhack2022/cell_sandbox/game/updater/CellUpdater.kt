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
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

class CellUpdater: Updater {

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
                        projection.to(oldCell.center).unit() * (oldCell.radius - projection.distance(oldCell.center))
                    val hardnessCoefficient = 0.8 + 0.2 * oldCell.genome.hardness
                    cell.speed += oppositeForce * hardnessCoefficient * delta
                }
            }

            for (other in oldArea.cells.values) {
                if (other == cell) continue
                val intersections = testCirclesIntersection(oldCell.center, oldCell.radius, other.center, other.radius)
                if (intersections.size == 2) {
                    val pivot =
                        if (oldCell.center.distance(other.center) > oldCell.radius)
                            (intersections.first() + intersections.last()) / 2
                        else
                            (oldCell.center + other.center) / 2

                    val massSum = oldCell.mass + other.mass
                    val thisMassCoefficient = oldCell.mass / massSum
                    val oppositeForce = pivot.to(oldCell.center).unit() * (oldCell.radius - pivot.distance(oldCell.center))
                    val hardnessCoefficient = 0.8 + 0.2 * oldCell.genome.hardness
                    cell.speed += (oppositeForce * hardnessCoefficient + (other.speed * thisMassCoefficient - oldCell.speed * (1 - thisMassCoefficient)) * oldCell.genome.hardness) * delta
                }
            }

            for (connection in oldCell.connections.values) {
                val partner = oldArea.cells[connection.partnerId]
                if (partner != null) {
                    val partnerConnection = partner.connections[cell.id]
                    if (partnerConnection != null) {
                        val effectiveConnectionAngle = oldCell.angle + connection.angle
                        val effectiveConnectionForceOrigin = oldCell.center + Vector2.unit(effectiveConnectionAngle) * oldCell.radius/2
                        val effectiveConnectionForceDestination = partner.center + Vector2.unit(partner.angle + partnerConnection.angle) * partner.radius/2
                        val connectionForceDirection = effectiveConnectionForceOrigin.to(effectiveConnectionForceDestination) * cell.center.distance(partner.center).pow(2) * delta
                        applyImpulse(cell, effectiveConnectionForceOrigin, connectionForceDirection)
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
                val splitNormal = cell.angle + cell.genome.splitAngle
                val child1Center = cell.center + Vector2.unit(splitNormal - Math.PI/2)
                val child2Center = cell.center + Vector2.unit(splitNormal + Math.PI/2)
                val child1Angle = splitNormal + cell.genome.child1Angle
                val child2Angle = splitNormal + cell.genome.child2Angle
                val child1Genome = cell.genome.children.first!!.deepCopy()
                val child2Genome = cell.genome.children.second!!.deepCopy()
                child1Genome.applyRadiation(world.area.radiation)
                child2Genome.applyRadiation(world.area.radiation)

                val child1ConnectionAngle = splitNormal - child1Angle + Math.PI / 2
                val child2ConnectionAngle = splitNormal - child2Angle - Math.PI / 2

                val child1 = cell.copy(
                    center = child1Center,
                    speed = cell.speed,
                    angle = child1Angle,
                    mass = cell.mass/2,
                    genome = child1Genome,
                )
                val child2 = cell.copy(
                    center = child2Center,
                    speed = cell.speed,
                    angle = child2Angle,
                    mass = cell.mass/2,
                    genome = child2Genome
                )

                child1.id = world.newId()
                child2.id = world.newId()

                child1.connections = mapOf(child2.id to CellConnectionState(child1ConnectionAngle, child2.id))
                child2.connections = mapOf(child1.id to CellConnectionState(child2ConnectionAngle, child1.id))

                world.remove(cell)
                world.add(child1)
                world.add(child2)
            } else {
                cell.center.x = cell.center.x.coerceIn(1.0..world.area.width - 1)
                cell.center.y = cell.center.y.coerceIn(1.0..world.area.height - 1)

                if (cell.center.isNaN())
                    throw IllegalStateException("Cell has NaN position!")
            }
        }
    }

    private fun applyImpulse(cell: CellState, impulseOrigin: Vector2, impulseDirection: Vector2) {
        if (impulseDirection.length == 0.0)
            return
        val originToCenterAngle = impulseOrigin.to(cell.center).angle()
        val directionRelativeAngle = originToCenterAngle - impulseDirection.angle()
        val impulseOriginDistance = cell.center.distance(impulseOrigin)
        val projectedDistance = sin(directionRelativeAngle) * impulseOriginDistance
        val translationImpactCoefficient = 1/((projectedDistance/cell.radius).pow(2) + 1)
        val rotationImpactCoefficient = 1 - translationImpactCoefficient
        cell.speed += impulseDirection * translationImpactCoefficient / cell.mass
        if (projectedDistance != 0.0)
            cell.angularSpeed -= atan(impulseDirection.length / projectedDistance) * rotationImpactCoefficient / cell.mass
    }

    private fun calculateLiveCost(cell: CellState): Double {
        return 0.002 * cell.mass
    }
}