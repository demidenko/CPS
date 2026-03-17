package com.demich.cps.ui.geom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

class RectProjector(
    private val from: Rect,
    private val to: Rect
) {
    fun projectX(x: Float): Float =
        x.projectX(from = from, to = to)

    fun projectY(y: Float): Float =
        y.projectY(from = from, to = to)
}

context(projector: RectProjector)
fun Offset.projectPoint(): Offset =
    Offset(
        x = projector.projectX(x),
        y = projector.projectY(y)
    )

context(projector: RectProjector)
fun Offset.projectVector(): Offset =
    projectPoint() - Offset.Zero.projectPoint()