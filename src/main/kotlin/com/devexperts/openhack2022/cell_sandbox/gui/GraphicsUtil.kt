package com.devexperts.openhack2022.cell_sandbox.gui

import java.awt.Graphics2D
import java.awt.RenderingHints.*

fun Graphics2D.applyQualityRenderingHints() {
    setRenderingHint(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY)
    setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
    setRenderingHint(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_QUALITY)
    setRenderingHint(KEY_DITHERING, VALUE_DITHER_ENABLE)
    setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON)
    setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR)
    setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY)
    setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE)
}