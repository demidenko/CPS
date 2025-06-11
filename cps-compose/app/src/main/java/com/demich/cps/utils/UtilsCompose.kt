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
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
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
inline fun <T, K> rememberFrom(
    key: K,
    crossinline calculation: @DisallowComposableCalls (K) -> T
): T = remember(key1 = key) {
    key.let(calculation)
}


inline fun Dp.plusIf(condition: Boolean, value: () -> Dp): Dp =
    if (condition) this + value() else this

inline fun Modifier.ifThen(condition: Boolean, block: Modifier.() -> Modifier): Modifier =
    if (condition) then(block()) else this

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
) = clickable(
    enabled = enabled,
    indication = null,
    interactionSource = null,
    onClick = onClick
)

fun Modifier.ignoreInputEvents(enabled: Boolean) =
    ifThen(enabled) {
        pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial)
                        .changes
                        .forEach { it.consume() }
                }
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
    return animateColorAsState(
        enabledColorState = rememberUpdatedState(enabledColor),
        disabledColor = disabledColor,
        enabled = enabled,
        animationSpec = animationSpec
    )
}

@Composable
fun animateColorAsState(
    enabledColorState: State<Color>,
    disabledColor: Color,
    enabled: Boolean,
    animationSpec: AnimationSpec<Float>
): State<Color> {
    val disabledColorState = rememberUpdatedState(disabledColor)
    val fractionState = animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = animationSpec,
        label = "color_fraction"
    )
    return remember(disabledColorState, enabledColorState, fractionState) {
        derivedStateOf {
            lerp(
                start = disabledColorState.value,
                stop = enabledColorState.value,
                fraction = fractionState.value
            )
        }
    }
}


@Composable
fun LaunchedEffectOneTime(
    block: suspend CoroutineScope.() -> Unit
) {
    val state = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (!state.value) {
            block()
            state.value = true
        }
    }
}

@Composable
fun rememberFocusOnCreationRequester(): FocusRequester {
    val requester = remember { FocusRequester() }
    LaunchedEffectOneTime {
        withFrameMillis {  }
        requester.requestFocus()
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


val rememberUUIDState: MutableLongState
    @Composable
    get() = rememberSaveable { mutableLongStateOf(randomUuid()) }

fun randomUuid(): Long = Random.nextLong()