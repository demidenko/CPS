package com.demich.cps.accounts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.CPSViewModels
import com.demich.cps.navigation.Screen
import com.demich.cps.accounts.managers.*
import com.demich.cps.ui.*
import com.demich.cps.ui.dialogs.CPSDeleteDialog
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.combine
import com.demich.cps.utils.context
import com.demich.cps.utils.openUrlInBrowser
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountsScreen(
    accountsViewModel: AccountsViewModel,
    onExpandAccount: (AccountManagers) -> Unit,
    onSetAdditionalMenu: (CPSMenuBuilder) -> Unit,
    reorderEnabled: () -> Boolean,
    enableReorder: () -> Unit
) {
    val recordedAccounts by rememberRecordedAccounts()

    val visibleOrder by remember {
        derivedStateOf { if (reorderEnabled()) recordedAccounts.map { it.type } else null }
    }
    if (recordedAccounts.size > 1) {
        onSetAdditionalMenu {
            CPSDropdownMenuItem(
                title = "Reorder",
                icon = CPSIcons.Reorder,
                onClick = enableReorder
            )
        }
    }

    LazyColumn(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 10.dp)
            .background(cpsColors.background)
    ) {
        itemsNotEmpty(
            items = recordedAccounts,
            key = { it.type },
            onEmptyMessage = { Text(text = "Accounts are not defined") }
        ) { userInfoWithManager ->
            AccountPanel(
                userInfoWithManager = userInfoWithManager,
                onReloadRequest = { accountsViewModel.reload(userInfoWithManager.manager) },
                onExpandRequest = { onExpandAccount(userInfoWithManager.type) },
                accountsViewModel = accountsViewModel,
                visibleOrder = visibleOrder,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .animateItemPlacement()
            )
        }
    }

}

@Composable
private fun rememberRecordedAccounts() = with(context) {
    rememberCollect {
        combine(
            flows = allAccountManagers
                .map { it.flowOfInfoWithManager() }
        ) { it }.combine(settingsUI.flowOfAccountsOrder()) { accountsArray, order ->
            order.mapNotNull { type ->
                accountsArray.find { it.type == type }?.takeIf { !it.userInfo.isEmpty() }
            }
        }
    }
}

@Composable
fun AccountExpandedScreen(
    type: AccountManagers,
    showDeleteDialog: Boolean,
    onDeleteRequest: (AccountManager<out UserInfo>) -> Unit,
    onDismissDeleteDialog: () -> Unit,
    setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit
) {
    val context = context
    val manager = remember(type) { context.allAccountManagers.first { it.type == type } }
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
    val userInfo by rememberCollect { manager.flowOfInfo() }
    manager.ExpandedContent(
        userInfo = userInfo,
        setBottomBarContent = setBottomBarContent,
        modifier = Modifier
            .padding(all = 10.dp)
            .fillMaxSize()
    )
}

@Composable
fun AccountSettingsScreen(
    type: AccountManagers
) {
    val context = context
    val scope = rememberCoroutineScope()

    val manager = remember(type) { context.allAccountManagers.first { it.type == type } }
    val userInfo by rememberCollect { manager.flowOfInfo() }

    var showChangeDialog by remember { mutableStateOf(false) }

    SettingsColumn {
        SettingsItem(
            modifier = Modifier.clickable { showChangeDialog = true }
        ) {
            Column {
                Text(
                    text = manager.userIdTitle + ":",
                    color = cpsColors.contentAdditional,
                    fontSize = 18.sp
                )
                Text(
                    text = userInfo.userId,
                    fontSize = 26.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (manager is AccountSettingsProvider) {
            manager.SettingsItems()
        }
    }

    if (showChangeDialog) {
        manager.ChangeSavedInfoDialog(
            scope = scope,
            onDismissRequest = { showChangeDialog = false }
        )
    }
}

fun accountExpandedMenuBuilder(
    type: AccountManagers,
    navigator: CPSNavigator,
    onShowDeleteDialog: () -> Unit
): CPSMenuBuilder = {
    val context = context
    CPSDropdownMenuItem(title = "Delete", icon = CPSIcons.Delete, onClick = onShowDeleteDialog)
    CPSDropdownMenuItem(title = "Settings", icon = CPSIcons.Settings) {
        navigator.navigateTo(Screen.AccountSettings(type))
    }
    CPSDropdownMenuItem(title = "Origin", icon = CPSIcons.Origin) {
        val url = context.allAccountManagers.first { it.type == type }.run {
            runBlocking { getSavedInfo().userPageUrl }
        }
        context.openUrlInBrowser(url)
    }
}

fun accountsBottomBarBuilder(
    cpsViewModels: CPSViewModels,
    reorderEnabled: () -> Boolean,
    onReorderDone: () -> Unit
): AdditionalBottomBarBuilder = {
    if (reorderEnabled()) {
        CPSIconButton(
            icon = CPSIcons.ReorderDone,
            onClick = onReorderDone
        )
    } else {
        AddAccountButton(cpsViewModels)
        ReloadAccountsButton(cpsViewModels.accountsViewModel)
    }
}

@Composable
private fun ReloadAccountsButton(accountsViewModel: AccountsViewModel) {
    val context = context

    val loadingStatus by rememberCollect {
        context.allAccountManagers
            .map { accountsViewModel.flowOfLoadingStatus(it) }
            .combine()
    }

    val anyRecordedAccount by rememberCollect {
        combine(flows = context.allAccountManagers.map { it.flowOfInfo() }) {
            it.any { manager -> !manager.isEmpty() }
        }
    }

    CPSReloadingButton(
        loadingStatus = loadingStatus,
        enabled = anyRecordedAccount
    ) {
        context.allAccountManagers.forEach { accountsViewModel.reload(it) }
    }
}

@Composable
private fun AddAccountButton(cpsViewModels: CPSViewModels) {
    var showMenu by remember { mutableStateOf(false) }
    var chosenManager: AccountManagers? by remember { mutableStateOf(null) }

    val context = context
    val scope = rememberCoroutineScope()

    Box {
        CPSIconButton(
            icon = CPSIcons.Add,
            enabled = cpsViewModels.progressBarsViewModel.clistImportIsRunning.not(),
            onClick = { showMenu = true }
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(cpsColors.backgroundAdditional)
        ) {
            context.allAccountManagers
                .filter { runBlocking { it.getSavedInfo() }.isEmpty() }
                .forEach { manager ->
                    DropdownMenuItem(
                        onClick = {
                            showMenu = false
                            chosenManager = manager.type
                        },
                        content = { MonospacedText(text = manager.type.name) }
                    )
                }
            DropdownMenuItem(
                onClick = {
                    showMenu = false
                    chosenManager = AccountManagers.clist
                },
                content = { MonospacedText(text = "import from clist.by") }
            )
        }
        
        chosenManager?.let { type -> 
            if (type == AccountManagers.clist) {
                CListImportDialog(cpsViewModels) { chosenManager = null }
            } else {
                context.allAccountManagers
                    .first { it.type == type }
                    .ChangeSavedInfoDialog(
                        scope = scope,
                        onDismissRequest = { chosenManager = null }
                    )
            }
        }
    }
}

@Composable
private fun<U: UserInfo> AccountManager<U>.ChangeSavedInfoDialog(
    scope: CoroutineScope,
    onDismissRequest: () -> Unit
) {
    DialogAccountChooser(
        manager = this,
        initialUserInfo = runBlocking { getSavedInfo() },
        onDismissRequest = onDismissRequest,
        onResult = { userInfo -> scope.launch { setSavedInfo(userInfo) } }
    )
}

@Composable
private fun CListImportDialog(
    cpsViewModels: CPSViewModels,
    onDismissRequest: () -> Unit
) {
    val context = context
    val cListAccountManager = remember { CListAccountManager(context) }
    DialogAccountChooser(
        manager = cListAccountManager,
        initialUserInfo = cListAccountManager.emptyInfo(),
        onDismissRequest = onDismissRequest,
        onResult = { userInfo ->
            cpsViewModels.accountsViewModel.runClistImport(
                cListUserInfo = userInfo,
                progressBarsViewModel = cpsViewModels.progressBarsViewModel,
                context = context
            )
        }
    )
}