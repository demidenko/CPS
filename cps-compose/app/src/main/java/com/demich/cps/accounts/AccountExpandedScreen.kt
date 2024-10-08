package com.demich.cps.accounts

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.accounts.managers.AccountManager
import com.demich.cps.accounts.managers.AccountManagerType
import com.demich.cps.accounts.managers.allAccountManagers
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.dialogs.CPSDeleteDialog
import com.demich.cps.utils.context
import com.demich.cps.utils.openUrlInBrowser
import com.demich.cps.utils.collectAsState
import kotlinx.coroutines.launch

@Composable
fun AccountExpandedScreen(
    type: AccountManagerType,
    showDeleteDialog: Boolean,
    onDeleteRequest: (AccountManager<out UserInfo>) -> Unit,
    onDismissDeleteDialog: () -> Unit,
    setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit
) {
    val manager = remember(type) { allAccountManagers.first { it.type == type } }
    AccountExpandedContent(
        manager = manager,
        setBottomBarContent = setBottomBarContent
    )

    if (showDeleteDialog) {
        CPSDeleteDialog(
            title = "Delete $type account?",
            onConfirmRequest = { onDeleteRequest(manager) },
            onDismissRequest = onDismissDeleteDialog
        )
    }
}

@Composable
private fun<U: UserInfo> AccountExpandedContent(
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

fun accountExpandedMenuBuilder(
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