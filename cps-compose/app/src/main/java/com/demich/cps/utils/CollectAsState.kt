package com.demich.cps.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demich.datastore_itemized.DataStoreValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@RememberInComposition
fun <T> Flow<T>.firstBlocking(): T =
    when (this) {
        is StateFlow<T> -> value
        else -> runBlocking { first() }
    }

@RememberInComposition
fun <T> DataStoreValue<T>.getValueBlocking(): T =
    runBlocking { invoke() }

@Composable
inline fun <T> collectAsState(crossinline block: () -> Flow<T>): State<T> =
    rememberWithFirst(block = block).let { (flow, value) ->
        flow.collectAsState(initial = value)
    }

@Composable
inline fun <T> collectAsStateWithLifecycle(crossinline block: () -> Flow<T>): State<T> =
    rememberWithFirst(block = block).let { (flow, value) ->
        flow.collectAsStateWithLifecycle(initialValue = value)
    }

@Composable
inline fun <T> collectItemAsState(
    crossinline block: () -> DataStoreValue<T>
): State<T> =
    remember { block().run { asFlow() to getValueBlocking() } }.let { (flow, value) ->
        flow.collectAsState(initial = value)
    }

@Composable
inline fun <T> rememberFirst(crossinline block: () -> Flow<T>): T =
    remember { block().firstBlocking() }

@Composable
inline fun <T> rememberWithFirst(crossinline block: () -> Flow<T>): Pair<Flow<T>, T> =
    remember {
        val flow = block()
        flow to flow.firstBlocking()
    }

@Composable
inline fun <T> rememberFirstValue(crossinline block: () -> DataStoreValue<T>): T =
    remember { block().getValueBlocking() }