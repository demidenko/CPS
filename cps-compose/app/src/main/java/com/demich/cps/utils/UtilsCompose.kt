package com.demich.cps.utils

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.demich.datastore_itemized.DataStoreItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.random.Random

val context: Context
    @Composable
    @ReadOnlyComposable
    get() = LocalContext.current


@Composable
fun ProvideContentColor(color: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides color, content = content)
}

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
inline fun<T> collectAsState(crossinline block: () -> Flow<T>): State<T> =
    remember(block).let { flow ->
        if (flow is StateFlow<T>) flow.collectAsState()
        else flow.collectAsState(initial = remember { runBlocking { flow.first() } })
    }

@Composable
inline fun<T> collectAsStateWithLifecycle(crossinline block: () -> Flow<T>): State<T> =
    remember(block).let { flow ->
        if (flow is StateFlow<T>) flow.collectAsStateWithLifecycle()
        else flow.collectAsStateWithLifecycle(initialValue = remember { runBlocking { flow.first() } })
    }

@Composable
inline fun <T> collectItemAsState(crossinline block: @DisallowComposableCalls () -> DataStoreItem<T>): State<T> =
    remember {
        val item = block()
        item.flow to runBlocking { item() }
    }.run { first.collectAsState(initial = second) }


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
) = this.composed {
    clickable(
        enabled = enabled,
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick
    )
}

fun Modifier.ignoreInputEvents(enabled: Boolean) =
    if (!enabled) this
    else this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial)
                    .changes
                    .forEach { it.consume() }
            }
        }
    }

fun Modifier.background(color: () -> Color) =
    this.drawBehind { drawRect(color = color()) }

@Composable
fun animateColorAsState(
    enabledColor: Color,
    disabledColor: Color,
    enabled: Boolean,
    animationSpec: AnimationSpec<Float>
): State<Color> {
    val fractionState = animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = animationSpec,
        label = "color_fraction"
    )
    return remember(disabledColor, enabledColor, fractionState) {
        derivedStateOf {
            lerp(
                start = disabledColor,
                stop = enabledColor,
                fraction = fractionState.value
            )
        }
    }
}

@Composable
fun animateColorAsState(
    enabledColorState: State<Color>,
    disabledColor: Color,
    enabled: Boolean,
    animationSpec: AnimationSpec<Float>
): State<Color> {
    val fractionState = animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = animationSpec,
        label = "color_fraction"
    )
    return remember(disabledColor, enabledColorState, fractionState) {
        derivedStateOf {
            lerp(
                start = disabledColor,
                stop = enabledColorState.value,
                fraction = fractionState.value
            )
        }
    }
}


@Composable
fun rememberFocusOnCreationRequester(): FocusRequester {
    val requester = remember { FocusRequester() }
    val focusedState = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(requester, focusedState) {
        if (!focusedState.value) {
            withFrameMillis {  }
            requester.requestFocus()
            focusedState.value = true
        }
    }
    return requester
}

@Stable
fun enterInColumn(): EnterTransition = expandVertically() + fadeIn()

@Stable
fun exitInColumn(): ExitTransition = shrinkVertically() + fadeOut()


@Composable
inline fun<reified T: ViewModel> sharedViewModel(): T =
    viewModel(viewModelStoreOwner = context as ComponentActivity)


val currentDataKey: Int
    @Composable
    get() = rememberSaveable { Random.nextInt() }
    //get() = currentCompositeKeyHash //hash is cached in if else