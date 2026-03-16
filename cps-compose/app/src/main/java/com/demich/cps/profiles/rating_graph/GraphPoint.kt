package com.demich.cps.profiles.rating_graph

import com.demich.cps.profiles.managers.RatingChange
import kotlin.time.Instant

internal data class GraphPoint(
    val x: Instant,
    val y: Long
)

internal fun RatingChange.toGraphPoint() =
    GraphPoint(x = date, y = rating.toLong())