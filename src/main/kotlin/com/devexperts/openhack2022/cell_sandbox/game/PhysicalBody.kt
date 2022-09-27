package com.devexperts.openhack2022.cell_sandbox.game

import com.devexperts.openhack2022.cell_sandbox.geom.Aabb

interface PhysicalBody {
    val aabb: Aabb
}