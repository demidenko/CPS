package com.demich.cps.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

fun Offset.transform(
    from: Rect,
    to: Rect
): Offset = Offset(
    x = (x - from.left) / from.width * to.width + to.left,
    y = (y - from.top) / from.height * to.height + to.top
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

fun Rect.flipVertical(): Rect =
    copy(top = bottom, bottom = top)

fun Float.scale(scale: Float, center: Float) =
    (this - center) / scale + center

fun Rect.scale(scale: Float, center: Offset): Rect =
    Rect(
        left = left.scale(scale, center.x),
        right = right.scale(scale, center.x),
        top = top.scale(scale, center.y),
        bottom = bottom.scale(scale, center.y)
    )