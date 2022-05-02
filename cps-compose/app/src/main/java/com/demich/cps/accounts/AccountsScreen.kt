package com.demich.cps.accounts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.demich.cps.*
import com.demich.cps.R
import com.demich.cps.accounts.managers.*
import com.demich.cps.ui.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountsScreen(
    accountsViewModel: AccountsViewModel,
    onExpandAccount: (AccountManagers) -> Unit,
    onSetAdditionalMenu: (CPSMenuBuilder) -> Unit
) {
    val context = context

    val recordedAccounts by rememberCollect {
        combine(
            flows = context.allAccountManagers
                .map { it.flowOfInfoWithManager() }
        ) { it }.combine(context.settingsUI.flowOfAccountsOrder()) { accountsArray, order ->
            order.mapNotNull { type ->
                accountsArray.find { it.type == type }?.takeIf { !it.userInfo.isEmpty() }
            }
        }
    }

    var showAccountsReorderUI by rememberSaveable { mutableStateOf(false) }
    val visibleOrder by remember {
        derivedStateOf { if (showAccountsReorderUI) recordedAccounts.map { it.type } else null }
    }
    if (recordedAccounts.size > 1) {
        onSetAdditionalMenu {
            CPSDropdownMenuItem(title = "Reorder", icon = CPSIcons.Reorder) {
                showAccountsReorderUI = true
            }
        }
    }


    Box(modifier = Modifier.fillMaxHeight()) {
        LazyColumn(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .background(cpsColors.background)
        ) {
            if (recordedAccounts.isEmpty()) item {
                MonospacedText(
                    text = stringResource(id = R.string.accounts_welcome),
                    color = cpsColors.textColorAdditional,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(6.dp)
                )
            } else {
                items(recordedAccounts, key = { it.type }) { userInfoWithManager ->
                    PanelWithUI(
                        userInfoWithManager = userInfoWithManager,
                        accountsViewModel = accountsViewModel,
                        visibleOrder = visibleOrder,
                        modifier = Modifier
                            .padding(start = 10.dp, top = 10.dp)
                            .animateItemPlacement(),
                        onExpandRequest = { onExpandAccount(userInfoWithManager.type) }
                    )
                }
            }
        }
        if (showAccountsReorderUI) {
            //TODO: this button instead of whole bottom bar
            Button(
                onClick = { showAccountsReorderUI = false },
                content = { Text("Done") },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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
    AccountExpandedPanel(
        manager = manager,
        setBottomBarContent = setBottomBarContent
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            confirmButton = {
                TextButton(
                    content = { Text(text = "Delete", color = cpsColors.errorColor) },
                    onClick = {
                        onDeleteRequest(manager)
                        onDismissDeleteDialog()
                    }
                )
            },
            dismissButton = {
                TextButton(
                    content = { Text("Cancel") },
                    onClick = onDismissDeleteDialog
                )
            },
            title = {
                Text("Delete $type account?")
            },
            backgroundColor = cpsColors.background
        )
    }
}

@Composable
private fun<U: UserInfo> AccountExpandedPanel(
    manager: AccountManager<U>,
    setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit
) {
    val userInfo by rememberCollect { manager.flowOfInfo() }
    manager.BigView(
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
                    color = cpsColors.textColorAdditional,
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
            manager.Settings()
        }
    }

    if (showChangeDialog) {
        manager.ChangeSavedInfoDialog { showChangeDialog = false }
    }
}

fun accountExpandedMenuBuilder(
    type: AccountManagers,
    navController: NavController,
    onShowDeleteDialog: () -> Unit
): CPSMenuBuilder = {
    val context = context
    CPSDropdownMenuItem(title = "Delete", icon = CPSIcons.Delete, onClick = onShowDeleteDialog)
    CPSDropdownMenuItem(title = "Settings", icon = CPSIcons.Settings) {
        navController.navigate(Screen.AccountSettings.route(type))
    }
    CPSDropdownMenuItem(title = "Origin", icon = CPSIcons.Origin) {
        val url = context.allAccountManagers.first { it.type == type }.run {
            runBlocking { getSavedInfo().link() }
        }
        context.openUrlInBrowser(url)
    }
}

fun accountsBottomBarBuilder(cpsViewModels: CPSViewModels)
: AdditionalBottomBarBuilder = {
    AddAccountButton(cpsViewModels)
    ReloadAccountsButton(cpsViewModels.accountsViewModel)
}

@Composable
private fun ReloadAccountsButton(accountsViewModel: AccountsViewModel) {
    val context = context
    val combinedStatus by remember {
        derivedStateOf {
            context.allAccountManagers
                .map { accountsViewModel.loadingStatusFor(it).value }
                .combine()
        }
    }

    CPSReloadingButton(loadingStatus = combinedStatus) {
        context.allAccountManagers.forEach { accountsViewModel.reload(it) }
    }
}

@Composable
private fun AddAccountButton(cpsViewModels: CPSViewModels) {
    var showMenu by remember { mutableStateOf(false) }
    var chosenManager: AccountManagers? by remember { mutableStateOf(null) }

    val context = context

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
                    .ChangeSavedInfoDialog { chosenManager = null }
            }
        }
    }
}

@Composable
private fun<U: UserInfo> AccountManager<U>.ChangeSavedInfoDialog(
    onDismissRequest: () -> Unit
) {
    val scope = rememberCoroutineScope()
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