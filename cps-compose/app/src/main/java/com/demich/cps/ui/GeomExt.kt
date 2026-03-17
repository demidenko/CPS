package com.demich.cps.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset

fun Float.projectX(
    from: Rect,
    to: Rect
): Float = (this - from.left) / from.width * to.width + to.left

fun Float.projectY(
    from: Rect,
    to: Rect
): Float = (this - from.top) / from.height * to.height + to.top

fun Offset.projectPoint(
    from: Rect,
    to: Rect
): Offset = Offset(
    x = x.projectX(from, to),
    y = y.projectY(from, to)
)

fun Offset.projectVector(
    from: Rect,
    to: Rect
): Offset = projectPoint(from, to) - Offset.Zero.projectPoint(from, to)

fun Rect.inflate(horizontal: Float, vertical: Float): Rect =
    Rect(
        left = left + horizontal,
        right = right - horizontal,
        top = top + vertical,
        bottom = bottom - vertical
    )

private fun Float.scale(scale: Float, center: Float) =
    (this - center) / scale + center

fun Rect.scale(scale: Float, center: Offset): Rect =
    Rect(
        left = left.scale(scale, center.x),
        right = right.scale(scale, center.x),
        top = top.scale(scale, center.y),
        bottom = bottom.scale(scale, center.y)
    )

@Stable
context(density: Density)
fun DpOffset.toOffset(): Offset =
    density.run {
        Offset(x = x.toPx(), y = y.toPx())
    }
