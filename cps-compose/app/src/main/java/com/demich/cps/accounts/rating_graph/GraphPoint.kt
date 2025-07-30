package com.demich.cps.accounts.rating_graph

import com.demich.cps.accounts.managers.RatingChange

internal data class GraphPoint(val x: Long, val y: Long)

internal fun RatingChange.toGraphPoint() =
    GraphPoint(x = date.epochSeconds, y = rating.toLong())