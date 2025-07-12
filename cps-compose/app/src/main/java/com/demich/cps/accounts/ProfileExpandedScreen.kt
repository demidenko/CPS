package com.demich.cps.accounts

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.accounts.managers.AccountManager
import com.demich.cps.accounts.managers.AccountManagerType
import com.demich.cps.accounts.managers.accountManagerOf
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.accounts.userinfo.userInfoOrNull
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenStaticTitleState
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.dialogs.CPSDeleteDialog
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.openUrlInBrowser
import kotlinx.coroutines.launch


@Composable
private fun <U: UserInfo> ProfileExpandedContent(
    manager: AccountManager<U>,
    setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit
) {
    val context = context
    val profileResult by collectItemAsState { manager.dataStore(context).profile }
    profileResult?.let {
        manager.ExpandedContent(
            profileResult = it,
            setBottomBarContent = setBottomBarContent,
            modifier = Modifier
                .padding(all = 10.dp)
                .fillMaxSize()
        )
    }
}

private fun profileExpandedMenuBuilder(
    type: AccountManagerType,
    onOpenSettings: () -> Unit,
    onShowDeleteDialog: () -> Unit
): CPSMenuBuilder = {
    val context = context
    val scope = rememberCoroutineScope()

    CPSDropdownMenuItem(title = "Delete", icon = CPSIcons.Delete, onClick = onShowDeleteDialog)
    CPSDropdownMenuItem(title = "Settings", icon = CPSIcons.Settings, onClick = onOpenSettings)
    CPSDropdownMenuItem(title = "Origin", icon = CPSIcons.Origin) {
        scope.launch {
            accountManagerOf(type)
                .dataStore(context).profile()
                ?.userInfoOrNull()
                ?.userPageUrl
                ?.let { url -> context.openUrlInBrowser(url) }
        }
    }
}

@Composable
fun CPSNavigator.ScreenScope<Screen.ProfileExpanded>.NavContentProfilesExpandedScreen(
    onOpenSettings: () -> Unit,
    onDeleteRequest: (AccountManager<out UserInfo>) -> Unit
) {
    val type = screen.managerType
    val manager = remember(type) { accountManagerOf(type) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    screenTitle = ScreenStaticTitleState("profiles", type.name)

    menu = profileExpandedMenuBuilder(
        type = type,
        onShowDeleteDialog = { showDeleteDialog = true },
        onOpenSettings = onOpenSettings
    )

    ProfileExpandedContent(
        manager = manager,
        setBottomBarContent = { bottomBar = it }
    )

    if (showDeleteDialog) {
        CPSDeleteDialog(
            title = "Delete $type profile?",
            onConfirmRequest = { onDeleteRequest(manager) },
            onDismissRequest = { showDeleteDialog = false }
        )
    }
}