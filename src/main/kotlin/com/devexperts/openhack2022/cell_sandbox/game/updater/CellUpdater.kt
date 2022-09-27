package com.devexperts.openhack2022.cell_sandbox.game.updater

import com.devexperts.openhack2022.cell_sandbox.game.World
import com.devexperts.openhack2022.cell_sandbox.game.state.CellState
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import com.devexperts.openhack2022.cell_sandbox.geom.projectPointOnLine
import com.devexperts.openhack2022.cell_sandbox.geom.testCirclesIntersection
import com.devexperts.openhack2022.cell_sandbox.geom.testLineAndCircleIntersection
import java.lang.IllegalStateException
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

class CellUpdater: Updater<CellState> {
    override fun update(target: CellState, world: World, delta: Double): Set<CellState> {
        with(target) {
            var newSpeed = target.speed + world.area.gravity/1000 * delta
            var newCenter = target.center.copy()
            var newMass = target.mass

            for (border in world.area.borders) {
                val intersections = testLineAndCircleIntersection(center, radius, border.a, border.b)
                if (intersections.isNotEmpty()) {
                    val projection = projectPointOnLine(center, Pair(border.a, border.b))
                    val depth = abs(1 - projection.to(center).length/radius)
                    val oppositeForce = projection.to(center)
                    val hardnessCoefficient = (-1/(depth - 1) - 1).pow(1/genome.hardness)
                    newSpeed += oppositeForce * hardnessCoefficient * delta
                }
            }

            for (other in world.area.cells) {
                if (other == this) continue
                val intersections = testCirclesIntersection(newCenter, radius, other.center, other.radius)
                if (intersections.size == 2) {
                    val pivot =
                        if (newCenter.distance(other.center) > radius)
                            (intersections.first() + intersections.last())/2
                        else
                            (newCenter + other.center)/2

                    val depth = abs(1 - other.center.distance(pivot)/other.radius)
                    val oppositeForce = pivot.to(newCenter)
                    val hardnessCoefficient = (-1/(depth - 1) - 1).pow(1/genome.hardness)
                    newSpeed += oppositeForce * hardnessCoefficient * delta

                    val distance = newCenter.distance(other.center)
                    val masses = mass + other.mass
                    val positionCorrector = other.center.to(newCenter).unit() * (radius + other.radius - distance)
                    newCenter += positionCorrector * mass/masses * genome.hardness * delta
                }
            }

            newCenter += newSpeed * delta

            val viscosityForce = -newSpeed * world.area.viscosity
            newSpeed += viscosityForce * delta

            world.area.food.forEach {
                if (center.distance(it.center) < radius + it.radius) {
                    val massToEat = min(it.mass, world.settings.maxFoodAbsorbingSpeed * delta)
                    newMass += massToEat
                }
            }

            newMass -= calculateLiveCost(target) * delta
            if (mass < world.settings.minCellMass)
                return emptySet() // TODO leave food
            else if (mass > genome.splitMass) {
                val child1 = target.copy(
                    center = newCenter + Vector2(1, 0).rotate(angle + genome.splitAngle - Math.PI/2),
                    speed = newSpeed,
                    angle = angle + genome.splitAngle + genome.child1Angle,
                    mass = newMass/2,
                    genome = genome.children.first!!.copy().also { it.applyRadiation(world.area.radiation) }
                )
                val child2 = target.copy(
                    center = newCenter + Vector2(1, 0).rotate(angle + genome.splitAngle + Math.PI/2),
                    speed = newSpeed,
                    angle = angle - genome.splitAngle + genome.child2Angle,
                    mass = newMass/2,
                    genome = genome.children.second!!.copy().also { it.applyRadiation(world.area.radiation) }
                )

                return setOf(child1, child2)
            } else {
                newCenter.x = newCenter.x.coerceIn(1.0..world.area.width - 1)
                newCenter.y = newCenter.y.coerceIn(1.0..world.area.height - 1)

                if (newCenter.isNaN())
                    throw IllegalStateException("Cell has NaN position!")

                return setOf(target.copy(
                    center = newCenter,
                    speed = newSpeed,
                    mass = newMass
                ))
            }
        }
    }

    private fun calculateLiveCost(cell: CellState): Double {
        return 0.001 * cell.mass
    }
}