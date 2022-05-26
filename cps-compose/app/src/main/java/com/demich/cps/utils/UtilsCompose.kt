package com.demich.cps.utils

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
        if (flow is StateFlow<T>) flow.collectAsState()
        else flow.collectAsState(initial = remember(flow) { runBlocking { flow.first() } })
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
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null
) {
    append(
        AnnotatedString(
            text = text,
            spanStyle = SpanStyle(
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle
            )
        )
    )
}

fun Modifier.clickableNoRipple(
    enabled: Boolean = true,
    onClick: () -> Unit
) = composed {
    this.clickable(
        enabled = enabled,
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick
    )
}

val LocalCurrentTime = compositionLocalOf { getCurrentTime() }

private fun currentTimeFlow(period: Duration): Flow<Instant> =
    flow {
        val periodMillis = period.inWholeMilliseconds
        require(periodMillis > 0)
        while (currentCoroutineContext().isActive) {
            val currentMillis = getCurrentTime().toEpochMilliseconds()
            val currentTimeMillisFloored = (currentMillis / periodMillis) * periodMillis
            emit(Instant.fromEpochMilliseconds(currentTimeMillisFloored))
            val rem = currentMillis % periodMillis
            delay(timeMillis = if (rem == 0L) periodMillis else periodMillis - rem)
        }
    }

@Composable
fun collectCurrentTimeEachSecond(): State<Instant> {
    return remember {
        currentTimeFlow(period = 1.seconds)
    }.collectAsStateLifecycleAware(initial = remember { getCurrentTime() })
}


@Composable
fun collectCurrentTimeEachMinute(): State<Instant> {
    return remember {
        currentTimeFlow(period = 1.minutes)
    }.collectAsStateLifecycleAware(initial = remember { getCurrentTime() })
}


fun LazyListState.visibleRange(): IntRange {
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return IntRange.EMPTY

    val firstVisibleItemIndex = firstVisibleItemIndex
    visibleItems.forEachIndexed { index, info -> require(info.index == firstVisibleItemIndex + index) }

    //assume less 50% of visibility as not visible
    val firstVisible = firstVisibleItemIndex.let { index ->
        val item = visibleItems[0]
        val topHidden = (-item.offset).coerceAtLeast(0)
        if (topHidden * 2 > item.size) index + 1 else index
    }
    val lastVisible = (firstVisibleItemIndex + visibleItems.size - 1).let { index ->
        val item = visibleItems.last()
        val bottomHidden = (item.offset + item.size - layoutInfo.viewportEndOffset)
            .coerceAtLeast(0)
        if (bottomHidden * 2 > item.size) index - 1 else index
    }

    return firstVisible .. lastVisible
}