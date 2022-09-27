package com.devexperts.openhack2022.cell_sandbox.gui

import com.devexperts.openhack2022.cell_sandbox.game.*
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints.*
import java.awt.event.*
import java.awt.geom.AffineTransform
import javax.swing.JPanel

class WorldView(
    var world: World,
    var camera: Camera
): JPanel(), MouseListener, MouseMotionListener, MouseWheelListener {
    companion object {
        val OUTER_AREA_COLOR = Color(75, 75, 75)
    }

    private var lastDragPoint = Point()

    init {
        addMouseListener(this)
        addMouseMotionListener(this)
        addMouseWheelListener(this)
    }

    override fun paint(graphics: Graphics) {
        val g = graphics as Graphics2D
        g.setRenderingHints(
            mapOf(
                KEY_ANTIALIASING to VALUE_ANTIALIAS_ON,
                KEY_TEXT_ANTIALIASING to VALUE_TEXT_ANTIALIAS_ON
            )
        )

        val scale = height/camera.height
        g.color = OUTER_AREA_COLOR
        g.fillRect(0, 0, width, height)
        g.transform = AffineTransform(
            scale, 0.0,
            0.0, scale,
            width/2 - scale*camera.center.x, height/2 - scale*camera.center.y
        )
        world.render(g)
    }

    override fun mouseClicked(e: MouseEvent) {}

    override fun mousePressed(e: MouseEvent) {
        lastDragPoint = e.point
    }

    override fun mouseDragged(e: MouseEvent) {
        val scale = height/camera.height
        camera.center += Vector2(
            (lastDragPoint.x - e.x)/scale,
            (lastDragPoint.y - e.y)/scale
        )
        lastDragPoint = e.point
        repaint()
    }

    override fun mouseWheelMoved(e: MouseWheelEvent) {
        if (e.preciseWheelRotation > 0)
            camera.height *= e.preciseWheelRotation + 1
        else
            camera.height *= -1/(e.preciseWheelRotation - 1)
        if (camera.height < 1.0)
            camera.height = 1.0
        repaint()
    }

    override fun mouseReleased(e: MouseEvent) {}
    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}
    override fun mouseMoved(e: MouseEvent) {}
}