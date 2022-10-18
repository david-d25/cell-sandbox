package com.devexperts.openhack2022.cell_sandbox.game

import java.util.*

enum class CellType {
    //    PHOTOCYTE,
    PHAGOCYTE,
//    FLAGELLOCYTE
}

class Genome (
    var type: CellType,
    var cyanPigment: Double,
    var magentaPigment: Double,
    var yellowPigment: Double,
    var hardness: Double,
    var splitMass: Double,
    var splitAngle: Double,
    var child1Angle: Double,
    var child2Angle: Double,
    var stickOnSplit: Boolean,
    var child1KeepConnections: Boolean,
    var child2KeepConnections: Boolean
) {
    var children: Pair<Genome, Genome> = Pair(this, this)

    fun deepCopy() = copyRecursive()

    fun applyRadiation(world: World, radiation: Double) {
        applyRadiationRecursive(world, this, radiation, findAllGenomes(), setOf(this))
    }

    private fun copyRecursive(
        copies: MutableMap<Genome?, Genome?> = mutableMapOf(null to null)
    ): Genome {
        val result = Genome(
            type,
            cyanPigment,
            magentaPigment,
            yellowPigment,
            hardness,
            splitMass,
            splitAngle,
            child1Angle,
            child2Angle,
            stickOnSplit,
            child1KeepConnections,
            child2KeepConnections
        )
        copies[this] = result
        result.children = Pair(
            if (copies.containsKey(this.children.first))
                copies[this.children.first]!!
            else
                this.children.first.copyRecursive(copies),

            if (copies.containsKey(this.children.second))
                copies[this.children.second]!!
            else
                this.children.second.copyRecursive(copies)
        )
        return result
    }

    private fun findAllGenomes(): Set<Genome> {
        val stash = LinkedList(listOf(this))
        val result = mutableSetOf(this)
        while (stash.isNotEmpty()) {
            stash.pop().children.toList().forEach {
                if (!result.contains(it)) {
                    stash.add(it)
                    result.add(it)
                }
            }
        }
        return result
    }

    private fun applyRadiationRecursive(
        world: World,
        current: Genome,
        radiation: Double,
        allGenomes: Set<Genome>,
        visitedGenomes: Set<Genome>
    ) {
        if (radiation > Math.random()) {
            current.type = CellType.values().random()
            current.cyanPigment = current.cyanPigment.radiated(0.0, 1.0, radiation)
            current.magentaPigment = current.magentaPigment.radiated(0.0, 1.0, radiation)
            current.yellowPigment = current.yellowPigment.radiated(0.0, 1.0, radiation)
            current.hardness = current.hardness.radiated(0.0, 1.0, radiation)
            current.splitMass = current.splitMass.radiated(world.settings.minCellMass, 999999.0, radiation)
            current.splitAngle = current.splitAngle.radiated(0.0, 2*Math.PI, radiation)
            current.child1Angle = current.child1Angle.radiated(0.0, 2*Math.PI, radiation)
            current.child2Angle = current.child2Angle.radiated(0.0, 2*Math.PI, radiation)
            // TODO children
        }

        current.children.toList().forEach {
            if (!visitedGenomes.contains(it))
                applyRadiationRecursive(world, it, radiation, allGenomes, visitedGenomes + it)
        }
    }

    private fun Double.radiated(min: Double, max: Double, radiation: Double) =
        if (radiation > Math.random()) (this + radiation*(Math.random()*2 - 1)).coerceIn(min..max) else this
}