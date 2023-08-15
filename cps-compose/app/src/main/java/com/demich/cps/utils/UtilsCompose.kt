package com.demich.cps.utils

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val context: Context
    @Composable
    @ReadOnlyComposable
    get() = LocalContext.current


inline fun<reified T> Json.saver() =
    Saver<T, String>(
        restore = ::decodeFromString,
        save = { encodeToString(it) }
    )

@Composable
inline fun<T, K> rememberWith(
    key: K,
    crossinline calculation: @DisallowComposableCalls K.() -> T
): T = remember(key1 = key) {
        with(receiver = key, block = calculation)
    }

@Composable
inline fun<T> rememberCollect(crossinline block: () -> Flow<T>): State<T> =
    remember(block).let { flow ->
        if (flow is StateFlow<T>) flow.collectAsState()
        else flow.collectAsState(initial = remember(flow) { runBlocking { flow.first() } })
    }

@Composable
inline fun<T> rememberCollectWithLifecycle(crossinline block: () -> Flow<T>): State<T> =
    remember(block).let { flow ->
        if (flow is StateFlow<T>) flow.collectAsStateWithLifecycle()
        else flow.collectAsStateWithLifecycle(initialValue = remember(flow) { runBlocking { flow.first() } })
    }


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


fun LazyListState.visibleRange(requiredVisiblePart: Float = 0.5f): IntRange {
    require(requiredVisiblePart in 0f..1f)

    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return IntRange.EMPTY

    val firstVisible = visibleItems.first().let { item ->
        val topHidden = (-item.offset).coerceAtLeast(0)
        val visiblePart = (item.size - topHidden).toFloat() / item.size
        if (visiblePart < requiredVisiblePart) item.index + 1 else item.index
    }

    val lastVisible = visibleItems.last().let { item ->
        val bottomHidden = (item.offset + item.size - layoutInfo.viewportEndOffset)
            .coerceAtLeast(0)
        val visiblePart = (item.size - bottomHidden).toFloat() / item.size
        if (visiblePart < requiredVisiblePart) item.index - 1 else item.index
    }

    return firstVisible .. lastVisible
}

@Composable
fun rememberFocusOnCreationRequester(): FocusRequester {
    val requester = remember { FocusRequester() }
    var focusedOnCreation by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(requester) {
        if (!focusedOnCreation) {
            awaitFrame() //TODO is this ok? instead of [if (!focusImmediately) delay(100.milliseconds)]
            requester.requestFocus()
            focusedOnCreation = true
        }
    }
    return requester
}

@Composable
inline fun<reified T: ViewModel> sharedViewModel(): T =
    viewModel(viewModelStoreOwner = context as ComponentActivity)