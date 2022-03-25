package com.demich.cps.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.demich.cps.accounts.allAccountManagers
import com.demich.cps.accounts.managers.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.google.accompanist.systemuicontroller.SystemUiController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun CPSStatusBar(
    systemUiController: SystemUiController
) {
    val context = context
    val coloredStatusBar by rememberCollect { context.settingsUI.coloredStatusBar.flow }

    val bestOrder by rememberCollect { makeFlowOfBestOrder(context) }

    ColorizeStatusBar(
        systemUiController = systemUiController,
        coloredStatusBar = coloredStatusBar && bestOrder != null,
        color = bestOrder?.run { manager.colorFor(handleColor) } ?: cpsColors.background
    )
}


private data class RatedOrder(
    val order: Double,
    val handleColor: HandleColor,
    val manager: RatedAccountManager<*>
)

private fun<U: UserInfo> RatedAccountManager<U>.getOrder(userInfo: U): RatedOrder? {
    if (userInfo.status != STATUS.OK) return null
    val rating = getRating(userInfo)
    if(rating == NOT_RATED) return null
    val handleColor = getHandleColor(rating)
    if(handleColor == HandleColor.RED) return RatedOrder(order = 1e9, handleColor = handleColor, manager = this)
    val i = rankedHandleColorsList.indexOfFirst { handleColor == it }
    val j = rankedHandleColorsList.indexOfLast { handleColor == it }
    val pos = ratingsUpperBounds.indexOfFirst { it.first == handleColor }
    require(i != -1 && j >= i && pos != -1)
    val lower = if(pos > 0) ratingsUpperBounds[pos-1].second else 0
    val upper = ratingsUpperBounds[pos].second
    val blockLength = (upper - lower).toDouble() / (j - i + 1)
    return RatedOrder(
        order = i + (rating - lower) / blockLength,
        handleColor = handleColor,
        manager = this
    )
}


private fun<U: UserInfo> RatedAccountManager<U>.flowOfRatedOrder(): Flow<RatedOrder?> =
    flowOfInfo().map { getOrder(it) }

private fun makeFlowOfBestOrder(context: Context): Flow<RatedOrder?> =
    combine(
        flow = combine(flows = context.allAccountManagers
            .filterIsInstance<RatedAccountManager<*>>()
            .map { it.flowOfRatedOrder() }
        ) { it },
        flow2 = context.settingsUI.statusBarDisabledManagers.flow
    ) { orders, disabledManagers ->
        orders.filterNotNull()
            .filter { it.manager.managerName !in disabledManagers }
            .maxByOrNull { it.order }
    }

@Composable
private fun ColorizeStatusBar(
    systemUiController: SystemUiController,
    coloredStatusBar: Boolean,
    color: Color,
    offColor: Color = cpsColors.background
) {
    /*
        Important:
        with statusbar=off switching dark/light mode MUST be as fast as everywhere else
    */
    val koef by animateFloatAsState(
        targetValue = if (coloredStatusBar) 1f else 0f,
        animationSpec = tween(buttonOnOffDurationMillis)
    )
    val statusBarColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(buttonOnOffDurationMillis)
    )
    systemUiController.setStatusBarColor(
        color = lerp(offColor, statusBarColor, koef),
        darkIcons = MaterialTheme.colors.isLight
    )
}

@Composable
internal fun StatusBarAccountsButton(
    enabled: Boolean
) {
    var showPopup by rememberSaveable { mutableStateOf(false) }
    Box {
        CPSIconButton(
            icon = Icons.Default.ExpandMore,
            enabled = enabled,
            onState = enabled
        ) {
            showPopup = true
        }
        StatusBarAccountsPopup(expanded = showPopup) { showPopup = false }
    }
}

@Composable
private fun StatusBarAccountsPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit
) {
    val context = context
    val scope = rememberCoroutineScope()
    val disabledManagers by rememberCollect {
        context.settingsUI.statusBarDisabledManagers.flow
    }

    val ratedAccountsArray by rememberCollect {
        combine(
            flows = context.allAccountManagers
                .filterIsInstance<RatedAccountManager<*>>()
                .map { it.flowOfInfoWithManager() }
        ) { it }
    }

    val recordedAccounts by remember {
        derivedStateOf { ratedAccountsArray.filterNot { it.userInfo.isEmpty() } }
    }

    CPSDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        recordedAccounts.forEach { (_, manager) ->
            DropdownMenuItem(onClick = { }) {
                CPSCheckBox(checked = manager.managerName !in disabledManagers) { checked ->
                    val newValue = if (checked) disabledManagers - manager.managerName
                    else disabledManagers + manager.managerName
                    scope.launch { context.settingsUI.statusBarDisabledManagers(newValue) }
                }
                MonospacedText(text = manager.managerName)
            }
        }
    }
}