package com.devexperts.openhack2022.cell_sandbox.game

import java.util.*

enum class CellType {
    //    PHOTOCYTE,
    PHAGOCYTE,
    FLAGELLOCYTE
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
    var child2KeepConnections: Boolean,
    var nutritionPriority: Double = 0.5,

    // For flagellocyte
    var flagellumForce: Double = 8.0
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
            child2KeepConnections,
            nutritionPriority,
            flagellumForce
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
        current.apply {
            type = if (radiation > Math.random()) CellType.values().random() else type
            cyanPigment = cyanPigment.radiated(0.0, 1.0, radiation)
            magentaPigment = magentaPigment.radiated(0.0, 1.0, radiation)
            yellowPigment = yellowPigment.radiated(0.0, 1.0, radiation)
            hardness = hardness.radiated(0.0, 1.0, radiation)
            splitMass = splitMass.radiated(world.settings.minCellMass, 999999.0, radiation)
            splitAngle = splitAngle.radiated(0.0, 2*Math.PI, radiation)
            child1Angle = child1Angle.radiated(0.0, 2*Math.PI, radiation)
            child2Angle = child2Angle.radiated(0.0, 2*Math.PI, radiation)
            stickOnSplit = stickOnSplit.radiated(radiation)
            child1KeepConnections = child1KeepConnections.radiated(radiation)
            child2KeepConnections = child2KeepConnections.radiated(radiation)
            nutritionPriority = nutritionPriority.radiated(0.0, 1.0, radiation)
            flagellumForce = flagellumForce.radiated(
                world.settings.flagellocyteFlagellumMinForce,
                world.settings.flagellocyteFlagellumMaxForce,
                radiation
            )
        }
        // TODO children

        current.children.toList().forEach {
            if (!visitedGenomes.contains(it))
                applyRadiationRecursive(world, it, radiation, allGenomes, visitedGenomes + it)
        }
    }

    private fun Boolean.radiated(radiation: Double) = if (radiation > Math.random()) !this else this

    private fun Double.radiated(min: Double, max: Double, radiation: Double) =
        if (radiation > Math.random()) (this + radiation*(Math.random()*2 - 1)).coerceIn(min..max) else this
}