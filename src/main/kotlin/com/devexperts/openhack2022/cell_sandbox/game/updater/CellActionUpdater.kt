package com.devexperts.openhack2022.cell_sandbox.game.updater

import com.devexperts.openhack2022.cell_sandbox.game.*
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2.Companion.unit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

class CellActionUpdater : Updater {
    override fun update(world: World, oldArea: AreaState, newArea: AreaState, delta: Double) {
        newArea.cells.values.forEach { cell ->
            cell.age += delta

            doTypeSpecificActions(cell, world, delta)

            if (cell.mass < world.settings.minCellMass) {
                processCellDeath(world, cell)
            } else if (cell.mass >= cell.genome.splitMass) {
                split(world, cell, newArea)
            } else {
                cell.center.x = cell.center.x.coerceIn(1.0..newArea.width - 1)
                cell.center.y = cell.center.y.coerceIn(1.0..newArea.height - 1)

                if (cell.center.isNaN())
                    throw IllegalStateException("Cell has NaN position!")
            }
        }
    }

    private fun doTypeSpecificActions(cell: CellState, world: World, delta: Double) {
        if (cell.genome.type == CellType.FLAGELLOCYTE) {
            val targetSpeed = unit(cell.angle) * cell.genome.flagellumForce
            val speedDiff = targetSpeed - cell.speed
            cell.applyImpulse(cell, cell.center, unit(cell.angle) * unit(cell.angle).dot(speedDiff) * delta)
            cell.mass -= speedDiff.length * world.settings.flagellocyteFlagellumForceCost * delta

            if (cell.connections.values.any { abs(it.angle - PI) < PI/12 })
                processCellDeath(world, cell) // Die if tail is blocked
        }

        // Other type-specific things here...
    }

    private fun processCellDeath(
        world: World,
        cell: CellState
    ) {
        world.add(FoodState(cell.center, sqrt(cell.mass)))
        world.remove(cell)
    }

    private fun split(world: World, cell: CellState, newArea: AreaState) {
        val splitNormal = cell.angle + cell.genome.splitAngle + PI/2
        val child1Center = cell.center + unit(splitNormal - Math.PI / 2)
        val child2Center = cell.center + unit(splitNormal + Math.PI / 2)
        val child1Angle = cell.angle + cell.genome.splitAngle + cell.genome.child1Angle
        val child2Angle = cell.angle + cell.genome.splitAngle + cell.genome.child2Angle
        val child1Genome = cell.genome.children.first.deepCopy()
        val child2Genome = cell.genome.children.second.deepCopy()
        child1Genome.applyRadiation(world, world.area.radiation)
        child2Genome.applyRadiation(world, world.area.radiation)

        val child1ConnectionAngle = (-cell.genome.child1Angle + 3 * PI) % (2 * PI)
        val child2ConnectionAngle = (-cell.genome.child2Angle + 2 * PI) % (2 * PI)

        val child1 = cell.copy(
            id = world.newId(),
            center = child1Center,
            speed = cell.speed,
            angle = child1Angle,
            mass = cell.mass / 2,
            genome = child1Genome,
            age = 0.0
        )
        val child2 = cell.copy(
            id = world.newId(),
            center = child2Center,
            speed = cell.speed,
            angle = child2Angle,
            mass = cell.mass / 2,
            genome = child2Genome,
            age = 0.0
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
        ).filterValues { it }.map { it.key }.forEach inner@{ child ->
            existingConnections.forEach { connection ->
                val thisIsFirstChild = child == child1
                val partner = newArea.cells[connection.partnerId]!!
                val partnerConnection = partner.connections[cell.id]!!

                var connectionAngleRelative = cell.genome.splitAngle - connection.angle + PI/2
                if (!thisIsFirstChild) connectionAngleRelative += Math.PI
                connectionAngleRelative %= (2 * Math.PI)

                val narrowRange = connectionAngleRelative in Math.PI / 15..Math.PI - Math.PI / 15
                val broadRange = connectionAngleRelative in -Math.PI / 15..Math.PI + Math.PI / 15
                val bothKeepConnections = cell.genome.child1KeepConnections && cell.genome.child2KeepConnections

                val childConnections = if (thisIsFirstChild) child1Connections else child2Connections
                val childGenomeAngle = if (thisIsFirstChild) cell.genome.child1Angle else cell.genome.child2Angle

                if (narrowRange || broadRange && !bothKeepConnections) { // Connect single
                    val partnerNewConnections = partner.connections
                        .plus(child.id to CellConnectionState(partnerConnection.angle, child.id))
                    childConnections[partner.id] = CellConnectionState(
                        (connection.angle - cell.genome.splitAngle - childGenomeAngle + 2 * Math.PI) % (2 * Math.PI),
                        partner.id
                    )
                    partner.connections = partnerNewConnections
                } else if (broadRange) { // Connect both with angle shift
                    var childShift = Math.PI / 6
                    if (!thisIsFirstChild)
                        childShift *= -1
                    if (abs(cell.genome.splitAngle - connection.angle + PI/2) > Math.PI / 15)
                        childShift *= -1

                    val partnerNewConnections = partner.connections
                        .plus(
                            child.id to CellConnectionState(
                                partnerConnection.angle + childShift,
                                child.id
                            )
                        )
                    childConnections[partner.id] = CellConnectionState(
                        (connection.angle - cell.genome.splitAngle - childGenomeAngle + childShift + 2 * Math.PI) % (2 * Math.PI),
                        partner.id
                    )
                    partner.connections = partnerNewConnections
                }
            }
        }

        child1.connections = child1Connections
        child2.connections = child2Connections

        world.remove(cell)
        world.add(child1)
        world.add(child2)
    }
}