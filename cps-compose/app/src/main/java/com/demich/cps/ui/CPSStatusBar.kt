package com.demich.cps.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demich.cps.Screen
import com.demich.cps.accounts.allAccountManagers
import com.demich.cps.accounts.managers.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.paddingHorizontal
import com.demich.cps.utils.rememberCollect
import com.google.accompanist.systemuicontroller.SystemUiController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun CPSStatusBar(
    systemUiController: SystemUiController,
    currentScreen: Screen?
) {
    val context = context
    val coloredStatusBar by rememberCollect { context.settingsUI.coloredStatusBar.flow }
    val orderGetter by rememberCollect { makeFlowOfOrderGetter(context) }
    ColorizeStatusBar(
        systemUiController = systemUiController,
        coloredStatusBar = coloredStatusBar,
        order = orderGetter[currentScreen]
    )
}

@Composable
private fun ColorizeStatusBar(
    systemUiController: SystemUiController,
    coloredStatusBar: Boolean,
    order: RatedOrder?,
    offColor: Color = cpsColors.background
) {
    ColorizeStatusBar(
        systemUiController = systemUiController,
        isStatusBarEnabled = coloredStatusBar && order != null,
        color = order?.run { manager.colorFor(handleColor) } ?: offColor,
        offColor = offColor
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
    flowOfInfo().map(this::getOrder)

private data class OrderGetter(
    private val validOrders: List<RatedOrder>,
    private val orderByMaximum: Boolean
) {
    operator fun get(screen: Screen?): RatedOrder? {
        return if (screen is Screen.AccountExpanded) {
            validOrders.find { it.manager.type == screen.type }
        } else {
            if (orderByMaximum) validOrders.maxByOrNull { it.order }
            else validOrders.minByOrNull { it.order }
        }
    }
}

private fun makeFlowOfOrderGetter(context: Context): Flow<OrderGetter> =
    combine(
        flow = combine(flows = context.allAccountManagers
            .filterIsInstance<RatedAccountManager<*>>()
            .map { it.flowOfRatedOrder() }
        ) { it },
        flow2 = context.settingsUI.statusBarDisabledManagers.flow,
        flow3 = context.settingsUI.statusBarOrderByMaximum.flow
    ) { orders, disabledManagers, orderByMaximum ->
        OrderGetter(
            validOrders = orders.filterNotNull().filter { it.manager.type !in disabledManagers },
            orderByMaximum = orderByMaximum
        )
    }.distinctUntilChanged()

@Composable
private fun ColorizeStatusBar(
    systemUiController: SystemUiController,
    isStatusBarEnabled: Boolean,
    color: Color,
    offColor: Color
) {
    /*
        Important:
        with statusbar=off switching dark/light mode MUST be as fast as everywhere else
    */
    val koef by animateFloatAsState(
        targetValue = if (isStatusBarEnabled) 1f else 0f,
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
fun StatusBarButtonsForUIPanel() {
    val scope = rememberCoroutineScope()

    val context = context
    val settingsUI = remember { context.settingsUI }

    val coloredStatusBar by rememberCollect { settingsUI.coloredStatusBar.flow }

    val ratedAccountsArray by rememberCollect {
        combine(
            flows = context.allAccountManagers
                .filterIsInstance<RatedAccountManager<*>>()
                .map { it.flowOfInfoWithManager() }
        ) { it }
    }
    val recordedAccounts by remember {
        derivedStateOf {
            ratedAccountsArray.filterNot { it.userInfo.isEmpty() }
        }
    }

    var showPopup by rememberSaveable { mutableStateOf(false) }
    val orderByMaximum by rememberCollect { settingsUI.statusBarOrderByMaximum.flow }
    val disabledManagers by rememberCollect { settingsUI.statusBarDisabledManagers.flow }

    val noneEnabled by remember {
        derivedStateOf {
            recordedAccounts.all { it.type in disabledManagers }
        }
    }

    if (recordedAccounts.isNotEmpty()) {
        CPSIconButton(
            icon = Icons.Default.WebAsset,
            onState = coloredStatusBar && !noneEnabled,
        ) {
            if (noneEnabled) {
                showPopup = true
            } else {
                scope.launch { settingsUI.coloredStatusBar(!coloredStatusBar) }
            }
        }
        if (coloredStatusBar) Box {
            CPSIconButton(
                icon = Icons.Default.ExpandMore,
                onClick = { showPopup = true }
            )
            StatusBarAccountsPopup(
                expanded = showPopup,
                orderByMaximum = orderByMaximum,
                disabledManagers = disabledManagers,
                recordedAccounts = recordedAccounts,
                onDismissRequest = { showPopup = false }
            )
        }
    }

}


@Composable
private fun StatusBarAccountsPopup(
    expanded: Boolean,
    orderByMaximum: Boolean,
    disabledManagers: Set<AccountManagers>,
    recordedAccounts: List<UserInfoWithManager<out UserInfo>>,
    onDismissRequest: () -> Unit
) {
    val settingsUI = with(context) { remember { settingsUI } }
    val scope = rememberCoroutineScope()

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.background(cpsColors.backgroundAdditional)
    ) {
        recordedAccounts.forEach { (_, manager) ->
            Row(modifier = Modifier.padding(end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CPSCheckBox(checked = manager.type !in disabledManagers) { checked ->
                    val newValue = if (checked) disabledManagers - manager.type
                    else disabledManagers + manager.type
                    scope.launch { settingsUI.statusBarDisabledManagers(newValue) }
                }
                MonospacedText(text = manager.type.name)
            }
        }
        Row(modifier = Modifier.paddingHorizontal(10.dp).align(Alignment.CenterHorizontally)) {
            Button(
                content = { Text(text = "worst", fontWeight = FontWeight.Bold.takeIf { !orderByMaximum }) },
                onClick = {
                    scope.launch { settingsUI.statusBarOrderByMaximum(false) }
                }
            )
            Button(
                content = { Text(text = "best", fontWeight = FontWeight.Bold.takeIf { orderByMaximum }) },
                onClick = {
                    scope.launch { settingsUI.statusBarOrderByMaximum(true) }
                }
            )
        }
    }
}