package com.demich.cps.utils

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.measureTimedValue

@OptIn(ExperimentalContracts::class)
inline fun <R> printDuration(block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return measureTimedValue(block).also {
        println("${it.duration}: ${it.value}")
    }.value
}