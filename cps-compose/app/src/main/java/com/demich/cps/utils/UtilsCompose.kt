package com.demich.cps.utils

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

val context: Context
    @Composable
    @ReadOnlyComposable
    get() = LocalContext.current


@Composable
inline fun<reified T> jsonSaver() = remember {
    Saver<T, String>(
        restore = jsonCPS::decodeFromString,
        save = { jsonCPS.encodeToString(it) }
    )
}


@Composable
fun<T> rememberCollect(block: () -> Flow<T>) =
    remember { block() }.let { flow ->
        flow.collectAsState(initial = remember(flow) { runBlocking { flow.first() } })
    }

//following from https://proandroiddev.com/how-to-collect-flows-lifecycle-aware-in-jetpack-compose-babd53582d0b
@Composable
fun <T> rememberFlow(
    flow: Flow<T>,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
): Flow<T> {
    return remember(
        key1 = flow,
        key2 = lifecycleOwner
    ) { flow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED) }
}

@Composable
fun <T : R, R> Flow<T>.collectAsStateLifecycleAware(
    initial: R,
    context: CoroutineContext = EmptyCoroutineContext
): State<R> {
    val lifecycleAwareFlow = rememberFlow(flow = this)
    return lifecycleAwareFlow.collectAsState(initial = initial, context = context)
}

@Suppress("StateFlowValueCalledInComposition")
@Composable
fun <T> StateFlow<T>.collectAsStateLifecycleAware(
    context: CoroutineContext = EmptyCoroutineContext
): State<T> = collectAsStateLifecycleAware(initial = value, context = context)


fun AnnotatedString.Builder.append(
    text: String,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null
) {
    append(
        AnnotatedString(
        text = text,
        spanStyle = SpanStyle(
            color = color,
            fontWeight = fontWeight,
            fontStyle = fontStyle
        )
    )
    )
}