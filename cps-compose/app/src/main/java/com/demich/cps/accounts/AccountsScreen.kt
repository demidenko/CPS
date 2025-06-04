package com.demich.cps.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.accounts.managers.AccountManager
import com.demich.cps.accounts.managers.AccountManagerType
import com.demich.cps.accounts.managers.CListAccountManager
import com.demich.cps.accounts.managers.allAccountManagers
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.bottomprogressbar.progressBarsViewModel
import com.demich.cps.ui.lazylist.itemsNotEmpty
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


@Composable
fun AccountsScreen(
    onExpandAccount: (AccountManagerType) -> Unit,
    onSetAdditionalMenu: (CPSMenuBuilder) -> Unit,
    reorderEnabled: () -> Boolean,
    enableReorder: () -> Unit
) {
    val accountsViewModel = accountsViewModel()
    val recordedAccounts by recordedAccountsState()

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
            onEmptyMessage = { Text(text = "Profiles are not defined") }
        ) { userInfoWithManager ->
            AccountPanel(
                userInfoWithManager = userInfoWithManager,
                onReloadRequest = { accountsViewModel.reload(userInfoWithManager.manager, context) },
                onExpandRequest = { onExpandAccount(userInfoWithManager.type) },
                visibleOrder = visibleOrder,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .animateItem()
            )
        }
    }

}

@Composable
private fun recordedAccountsState() = with(context) {
    collectAsState {
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

    val loadingStatus by collectAsState {
        accountsViewModel.flowOfLoadingStatus(allAccountManagers)
    }

    val anyRecordedAccount by collectAsState {
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
    val clistImportIsRunning by collectAsState { progressBarsViewModel.flowOfClistImportIsRunning() }

    val types by collectAsState {
        combine(flows = allAccountManagers.map { it.flowOfInfoWithManager(context) }) {
            val allTypes = allAccountManagers.map { it.type }
            val recordedTypes = it.mapNotNull { it?.type }
            allTypes - recordedTypes + AccountManagerType.clist
        }
    }

    Box {
        CPSIconButton(
            icon = CPSIcons.Add,
            enabled = !clistImportIsRunning,
            onClick = { showMenu = true }
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(cpsColors.backgroundAdditional)
        ) {
            types.forEach { type ->
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
            ChangeSavedInfoDialog(
                manager = allAccountManagers.first { it.type == type },
                initialUserInfo = null,
                scope = scope,
                onDismissRequest = { chosenManager = null }
            )
        }
    }
}

@Composable
internal fun <U: UserInfo> ChangeSavedInfoDialog(
    manager: AccountManager<U>,
    initialUserInfo: U?,
    scope: CoroutineScope,
    onDismissRequest: () -> Unit
) {
    val context = context
    DialogAccountChooser(
        manager = manager,
        initialUserInfo = initialUserInfo,
        onDismissRequest = onDismissRequest,
        onResult = { userInfo -> scope.launch { manager.dataStore(context).setSavedInfo(userInfo) } }
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