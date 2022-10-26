package com.devexperts.openhack2022.cell_sandbox.geom

import com.devexperts.openhack2022.cell_sandbox.game.WorldObject
import com.devexperts.openhack2022.cell_sandbox.geom.Axis.*

val defaultAreaNode = AreaNode(X, BoxBoundary(0, 0, 20, 20))
const val areaBoundaryIncreasing = 20

class WorldAreaTree(private var root: AreaNode = defaultAreaNode) {

    fun remove(obj: WorldObject) {
        var current: AreaNode? = root

        while (true) {

            if (current == null) {
                return
            }

            if (current.boundary.inCurrentArea(obj.boundary)) {
                current.objectsTree?.remove(obj)
                return
            }

            // try to find necessary area
            current = moveToChild(current, obj)
        }
    }

    fun add(obj: WorldObject) {
        if (root == defaultAreaNode) {
            root = createNewAreaFromObjDot(obj, X)
        } else {
            var current: AreaNode? = root
            var parent: AreaNode = root

            while (true) {
                // we don't found area for object, create new one and add object to it
                if (current == null) {
                    if (parent.axis == X) {
                        if (parent.boundary.max.x <= obj.boundary.max.x) {
                            parent.right = createNewAreaFromObjDot(obj, parent.axis.reverseCurrent())
                        } else {
                            parent.left = createNewAreaFromObjDot(obj, parent.axis.reverseCurrent())
                        }
                    }

                    if (parent.axis == Y) {
                        if (parent.boundary.max.y <= obj.boundary.max.y) {
                            parent.right = createNewAreaFromObjDot(obj, parent.axis.reverseCurrent())
                        } else {
                            parent.left = createNewAreaFromObjDot(obj, parent.axis.reverseCurrent())
                        }
                    }

                    return
                }

                // if we found area for object, add it to object tree
                if (current.boundary.inCurrentArea(obj.boundary)) {
                    current.objectsTree?.add(obj)
                    return
                }

                parent = current

                // try to find necessary area
                current = moveToChild(current, obj)
            }
        }
    }

    fun find(obj: WorldObject): WorldObject? {
        var current: AreaNode? = root

        while (true) {

            if (current == null) {
                return null
            }

            if (current.boundary.inCurrentArea(obj.boundary)) {
                return current.objectsTree?.find(obj)
            }

            // try to find necessary area
            current = moveToChild(current, obj)
        }
    }

    private fun moveToChild(
        current: AreaNode,
        obj: WorldObject
    ): AreaNode? {
        if (current.axis == X) {
            return if (current.boundary.max.x <= obj.boundary.max.x) {
                current.right
            } else {
                current.left
            }
        }

        if (current.axis == Y) {
            return if (current.boundary.max.y <= obj.boundary.max.y) {
                current.right
            } else {
                current.left
            }
        }
        return null
    }

    // for simplification use center of the object, but better to check to which area bigger part of object included
    private fun BoxBoundary.inCurrentArea(objBoundary: BoxBoundary): Boolean {
        return this.min.x <= objBoundary.center.x &&
                this.min.y <= objBoundary.center.y &&
                this.max.x >= objBoundary.center.x &&
                this.max.y >= objBoundary.center.y
    }

    private fun createNewAreaFromObjDot(obj: WorldObject, axis: Axis) = AreaNode(
        axis = axis,
        boundary = obj.boundary.copy(
            max = Vector2(
                obj.boundary.max.x + areaBoundaryIncreasing,
                obj.boundary.max.y + areaBoundaryIncreasing
            )
        ),
        objectsTree = WorldObjectTree(
            WorldObjectNode(
                axis = X,
                boundary = obj.boundary,
                obj = obj
            )
        )
    )
}

class WorldObjectTree(private var root: WorldObjectNode? = null) {

    fun remove(obj: WorldObject) {
        // TODO implement delete, I guess it should be exactly by dot
    }

    fun add(obj: WorldObject) {
        if (root == null) {
            root = createWorldObject(obj, X)
        } else {
            var current: WorldObjectNode? = root!!
            var parent: WorldObjectNode? = root!!

            while (true) {
                if (current == null) {
                    // we don't found area for object, create new one and add object to it
                    if (parent != null && parent.axis == X) {
                        if (parent.boundary.max.x <= obj.boundary.max.x) {
                            parent.right = createWorldObject(obj, parent.axis.reverseCurrent())
                        } else {
                            parent.left = createWorldObject(obj, parent.axis.reverseCurrent())
                        }
                    }

                    if (parent != null && parent.axis == Y) {
                        if (parent.boundary.max.y <= obj.boundary.max.y) {
                            parent.right = createWorldObject(obj, parent.axis.reverseCurrent())
                        } else {
                            parent.left = createWorldObject(obj, parent.axis.reverseCurrent())
                        }
                    }
                }

                parent = current

                // try to find necessary area
                current = moveToChild(current!!, obj)
            }
        }
    }

    private fun moveToChild(
        current: WorldObjectNode,
        obj: WorldObject
    ): WorldObjectNode? {
        if (current.axis == X) {
            return if (current.boundary.max.x <= obj.boundary.max.x) {
                current.right
            } else {
                current.left
            }
        }

        if (current.axis == Y) {
            return if (current.boundary.max.y <= obj.boundary.max.y) {
                current.right
            } else {
                current.left
            }
        }
        return null
    }

    private fun createWorldObject(obj: WorldObject, axis: Axis) = WorldObjectNode(
        axis = axis,
        boundary = obj.boundary,
        obj = obj
    )

    fun find(obj: WorldObject): WorldObject? {
        var current: WorldObjectNode? = root

        // TODO implement search, should it be exactly by Dot?
        return null
    }
}

data class WorldObjectNode(
    var axis: Axis,
    var boundary: BoxBoundary,
    var obj: WorldObject,
    var left: WorldObjectNode? = null,
    var right: WorldObjectNode? = null,
)

data class AreaNode(
    var axis: Axis,
    var boundary: BoxBoundary,
    var objectsTree: WorldObjectTree? = null,
    var left: AreaNode? = null,
    var right: AreaNode? = null,
    var final: Boolean = true,
    var splitLine: Double = 0.0,
)


enum class Axis {
    X,
    Y;

    fun reverseCurrent(): Axis = if (this == X) Y else X
}