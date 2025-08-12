package com.demich.cps.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demich.datastore_itemized.DataStoreValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

fun <T> Flow<T>.firstBlocking(): T =
    when (this) {
        is StateFlow<T> -> value
        else -> runBlocking { first() }
    }

@Composable
inline fun <T> collectAsState(crossinline block: () -> Flow<T>): State<T> =
    remember(calculation = block).let { flow ->
        flow.collectAsState(initial = remember { flow.firstBlocking() })
    }

@Composable
inline fun <T> collectAsStateWithLifecycle(crossinline block: () -> Flow<T>): State<T> =
    remember(calculation = block).let { flow ->
        flow.collectAsStateWithLifecycle(initialValue = remember { flow.firstBlocking() })
    }

@Composable
inline fun <T> collectItemAsState(
    crossinline block: () -> DataStoreValue<T>
): State<T> =
    remember {
        val item = block()
        item.asFlow() to runBlocking { item() }
    }.run { first.collectAsState(initial = second) }

@Composable
inline fun <T> rememberFirst(crossinline block: () -> Flow<T>): T =
    remember { block().firstBlocking() }

@Composable
inline fun <T> rememberFirstValue(crossinline block: () -> DataStoreValue<T>): T =
    remember {
        val item = block()
        runBlocking { item() }
    }