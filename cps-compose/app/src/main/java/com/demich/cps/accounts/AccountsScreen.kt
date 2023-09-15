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
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.accounts.managers.*
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.ui.*
import com.demich.cps.ui.bottomprogressbar.progressBarsViewModel
import com.demich.cps.ui.lazylist.itemsNotEmpty
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
    onExpandAccount: (AccountManagerType) -> Unit,
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

    val context = context

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
                onReloadRequest = { accountsViewModel.reload(userInfoWithManager.manager, context) },
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
            flows = allAccountManagers.map { it.flowOfInfoWithManager(this) }
        ) {
            it.filterNotNull()
        }.combine(settingsUI.accountsOrder.flow) { accounts, order ->
            order.mapNotNull { type ->
                accounts.find { it.type == type }
            }
        }
    }
}

fun accountsBottomBarBuilder(
    reorderEnabled: () -> Boolean,
    onReorderDone: () -> Unit
): AdditionalBottomBarBuilder = {
    if (reorderEnabled()) {
        CPSIconButton(
            icon = CPSIcons.ReorderDone,
            onClick = onReorderDone
        )
    } else {
        AddAccountButton()
        ReloadAccountsButton()
    }
}

@Composable
private fun ReloadAccountsButton() {
    val context = context
    val accountsViewModel = accountsViewModel()

    val loadingStatus by rememberCollect {
        allAccountManagers
            .map { accountsViewModel.flowOfLoadingStatus(it) }
            .combine()
    }

    val anyRecordedAccount by rememberCollect {
        combine(flows = allAccountManagers.map { it.dataStore(context).flowOfInfo() }) {
            it.any { userInfo -> userInfo != null }
        }
    }

    CPSReloadingButton(
        loadingStatus = loadingStatus,
        enabled = anyRecordedAccount
    ) {
        allAccountManagers.forEach { accountsViewModel.reload(it, context) }
    }
}

@Composable
private fun AddAccountMenuItem(type: AccountManagerType, onSelect: () -> Unit) {
    DropdownMenuItem(
        onClick = onSelect,
        content = {
            Text(
                text = when (type) {
                    AccountManagerType.clist -> "import from clist.by"
                    else -> type.name
                },
                style = CPSDefaults.MonospaceTextStyle
            )
        }
    )
}

@Composable
private fun AddAccountButton() {
    var showMenu by remember { mutableStateOf(false) }
    var chosenManager: AccountManagerType? by remember { mutableStateOf(null) }

    val context = context
    val scope = rememberCoroutineScope()
    val progressBarsViewModel = progressBarsViewModel()

    Box {
        CPSIconButton(
            icon = CPSIcons.Add,
            enabled = progressBarsViewModel.clistImportIsRunning.not(),
            onClick = { showMenu = true }
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(cpsColors.backgroundAdditional)
        ) {
            remember {
                runBlocking {
                    allAccountManagers.filter { it.dataStore(context).getSavedInfo() == null }
                }.map { it.type }.plus(AccountManagerType.clist)
            }.forEach { type ->
                AddAccountMenuItem(type = type) {
                    showMenu = false
                    chosenManager = type
                }
            }
        }
    }

    chosenManager?.let { type ->
        if (type == AccountManagerType.clist) {
            CListImportDialog { chosenManager = null }
        } else {
            allAccountManagers.first { it.type == type }
                .ChangeSavedInfoDialog(
                    scope = scope,
                    onDismissRequest = { chosenManager = null }
                )
        }
    }
}

@Composable
internal fun<U: UserInfo> AccountManager<U>.ChangeSavedInfoDialog(
    scope: CoroutineScope,
    onDismissRequest: () -> Unit
) {
    val dataStore = dataStore(context)
    DialogAccountChooser(
        manager = this,
        initialUserInfo = runBlocking { dataStore.getSavedInfo() },
        onDismissRequest = onDismissRequest,
        onResult = { userInfo -> scope.launch { dataStore.setSavedInfo(userInfo) } }
    )
}

@Composable
private fun CListImportDialog(
    onDismissRequest: () -> Unit
) {
    val context = context
    val accountsViewModel = accountsViewModel()
    val progressBarsViewModel = progressBarsViewModel()
    val cListAccountManager = remember { CListAccountManager() }
    DialogAccountChooser(
        manager = cListAccountManager,
        initialUserInfo = null,
        onDismissRequest = onDismissRequest,
        onResult = { userInfo ->
            accountsViewModel.runClistImport(
                cListUserInfo = userInfo,
                progressBarsViewModel = progressBarsViewModel,
                context = context
            )
        }
    )
}