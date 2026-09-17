package com.demich.cps.profiles

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.demich.cps.platforms.Platform
import com.demich.cps.profiles.managers.ProfileResultWithManager
import com.demich.cps.profiles.managers.RatedProfileManager
import com.demich.cps.profiles.managers.colorFor
import com.demich.cps.profiles.managers.makeHandleSpan
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.RatedUserInfo
import com.demich.cps.profiles.userinfo.UserInfo
import com.demich.cps.profiles.userinfo.ratingToString
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.append
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.getSystemTime
import com.demich.cps.utils.ifThen
import com.demich.datastore_itemized.setValueIn
import com.demich.kotlin_stdlib_boost.swap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Composable
fun <U: UserInfo> ProfilePanel(
    profileResultWithManager: ProfileResultWithManager<U>,
    modifier: Modifier = Modifier,
    visibleOrder: List<Platform>? = null,
    onReloadRequest: () -> Unit,
    onExpandRequest: () -> Unit
) {
    val context = context
    val profilesViewModel = profilesViewModel()
    val (result, manager) = profileResultWithManager

    val lastClickState = remember { mutableStateOf(Instant.DISTANT_PAST) }

    val loadingStatus by collectAsState {
        profilesViewModel.flowOfLoadingStatus(manager)
            .onEach {
                // TODO
                if (it == LOADING) lastClickState.value = Instant.DISTANT_PAST
            }
    }

    val mode = if (visibleOrder != null) {
        PanelMode.Reorder(
            index = visibleOrder.indexOf(manager.platform),
            count = visibleOrder.size
        )
    } else {
        when (loadingStatus) {
            LOADING -> PanelMode.Reloading
            else -> PanelMode.Pending(
                isFailed = loadingStatus == FAILED,
                lastClick = lastClickState.value
            )
        }
    }

    Box(modifier = modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .ifThen(mode is PanelMode.Pending) {
            pointerInput(lastClickState, onExpandRequest) {
                detectTapGestures(
                    onPress = {
                        if (tryAwaitRelease()) {
                            lastClickState.value = getSystemTime()
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

        PanelUIButtons(
            mode = mode,
            modifier = Modifier.align(Alignment.CenterEnd),
            onReloadRequest = onReloadRequest,
            onExpandRequest = onExpandRequest,
            onSwap = { i, j ->
                val visibleOrder = requireNotNull(visibleOrder) // TODO
                context.settingsUI.profilesOrder.setValueIn(
                    scope = profilesViewModel.viewModelScope,
                    value = visibleOrder.toMutableList().apply { swap(i, j) }
                )
            }
        )
    }
}


@Composable
private fun PanelUIButtons(
    mode: PanelMode,
    modifier: Modifier = Modifier,
    onReloadRequest: () -> Unit,
    onExpandRequest: () -> Unit,
    onSwap: (Int, Int) -> Unit
) {
    when (mode) {
        is PanelMode.Reorder -> {
            val index = mode.index
            PanelMovingButtons(
                modifier = modifier,
                onUpClick = {
                    onSwap(index - 1, index)
                }.takeIf { index > 0 },
                onDownClick = {
                    onSwap(index, index + 1)
                }.takeIf { index + 1 < mode.count }
            )
        }
        is PanelMode.Reloading -> {
            CPSReloadingButton(
                loadingStatus = LOADING,
                onClick = onReloadRequest,
                modifier = modifier
            )
        }
        is PanelMode.Pending -> {
            val loadingStatus: LoadingStatus = if (mode.isFailed) FAILED else PENDING
            Row(modifier = modifier) {
                val uiAlpha by hidingState(mode.lastClick)
                if (uiAlpha > 0f) {
                    CPSIconButton(
                        icon = CPSIcons.Expand,
                        modifier = Modifier.alpha(uiAlpha),
                        onClick = onExpandRequest
                    )
                }
                if (loadingStatus != PENDING || uiAlpha > 0f) {
                    CPSReloadingButton(
                        loadingStatus = loadingStatus,
                        modifier = Modifier.alpha(if (loadingStatus == PENDING) uiAlpha else 1f),
                        onClick = onReloadRequest
                    )
                }
            }
        }
    }
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

private sealed interface PanelMode {
    data object Reloading: PanelMode

    data class Pending(
        val isFailed: Boolean,
        val lastClick: Instant
    ): PanelMode

    data class Reorder(
        val index: Int,
        val count: Int
    ): PanelMode
}

@Composable
private fun hidingState(
    lastClick: Instant
): State<Float> = produceState(initialValue = 0f, key1 = lastClick) {
    val delay = 3.seconds
    val hideDuration = 2.seconds
    value = 1f
    while (isActive) {
        val dist = getSystemTime() - lastClick
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
fun SmallProfilePanelTwoLines(
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
fun <U: RatedUserInfo> RatedProfileManager<U>.SmallRatedProfilePanel(
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
                color = userInfo.rating?.let { cpsColors.colorFor(rating = it) } ?: cpsColors.contentAdditional,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
) {
    SmallProfilePanelTwoLines(
        title = title,
        additionalTitle = additionalTitle
    )
}

@Composable
fun SmallProfilePanelTypeArchive(
    title: String,
    infoArgs: List<Pair<String, String>>
) {
    SmallProfilePanelTwoLines(
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