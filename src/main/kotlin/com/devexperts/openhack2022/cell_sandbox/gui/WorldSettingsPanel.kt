package com.devexperts.openhack2022.cell_sandbox.gui

import com.devexperts.openhack2022.cell_sandbox.game.WorldSettings
import java.awt.*
import java.awt.GridBagConstraints.HORIZONTAL
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel

class WorldSettingsPanel(private val settings: WorldSettings): JPanel() {
    init {
        background = Color(128, 128, 128)

        val defaultBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        add(JPanel().also {
            it.background = null
            it.add(Label("Gravity X:"))
            it.add(NumberSelector().also { s ->
                s.value = settings.gravity.x
                s.addValueChangeListener { _, v -> settings.gravity.x = v }
            })
            it.border = defaultBorder
        })

        add(JPanel().also {
            it.background = null
            it.add(Label("Gravity Y:"))
            it.add(NumberSelector().also { s ->
                s.value = settings.gravity.y
                s.addValueChangeListener { _, v -> settings.gravity.y = v }
            })
            it.border = defaultBorder
        })

        add(JPanel().also {
            it.background = null
            it.add(Label("Viscosity:"))
            it.add(NumberSelector().also { s ->
                s.value = settings.viscosity
                s.addValueChangeListener { _, v -> settings.viscosity = v }
            })
            it.border = defaultBorder
        })

        add(JPanel().also {
            it.background = null
            it.add(Label("Radiation:"))
            it.add(NumberSelector().also { s ->
                s.value = settings.radiation
                s.addValueChangeListener { _, v -> settings.radiation = v }
            })
            it.border = defaultBorder
        })

        add(Box.createVerticalGlue())
    }
}