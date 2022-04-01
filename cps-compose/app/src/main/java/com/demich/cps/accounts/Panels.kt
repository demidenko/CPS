package com.demich.cps.accounts

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.*
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.getCurrentTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun<U: UserInfo> PanelWithUI(
    userInfoWithManager: UserInfoWithManager<U>,
    accountsViewModel: AccountsViewModel,
    modifier: Modifier = Modifier,
    onExpandRequest: () -> Unit
) {
    val (userInfo, manager) = userInfoWithManager
    val loadingStatus by remember { accountsViewModel.loadingStatusFor(manager) }

    var lastClickMillis by remember { mutableStateOf(0L) }
    val uiAlpha by hidingState(lastClickMillis)

    if (loadingStatus == LoadingStatus.LOADING) lastClickMillis = 0

    if (!userInfo.isEmpty()) {
        Box(modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (loadingStatus != LoadingStatus.LOADING) {
                            lastClickMillis = getCurrentTime().toEpochMilliseconds()
                        }
                    },
                    onDoubleTap = {
                        onExpandRequest()
                    }
                )
            }
        ) {
            manager.Panel(userInfo)

            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                if (loadingStatus != LoadingStatus.LOADING && uiAlpha > 0f) {
                    CPSIconButton(
                        icon = Icons.Default.UnfoldMore,
                        modifier = Modifier.alpha(uiAlpha),
                        onClick = onExpandRequest
                    )
                }
                if (loadingStatus != LoadingStatus.PENDING || uiAlpha > 0f) {
                    CPSReloadingButton(
                        loadingStatus = loadingStatus,
                        modifier = Modifier.alpha(if (loadingStatus == LoadingStatus.PENDING) uiAlpha else 1f)
                    ) {
                        accountsViewModel.reload(manager)
                    }
                }
            }
        }
    }
}

@Composable
private fun hidingState(
    lastClickMillis: Long
): State<Float> {
    val a = remember { mutableStateOf(0f) }
    val delayMillis = 3000
    val durationMillis = 2000
    LaunchedEffect(key1 = lastClickMillis) {
        a.value = 1f
        while (isActive) {
            val dist = getCurrentTime().toEpochMilliseconds() - lastClickMillis
            if (dist > delayMillis + durationMillis) {
                a.value = 0f
                break
            }
            if (dist < delayMillis) {
                delay(delayMillis - dist)
            } else {
                a.value = (durationMillis - (dist - delayMillis)).toFloat() / durationMillis
                delay(100)
            }
        }
    }
    return a
}

@Composable
private fun<S> AutoHiding(
    currentState: S,
    startHidingState: S,
    finishHidingState: S,
    hideDelay: Int,
    hideDuration: Int,
    modifier: Modifier = Modifier,
    content: @Composable (S) -> Unit
) {
    val transition = updateTransition(targetState = currentState, label = "")
    val uiAlpha by transition.animateFloat(
        transitionSpec = {
            when {
                //TODO: reversed transitioning is glitching
                startHidingState isTransitioningTo finishHidingState
                -> tween(delayMillis = hideDelay, durationMillis = hideDuration)
                else -> snap()
            }
        },
        label = ""
    ) {
        if (it == finishHidingState) 0f else 1f
    }
    transition.currentState.let {
        if (it != finishHidingState) {
            Box(
                modifier = modifier.alpha(uiAlpha),
                content = { content(it) }
            )
        }
    }
}

@Composable
fun SmallAccountPanelTwoLines(
    title: @Composable () -> Unit,
    additionalTitle: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.Start
    ) {
        title()
        additionalTitle()
    }
}

@Composable
fun<U: UserInfo> RatedAccountManager<U>.SmallAccountPanelTypeRated(userInfo: U) {
    SmallAccountPanelTwoLines(
        title = {
            Text(
                text = makeHandleSpan(userInfo),
                fontSize = 30.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        additionalTitle = {
            if (userInfo.status == STATUS.OK) {
                val rating = getRating(userInfo)
                Text(
                    text = if (rating == NOT_RATED) "[not rated]" else rating.toString(),
                    fontSize = 25.sp,
                    color = if (rating == NOT_RATED) cpsColors.textColorAdditional else colorFor(rating = rating),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Composable
fun SmallAccountPanelTypeArchive(
    title: String,
    infoArgs: List<Pair<String, String>>
) {
    SmallAccountPanelTwoLines(
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                color = cpsColors.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        additionalTitle = {
            Text(
                text = buildAnnotatedString {
                    infoArgs.forEachIndexed { index, arg ->
                        if (index > 0) append("  ")
                        withStyle(SpanStyle(color = cpsColors.textColorAdditional)) {
                            append("${arg.first}: ")
                        }
                        append(arg.second)
                    }
                },
                fontSize = 14.sp,
                color = cpsColors.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}