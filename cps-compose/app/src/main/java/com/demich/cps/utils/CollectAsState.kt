package com.demich.cps.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
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

@Composable
inline fun <T> rememberRunBlocking(crossinline block: suspend () -> T): T =
    remember { runBlocking { block() } }

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
    rememberRunBlocking { block().run { asFlow() to invoke() } }.let { (flow, value) ->
        flow.collectAsState(initial = value)
    }

@Composable
fun <T: R, R> DataStoreValue<T>.collectAsState(
    initial: R
): State<R> =
    produceState(initialValue = initial, key1 = this) {
        asFlow().collect { value = it }
    }

@Composable
fun <T: Any> DataStoreValue<T>.collectAsNullableState(): State<T?> =
    collectAsState(initial = null)

@Composable
inline fun <T> rememberWithFirst(crossinline block: () -> Flow<T>): Pair<Flow<T>, T> =
    remember {
        val flow = block()
        flow to flow.firstBlocking()
    }

@Composable
fun <T: Any> Flow<T>.collectUntilFirst(): State<T?> =
    produceState(initialValue = null, key1 = this) {
        value = first()
    }

@Composable
fun <T: Any> DataStoreValue<T>.collectUntilFirst(): State<T?> =
    produceState(initialValue = null) {
        value = invoke()
    }

@Composable
fun <T: Any> suspendAsState(block: suspend () -> T): State<T?> =
    produceState(initialValue = null) { //key1 = block????
        value = block()
    }