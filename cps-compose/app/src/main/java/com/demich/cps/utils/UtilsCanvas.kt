package com.demich.cps.utils

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill

fun DrawScope.drawRoundRectWithBorderInside(
    color: Color,
    borderColor: Color,
    borderWidth: Float,
    topLeft: Offset = Offset.Zero,
    size: Size = this.size,
    cornerRadius: CornerRadius
) {
    // border
    drawRoundRect(
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
        color = borderColor,
        style = Fill
    )

    drawRoundRect(
        topLeft = topLeft.run { Offset(x = x + borderWidth, y = y + borderWidth) },
        size = size.run { Size(width = width - borderWidth *  2, height = height - borderWidth * 2) },
        cornerRadius = cornerRadius,
        color = color,
        style = Fill
    )
}