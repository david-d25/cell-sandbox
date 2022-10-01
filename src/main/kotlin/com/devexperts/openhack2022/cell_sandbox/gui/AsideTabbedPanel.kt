package com.devexperts.openhack2022.cell_sandbox.gui

import java.awt.Color
import javax.swing.JTabbedPane

class AsideTabbedPanel: JTabbedPane() {
    init {
        background = Color(128, 128, 128)

//        ui = PaneUi()
    }

//    private class PaneUi: BasicTabbedPaneUI() {
//        override fun paintTabArea(g: Graphics?, tabPlacement: Int, selectedIndex: Int) {
//            super.paintTabArea(g, tabPlacement, selectedIndex)
//        }
//
//        override fun paintTabBackground(
//            g: Graphics?,
//            tabPlacement: Int,
//            tabIndex: Int,
//            x: Int,
//            y: Int,
//            w: Int,
//            h: Int,
//            isSelected: Boolean
//        ) {
//            super.paintTabBackground(g, tabPlacement, tabIndex, x, y, w, h, isSelected)
//        }
//
//        override fun paintText(
//            g: Graphics?,
//            tabPlacement: Int,
//            font: Font?,
//            metrics: FontMetrics?,
//            tabIndex: Int,
//            title: String?,
//            textRect: Rectangle?,
//            isSelected: Boolean
//        ) {
//            super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected)
//        }
//
//        override fun calculateTabWidth(tabPlacement: Int, tabIndex: Int, metrics: FontMetrics?): Int {
//            return super.calculateTabWidth(tabPlacement, tabIndex, metrics)
//        }
//
//        override fun paintFocusIndicator(
//            g: Graphics?,
//            tabPlacement: Int,
//            rects: Array<out Rectangle>?,
//            tabIndex: Int,
//            iconRect: Rectangle?,
//            textRect: Rectangle?,
//            isSelected: Boolean
//        ) {
//            super.paintFocusIndicator(g, tabPlacement, rects, tabIndex, iconRect, textRect, isSelected)
//        }
//    }
}