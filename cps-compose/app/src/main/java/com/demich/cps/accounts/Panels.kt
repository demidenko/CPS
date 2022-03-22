package com.demich.cps.accounts

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
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
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import kotlinx.coroutines.delay

@Composable
fun<U: UserInfo> AccountManager<U>.Panel(
    accountsViewModel: AccountsViewModel,
    modifier: Modifier = Modifier
) {
    val userInfo: U by flowOfInfo().collectAsState(emptyInfo())
    val loadingStatus by accountsViewModel.loadingStatusFor(this)

    var showUI by remember { mutableStateOf(false) }

    if (!userInfo.isEmpty()) {
        Box(modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (loadingStatus != LoadingStatus.LOADING) {
                            showUI = true
                            tryAwaitRelease()
                            delay(300)
                            showUI = false
                        }
                    }
                )
            }
        ) {
            AutoHiding(
                targetState = remember(loadingStatus, showUI) { loadingStatus to showUI },
                finishHidingState = LoadingStatus.PENDING to false,
                startHidingState = LoadingStatus.PENDING to true,
                hideDelay = 3000,
                hideDuration = 2000,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 5.dp)
            ) {
                CPSReloadingButton(loadingStatus = it.first) {
                    accountsViewModel.reload(manager = this@Panel)
                }
            }

            Panel(userInfo)
        }
    }
}


@Composable
private fun<S> AutoHiding(
    targetState: S,
    startHidingState: S,
    finishHidingState: S,
    hideDelay: Int,
    hideDuration: Int,
    modifier: Modifier = Modifier,
    content: @Composable (S) -> Unit
) {
    val transition = updateTransition(targetState = targetState, label = "")
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
                    color = colorFor(rating = rating),
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