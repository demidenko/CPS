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
import com.demich.cps.accounts.managers.allAccountManagers
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.dialogs.CPSDeleteDialog
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.openUrlInBrowser
import kotlinx.coroutines.launch

@Composable
private fun ProfileExpandedScreen(
    type: AccountManagerType,
    showDeleteDialog: Boolean,
    onDeleteRequest: (AccountManager<out UserInfo>) -> Unit,
    onDismissDeleteDialog: () -> Unit,
    setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit
) {
    val manager = remember(type) { allAccountManagers.first { it.type == type } }
    ProfileExpandedContent(
        manager = manager,
        setBottomBarContent = setBottomBarContent
    )

    if (showDeleteDialog) {
        CPSDeleteDialog(
            title = "Delete $type profile?",
            onConfirmRequest = { onDeleteRequest(manager) },
            onDismissRequest = onDismissDeleteDialog
        )
    }
}

@Composable
private fun<U: UserInfo> ProfileExpandedContent(
    manager: AccountManager<U>,
    setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit
) {
    val context = context
    val userInfo by collectAsState { manager.dataStore(context).flowOfInfo() }
    userInfo?.let {
        manager.ExpandedContent(
            userInfo = it,
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
            allAccountManagers.first { it.type == type }
                .dataStore(context).getSavedInfo()
                ?.userPageUrl
                ?.let { url -> context.openUrlInBrowser(url) }
        }
    }
}

@Composable
fun NavContentProfilesExpandedScreen(
    holder: CPSNavigator.DuringCompositionHolder,
    type: AccountManagerType,
    onOpenSettings: () -> Unit,
    onDeleteRequest: (AccountManager<out UserInfo>) -> Unit
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    ProfileExpandedScreen(
        type = type,
        showDeleteDialog = showDeleteDialog,
        onDeleteRequest = onDeleteRequest,
        onDismissDeleteDialog = { showDeleteDialog = false },
        setBottomBarContent = holder.bottomBarSetter
    )

    holder.menu = profileExpandedMenuBuilder(
        type = type,
        onShowDeleteDialog = { showDeleteDialog = true },
        onOpenSettings = onOpenSettings
    )

    holder.setSubtitle("profiles", type.name)
}