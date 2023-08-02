package com.demich.cps.accounts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.CPSViewModels
import com.demich.cps.accounts.managers.*
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.ui.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.combine
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountsScreen(
    onExpandAccount: (AccountManagers) -> Unit,
    onSetAdditionalMenu: (CPSMenuBuilder) -> Unit,
    reorderEnabled: () -> Boolean,
    enableReorder: () -> Unit
) {
    val accountsViewModel = accountsViewModel()
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
            flows = allAccountManagers.map { it.flowOfInfoWithManager() }
        ) {
            it.filterNotNull()
        }.combine(settingsUI.flowOfAccountsOrder()) { accounts, order ->
            order.mapNotNull { type ->
                accounts.find { it.type == type }
            }
        }
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
        ReloadAccountsButton()
    }
}

@Composable
private fun ReloadAccountsButton() {
    val context = context
    val accountsViewModel = accountsViewModel()

    val loadingStatus by rememberCollect {
        context.allAccountManagers
            .map { accountsViewModel.flowOfLoadingStatus(it) }
            .combine()
    }

    val anyRecordedAccount by rememberCollect {
        combine(flows = context.allAccountManagers.map { it.flowOfInfo() }) {
            it.any { userInfo -> userInfo != null }
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
            runBlocking {
                context.allAccountManagers.filter { it.getSavedInfo() == null }
            }.forEach { manager ->
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
internal fun<U: UserInfo> AccountManager<U>.ChangeSavedInfoDialog(
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
    val accountsViewModel = accountsViewModel()
    val cListAccountManager = remember { CListAccountManager(context) }
    DialogAccountChooser(
        manager = cListAccountManager,
        initialUserInfo = null,
        onDismissRequest = onDismissRequest,
        onResult = { userInfo ->
            accountsViewModel.runClistImport(
                cListUserInfo = userInfo,
                progressBarsViewModel = cpsViewModels.progressBarsViewModel,
                context = context
            )
        }
    )
}