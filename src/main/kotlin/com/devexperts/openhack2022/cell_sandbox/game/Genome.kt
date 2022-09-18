package com.devexperts.openhack2022.cell_sandbox.game

class Genome (
    val type: CellType,
    val cyanPigment: Double,
    val magentaPigment: Double,
    val yellowPigment: Double,
    val hardness: Double,
    val splitMass: Double,
    val splitAngle: Double,
    val child1Angle: Double,
    val child2Angle: Double,
    var child1Genome: Genome?,
    var child2Genome: Genome?
)