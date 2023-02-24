package com.demich.cps.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.*
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun<U: UserInfo> PanelWithUI(
    userInfoWithManager: UserInfoWithManager<U>,
    accountsViewModel: AccountsViewModel,
    modifier: Modifier = Modifier,
    visibleOrder: List<AccountManagers>? = null,
    onExpandRequest: () -> Unit
) {
    val (userInfo, manager) = userInfoWithManager
    val loadingStatus by rememberCollect { accountsViewModel.flowOfLoadingStatus(manager) }

    var lastClickMillis by remember { mutableStateOf(0L) }

    if (loadingStatus == LoadingStatus.LOADING) lastClickMillis = 0

    val clickEnabled = loadingStatus != LoadingStatus.LOADING && visibleOrder == null

    Box(modifier = modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .pointerInput(clickEnabled) {
            if (clickEnabled) {
                detectTapGestures(
                    onPress = {
                        if (tryAwaitRelease()) {
                            lastClickMillis = getCurrentTime().toEpochMilliseconds()
                        }
                    },
                    onDoubleTap = {
                        onExpandRequest()
                    }
                )
            }
        }
    ) {
        manager.Panel(userInfo)

        if (visibleOrder == null) {
            AccountPanelUI(
                loadingStatus = loadingStatus,
                lastClickMillis = lastClickMillis,
                onReloadRequest = { accountsViewModel.reload(manager) },
                onExpandRequest = onExpandRequest,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        } else {
            AccountMovingUI(
                type = manager.type,
                visibleOrder = visibleOrder,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

    }
}

@Composable
private fun AccountPanelUI(
    loadingStatus: LoadingStatus,
    lastClickMillis: Long,
    modifier: Modifier = Modifier,
    onReloadRequest: () -> Unit,
    onExpandRequest: () -> Unit
) {
    Row(modifier = modifier) {
        val uiAlpha by hidingState(lastClickMillis)
        if (loadingStatus != LoadingStatus.LOADING && uiAlpha > 0f) {
            CPSIconButton(
                icon = CPSIcons.Expand,
                modifier = Modifier.alpha(uiAlpha),
                onClick = onExpandRequest
            )
        }
        if (loadingStatus != LoadingStatus.PENDING || uiAlpha > 0f) {
            CPSReloadingButton(
                loadingStatus = loadingStatus,
                modifier = Modifier.alpha(if (loadingStatus == LoadingStatus.PENDING) uiAlpha else 1f),
                onClick = onReloadRequest
            )
        }
    }
}


@Composable
private fun AccountMovingUI(
    type: AccountManagers,
    visibleOrder: List<AccountManagers>,
    modifier: Modifier = Modifier
) {
    val context = context
    val scope = rememberCoroutineScope()
    fun saveOrder(newVisibleOrder: List<AccountManagers>) {
        scope.launch {
            context.settingsUI.saveAccountsOrder(newVisibleOrder)
        }
    }

    Column(modifier = modifier) {
        if (visibleOrder.first() != type) {
            Icon(
                imageVector = CPSIcons.MoveUp,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .clickable {
                        val index = visibleOrder.indexOf(type)
                        saveOrder(visibleOrder.toMutableList().apply {
                            this[index] = this[index - 1]
                            this[index - 1] = type
                        })
                    }
            )
        }
        if (visibleOrder.last() != type) {
            Icon(
                imageVector = CPSIcons.MoveDown,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .clickable {
                        val index = visibleOrder.indexOf(type)
                        saveOrder(visibleOrder.toMutableList().apply {
                            this[index] = this[index + 1]
                            this[index + 1] = type
                        })
                    }
            )
        }
    }
}

@Composable
private fun hidingState(
    lastClickMillis: Long
): State<Float> = produceState(initialValue = 0f, key1 = lastClickMillis) {
    val delayMillis = 3000
    val durationMillis = 2000
    value = 1f
    while (isActive) {
        val dist = getCurrentTime().toEpochMilliseconds() - lastClickMillis
        if (dist > delayMillis + durationMillis) {
            value = 0f
            break
        }
        if (dist < delayMillis) {
            delay(delayMillis - dist)
        } else {
            value = (durationMillis - (dist - delayMillis)).toFloat() / durationMillis
            delay(100)
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
fun<U: RatedUserInfo> RatedAccountManager<U>.SmallRatedAccountPanel(
    userInfo: U,
    title: @Composable () -> Unit = {
        Text(
            text = makeHandleSpan(userInfo),
            fontSize = 30.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    },
    additionalTitle: @Composable () -> Unit = {
        if (userInfo.status == STATUS.OK) {
            Text(
                text = userInfo.ratingToString(),
                fontSize = 25.sp,
                color = if (userInfo.hasRating()) colorFor(rating = userInfo.rating) else cpsColors.contentAdditional,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
) {
    SmallAccountPanelTwoLines(
        title = title,
        additionalTitle = additionalTitle
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
                color = cpsColors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        additionalTitle = {
            Text(
                text = buildAnnotatedString {
                    infoArgs.forEachIndexed { index, (key, value) ->
                        if (index > 0) append("  ")
                        append(text = "${key}: ", color = cpsColors.contentAdditional)
                        append(value)
                    }
                },
                fontSize = 14.sp,
                color = cpsColors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}