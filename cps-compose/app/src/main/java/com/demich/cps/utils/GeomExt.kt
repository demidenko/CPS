package com.demich.cps.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

fun Float.transformX(
    from: Rect,
    to: Rect
): Float = (this - from.left) / from.width * to.width + to.left

fun Float.transformY(
    from: Rect,
    to: Rect
): Float = (this - from.top) / from.height * to.height + to.top

fun Offset.transform(
    from: Rect,
    to: Rect
): Offset = Offset(
    x = x.transformX(from, to),
    y = y.transformY(from, to)
)

fun Offset.transformVector(
    from: Rect,
    to: Rect
): Offset = transform(from, to) - Offset.Zero.transform(from, to)

fun Rect.inflate(horizontal: Float, vertical: Float): Rect =
    Rect(
        left = left + horizontal,
        right = right - horizontal,
        top = top + vertical,
        bottom = bottom - vertical
    )

fun Float.scale(scale: Float, center: Float) =
    (this - center) / scale + center

fun Rect.scale(scale: Float, center: Offset): Rect =
    Rect(
        left = left.scale(scale, center.x),
        right = right.scale(scale, center.x),
        top = top.scale(scale, center.y),
        bottom = bottom.scale(scale, center.y)
    )