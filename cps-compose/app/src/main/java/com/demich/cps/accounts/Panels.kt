package com.demich.cps.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
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
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.context
import com.demich.cps.utils.getCurrentTime
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
    val loadingStatus by remember { accountsViewModel.loadingStatusFor(manager) }

    var lastClickMillis by remember { mutableStateOf(0L) }
    val uiAlpha by hidingState(lastClickMillis)

    if (loadingStatus == LoadingStatus.LOADING) lastClickMillis = 0

    if (!userInfo.isEmpty()) {
        Box(modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .pointerInput(Unit) {
                //TODO ignore if reorder
                detectTapGestures(
                    onPress = {
                        if (loadingStatus != LoadingStatus.LOADING) {
                            tryAwaitRelease()
                            lastClickMillis = getCurrentTime().toEpochMilliseconds()
                        }
                    },
                    onDoubleTap = {
                        if (loadingStatus != LoadingStatus.LOADING) {
                            onExpandRequest()
                        }
                    }
                )
            }
        ) {
            manager.Panel(userInfo)

            if (visibleOrder == null) {
                AccountPanelUI(
                    loadingStatus = loadingStatus,
                    uiAlpha = uiAlpha,
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
}

@Composable
private fun AccountPanelUI(
    loadingStatus: LoadingStatus,
    uiAlpha: Float,
    modifier: Modifier = Modifier,
    onReloadRequest: () -> Unit,
    onExpandRequest: () -> Unit
) {
    Row(modifier = modifier) {
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
        val item = context.settingsUI.accountsOrder
        scope.launch {
            val oldOrder = item().let { order ->
                //adding looks useless but not
                order + AccountManagers.values().filter { it !in order }
            }
            item(newValue = newVisibleOrder + oldOrder.filter { it !in newVisibleOrder })
        }
    }

    Column(modifier = modifier) {
        if (visibleOrder.first() != type) {
            Icon(
                imageVector = Icons.Default.ArrowDropUp,
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
                imageVector = Icons.Default.ArrowDropDown,
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