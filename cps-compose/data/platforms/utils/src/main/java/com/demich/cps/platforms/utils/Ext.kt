package com.demich.cps.platforms.utils

internal fun <T> Sequence<Result<T>>.values(): Sequence<T> =
    mapNotNull { it.getOrNull() }

internal fun <T> Iterable<Result<T>>.values(): List<T> =
    mapNotNull { it.getOrNull() }