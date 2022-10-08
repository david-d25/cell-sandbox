package com.devexperts.openhack2022.cell_sandbox.gui

import java.awt.*
import java.awt.event.*
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.lang.Exception
import java.util.*
import javax.swing.JComponent
import javax.swing.JTextField
import kotlin.math.abs
import kotlin.math.round

class SlidingNumberSelector: JComponent(), MouseMotionListener, MouseListener, FocusListener, KeyListener {
    var softMin = Double.NEGATIVE_INFINITY
    var softMax = Double.POSITIVE_INFINITY
    var hardMin = Double.NEGATIVE_INFINITY
    var hardMax = Double.POSITIVE_INFINITY

    private var hover = false
    private var dragging = false
    private var editing = false

    var value = 0.0
        set(value) {
            val old = value
            field = value
            onValueChange(old, value)
        }

    var step = 0.1

    private var lastMousePressX = 0.0
    private var textInput = JTextField()

    private var valueChangeListeners = LinkedList<ValueChangeListener>()

    init {
        minimumSize = Dimension(100, 25)
        preferredSize = Dimension(100, 25)

        font = Font("Arial", Font.PLAIN, 18)

        layout = BorderLayout()
        add(textInput, BorderLayout.CENTER)

        textInput.horizontalAlignment = JTextField.CENTER

        textInput.font = font
        textInput.selectionColor = COLOR_SELECTION
        textInput.isOpaque = false
        textInput.border = null
        textInput.isVisible = false

        addMouseMotionListener(this)
        addMouseListener(this)
        addKeyListener(this)
        textInput.addKeyListener(this)
    }

    fun addValueChangeListener(listener: ValueChangeListener) {
        valueChangeListeners.add(listener)
    }

    override fun paint(g: Graphics) {
        g as Graphics2D
        g.applyQualityRenderingHints()

        g.stroke = BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER)
        RoundRectangle2D.Double(
            0.5, 0.5, width.toDouble() - 1f, height.toDouble() - 1f, 25.0, 25.0
        ).also {
            g.color = if (dragging) COLOR_BACKGROUND_DRAGGING else if (hover) COLOR_BACKGROUND_HOVER else COLOR_BACKGROUND
            g.fill(it)
            g.color = COLOR_STROKE
            g.draw(it)
        }

        if (!editing) {
            g.color = COLOR_TEXT
            g.font = font
            val metrics = g.fontMetrics
            val bounds = metrics.getStringBounds(getText(), g)
            val x = width/2f - bounds.width.toFloat()/2f
            val y = height/2f + bounds.height.toFloat()/4f
            g.drawString(getText(), x, y)

            g.color = COLOR_ARROWS
            g.stroke = BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER)
            val leftArrowPath = Path2D.Double()
            leftArrowPath.moveTo(ARROWS_PADDING + ARROWS_SIZE, height/2.0 - ARROWS_SIZE)
            leftArrowPath.lineTo(ARROWS_PADDING, height/2.0)
            leftArrowPath.lineTo(ARROWS_PADDING + ARROWS_SIZE, height/2.0 + ARROWS_SIZE)
            g.draw(leftArrowPath)

            val rightArrowPath = Path2D.Double()
            rightArrowPath.moveTo(width - (ARROWS_PADDING + ARROWS_SIZE), height/2.0 - ARROWS_SIZE)
            rightArrowPath.lineTo(width - ARROWS_PADDING, height/2.0)
            rightArrowPath.lineTo(width - (ARROWS_PADDING + ARROWS_SIZE), height/2.0 + ARROWS_SIZE)
            g.draw(rightArrowPath)
        }

        paintChildren(g)
    }

    override fun mouseDragged(e: MouseEvent) {
        if (abs(e.x - lastMousePressX) > step / MOUSE_SENSITIVITY) {
            val diff = round((e.x - lastMousePressX) * MOUSE_SENSITIVITY / step) * step
            lastMousePressX = e.x.toDouble()
            value += diff
            if (diff > 0 && value > softMax) value = softMax
            if (diff < 0 && value < softMin) value = softMin
        }
    }

    override fun mouseClicked(e: MouseEvent) {
        if (e.x < 25) {
            value -= step
            if (value < softMin) value = softMin
        } else if (e.x > width - 25) {
            value += step
            if (value > softMax) value = softMax
        } else
            startEditing()
    }

    override fun mouseEntered(e: MouseEvent) {
        hover = true
        repaint()
    }

    override fun mouseExited(e: MouseEvent) {
        hover = false
        repaint()
    }

    override fun mouseMoved(e: MouseEvent) {
        cursor = Cursor(Cursor.E_RESIZE_CURSOR)
        if (e.x < 25 || e.x > width - 25)
            cursor = Cursor(Cursor.HAND_CURSOR)
    }

    override fun mousePressed(e: MouseEvent) {
        dragging = true
        lastMousePressX = e.x.toDouble()
        repaint()
    }

    override fun mouseReleased(e: MouseEvent) {
        dragging = false
        repaint()
    }

    override fun keyPressed(e: KeyEvent) {
        if (editing) {
            if (e.keyCode == KeyEvent.VK_ESCAPE)
                dropEditing()
            if (e.keyCode == KeyEvent.VK_ENTER)
                applyEditing()
        }
    }

    override fun focusGained(e: FocusEvent?) {
        startEditing()
    }

    override fun focusLost(e: FocusEvent?) {
        dropEditing()
    }

    private fun startEditing() {
        editing = true
        textInput.text = getText()
        textInput.isVisible = true
        textInput.grabFocus()
        textInput.selectAll()
        repaint()
    }

    private fun applyEditing() {
        try {
            value = textInput.text.toDouble().coerceIn(hardMin, hardMax)
        } catch (ignored: Exception) {}
        editing = false
        textInput.isVisible = false
        repaint()
    }

    private fun dropEditing() {
        editing = false
        textInput.isVisible = false
        repaint()
    }

    private fun getText(): String {
        if (value == value.toLong().toDouble())
            return String.format(Locale.US, "%d", value.toLong())
        return String.format(Locale.US, "%.6f", value).replace(Regex("(\\.\\d+?)0*$"), "$1")
    }

    private fun onValueChange(oldValue: Double, newValue: Double) {
        valueChangeListeners.forEach { it.onValueChange(oldValue, newValue) }
        repaint()
    }

    override fun keyTyped(e: KeyEvent) {}
    override fun keyReleased(e: KeyEvent) {}

    companion object {
        val COLOR_BACKGROUND = Color(192, 192, 192)
        val COLOR_BACKGROUND_HOVER = Color(204, 204, 204)
        val COLOR_BACKGROUND_DRAGGING = Color(160, 160, 160)
        val COLOR_STROKE = Color(150, 150, 150)
        val COLOR_TEXT = Color(42, 42, 42)
        val COLOR_ARROWS = Color(72, 72, 72)
        val COLOR_SELECTION = Color(96, 96, 96)

        const val MOUSE_SENSITIVITY = 0.01

        const val ARROWS_SIZE = 5.0
        const val ARROWS_PADDING = 8.0
    }

    fun interface ValueChangeListener {
        fun onValueChange(oldValue: Double, newValue: Double)
    }
}