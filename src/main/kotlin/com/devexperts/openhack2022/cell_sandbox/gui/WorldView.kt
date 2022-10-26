package com.devexperts.openhack2022.cell_sandbox.gui

import com.devexperts.openhack2022.cell_sandbox.game.Camera
import com.devexperts.openhack2022.cell_sandbox.game.World
import com.devexperts.openhack2022.cell_sandbox.geom.Vector2
import javafx.animation.AnimationTimer
import javafx.event.EventHandler
import javafx.geometry.VPos
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.input.ZoomEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color

class WorldView(var world: World, var camera: Camera): StackPane() {
    companion object {
        private const val SCROLL_SENSITIVITY = 0.03
        private val OUTER_AREA_COLOR: Color = Color.rgb(75, 75, 75)
    }

    private var lastDragPoint = Vector2(0, 0)

    private var lastFpsUpdate = System.currentTimeMillis()
    private var framesCounter = 0L
    private var lastFpsValue = 0L

    private val timer = object : AnimationTimer() {
        override fun handle(now: Long) {
            draw()
        }
    }

    private val canvas = Canvas()

    init {
        setMinSize(0.0, 0.0)

        widthProperty().addListener { _, _, v -> camera.viewportWidth = v.toDouble() }
        heightProperty().addListener { _, _, v -> camera.viewportHeight = v.toDouble() }

        camera.viewportWidth = width
        camera.viewportHeight = height

        canvas.widthProperty().bind(widthProperty())
        canvas.heightProperty().bind(heightProperty())
        children += canvas

        onMousePressed = EventHandler { mousePressed(it) }
        onMouseDragged = EventHandler { mouseDragged(it) }
        onScroll = EventHandler { scrolled(it) }
        onZoom = EventHandler { zoomed(it) }
        timer.start()
    }

    private fun draw() {
        val context = canvas.graphicsContext2D
        context.save()

        context.fill = OUTER_AREA_COLOR
        context.clearRect(0.0, 0.0, width, height)
        context.transform(camera.transform)

        world.render(context)

        context.restore()

        tryLookAtSelectedCell()

        tickFpsCounter(context)
    }

    private fun tryLookAtSelectedCell() {
        val cell = world.area.cells[world.selectedCellIdProperty.value]
        if (cell != null)
            camera.center += (cell.center - camera.center)/10
    }

    private fun tickFpsCounter(context: GraphicsContext) {
        context.save()
        if (System.currentTimeMillis() - lastFpsUpdate > world.settings.fpsUpdateIntervalMs) {
            lastFpsValue = 1000*framesCounter/(System.currentTimeMillis() - lastFpsUpdate)
            lastFpsUpdate = System.currentTimeMillis()
            framesCounter = 0
        }

        if (world.settings.showFps) {
            context.stroke = Color.WHITE
            context.fill = Color.GREEN
            context.textBaseline = VPos.TOP
            context.strokeText("$lastFpsValue FPS", 0.0, 0.0)
            context.fillText("$lastFpsValue FPS", 0.0, 0.0)
        }

        framesCounter++
        context.restore()
    }


    private fun mousePressed(event: MouseEvent) {
        lastDragPoint = Vector2(event.x, event.y)
        draw()
    }

    private fun mouseDragged(event: MouseEvent) {
        val scale = height/camera.height
        camera.center += Vector2(
            (lastDragPoint.x - event.x)/scale,
            (lastDragPoint.y - event.y)/scale
        )
        lastDragPoint = Vector2(event.x, event.y)
        draw()
    }

    private fun scrolled(event: ScrollEvent) {
        if (event.deltaY < 0)
            camera.height *= 1 - event.deltaY*SCROLL_SENSITIVITY
        else
            camera.height *= 1/(event.deltaY*SCROLL_SENSITIVITY + 1)
        if (camera.height < 1.0)
            camera.height = 1.0
        draw()
    }


    private fun zoomed(event: ZoomEvent) {
        camera.height /= event.zoomFactor
        if (camera.height < 1.0)
            camera.height = 1.0
        draw()
    }
}