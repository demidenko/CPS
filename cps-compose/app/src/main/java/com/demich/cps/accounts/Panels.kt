package com.demich.cps.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.AccountManagerType
import com.demich.cps.accounts.managers.ProfileResultWithManager
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.colorFor
import com.demich.cps.accounts.managers.makeHandleSpan
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.accounts.userinfo.ratingToString
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.append
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.getCurrentTime
import com.demich.kotlin_stdlib_boost.swap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

@Composable
fun <U: UserInfo> ProfilePanel(
    profileResultWithManager: ProfileResultWithManager<U>,
    modifier: Modifier = Modifier,
    visibleOrder: List<AccountManagerType>? = null,
    onReloadRequest: () -> Unit,
    onExpandRequest: () -> Unit
) {
    val profilesViewModel = profilesViewModel()
    val (result, manager) = profileResultWithManager

    var lastClick by remember { mutableStateOf(Instant.DISTANT_PAST) }

    val loadingStatus by collectAsState {
        profilesViewModel.flowOfLoadingStatus(manager)
            .onEach {
                if (it == LoadingStatus.LOADING) lastClick = Instant.DISTANT_PAST
            }
    }

    val clickEnabled = loadingStatus != LoadingStatus.LOADING && visibleOrder == null

    Box(modifier = modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .pointerInput(clickEnabled) {
            if (clickEnabled) {
                detectTapGestures(
                    onPress = {
                        if (tryAwaitRelease()) {
                            lastClick = getCurrentTime()
                        }
                    },
                    onDoubleTap = {
                        onExpandRequest()
                    }
                )
            }
        }
    ) {
        manager.PanelContent(result)

        if (visibleOrder == null) {
            PanelUIButtons(
                loadingStatus = loadingStatus,
                lastClick = lastClick,
                onReloadRequest = onReloadRequest,
                onExpandRequest = onExpandRequest,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        } else {
            PanelMovingButtons(
                type = manager.type,
                visibleOrder = visibleOrder,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

    }
}

@Composable
private fun PanelUIButtons(
    loadingStatus: LoadingStatus,
    lastClick: Instant,
    modifier: Modifier = Modifier,
    onReloadRequest: () -> Unit,
    onExpandRequest: () -> Unit
) {
    Row(modifier = modifier) {
        val uiAlpha by hidingState(lastClick)
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
private fun PanelMovingButtons(
    type: AccountManagerType,
    visibleOrder: List<AccountManagerType>,
    modifier: Modifier = Modifier
) {
    val context = context
    val scope = rememberCoroutineScope()
    fun saveSwapped(i: Int, j: Int) {
        scope.launch {
            context.settingsUI.profilesOrder.setValue(visibleOrder.toMutableList().apply { swap(i, j) })
        }
    }
    val index = visibleOrder.indexOf(type)
    PanelMovingButtons(
        modifier = modifier,
        onUpClick = {
            saveSwapped(index - 1, index)
        }.takeIf { index > 0 },
        onDownClick = {
            saveSwapped(index, index + 1)
        }.takeIf { index + 1 < visibleOrder.size }
    )
}

@Composable
private fun PanelMovingButtons(
    modifier: Modifier = Modifier,
    onUpClick: (() -> Unit)? = null,
    onDownClick: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        if (onUpClick != null) {
            Icon(
                imageVector = CPSIcons.MoveUp,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .clickable(onClick = onUpClick)
            )
        }
        if (onDownClick != null) {
            Icon(
                imageVector = CPSIcons.MoveDown,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .clickable(onClick = onDownClick)
            )
        }
    }
}

@Composable
private fun hidingState(
    lastClick: Instant
): State<Float> = produceState(initialValue = 0f, key1 = lastClick) {
    val delay = 3.seconds
    val hideDuration = 2.seconds
    value = 1f
    while (isActive) {
        val dist = getCurrentTime() - lastClick
        if (dist > delay + hideDuration) {
            value = 0f
            break
        }
        if (dist < delay) {
            delay(delay - dist)
        } else {
            value = ((hideDuration - (dist - delay)) / hideDuration).toFloat()
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
fun <U: RatedUserInfo> RatedAccountManager<U>.SmallRatedAccountPanel(
    profileResult: ProfileResult<U>,
    title: @Composable () -> Unit = {
        Text(
            text = makeHandleSpan(profileResult),
            fontSize = 30.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    },
    additionalTitle: @Composable () -> Unit = {
        if (profileResult is ProfileResult.Success) {
            val userInfo = profileResult.userInfo
            Text(
                text = userInfo.ratingToString(),
                fontSize = 25.sp,
                color = userInfo.rating?.let { colorFor(rating = it) } ?: cpsColors.contentAdditional,
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