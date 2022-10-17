package com.devexperts.openhack2022.cell_sandbox.geom

import com.devexperts.openhack2022.cell_sandbox.geom.Aabb

interface PhysicalBody {
    val aabb: Aabb
}