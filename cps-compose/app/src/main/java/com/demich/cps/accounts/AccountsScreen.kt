package com.demich.cps.accounts

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.demich.cps.R
import com.demich.cps.Screen
import com.demich.cps.accounts.managers.*
import com.demich.cps.ui.CPSDropdownMenuScope
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


@Composable
fun AccountsScreen(navController: NavController, accountsViewModel: AccountsViewModel) {
    val context = context

    val accountsArray by rememberCollect {
        combine(
            flows = context.allAccountManagers
                .map { it.flowOfInfoWithManager() }
        ) { it }
    }

    val recordedAccounts by remember {
        derivedStateOf { accountsArray.filterNot { it.userInfo.isEmpty() } }
    }

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .background(cpsColors.background)
    ) {
        if (recordedAccounts.isEmpty()) {
            MonospacedText(
                text = stringResource(id = R.string.accounts_welcome),
                color = cpsColors.textColorAdditional,
                fontSize = 18.sp,
                modifier = Modifier.padding(6.dp)
            )
        } else {
            recordedAccounts.forEach {
                key(it.type) {
                    PanelWithUI(
                        userInfoWithManager = it,
                        accountsViewModel = accountsViewModel,
                        modifier = Modifier.padding(start = 10.dp, top = 10.dp)
                    ) {
                        navController.navigate(route = "account/${it.type}")
                    }
                }
            }
        }
    }
}

@Composable
fun AccountExpandedScreen(
    type: AccountManagers,
    navController: NavController
) {
    val context = context
    val manager = remember(type) { context.allAccountManagers.first { it.type == type } }
    AccountExpandedScreen(manager = manager)
}

@Composable
private fun<U: UserInfo> AccountExpandedScreen(
    manager: AccountManager<U>
) {
    val userInfo by rememberCollect { manager.flowOfInfo() }
    Box(modifier = Modifier
        .padding(all = 10.dp)
        .fillMaxSize()
    ) {
        manager.BigView(userInfo)
    }
}

@Composable
fun AccountsBottomBar(accountsViewModel: AccountsViewModel) {
    AddAccountButton(accountsViewModel)
    ReloadAccountsButton(accountsViewModel)
}

@Composable
fun CPSDropdownMenuScope.BuildAccountsMenu(currentScreen: Screen) {
    if (currentScreen is Screen.AccountExpanded) {
        Divider(color = cpsColors.dividerColor)
        CPSDropdownMenuItem(title = "Delete", icon = Icons.Default.DeleteForever) {
            //TODO: Delete userInfo
        }
        CPSDropdownMenuItem(title = "Settings", icon = Icons.Default.Settings) {
            //TODO: Open Settings
        }
        CPSDropdownMenuItem(title = "Origin", icon = Icons.Default.Photo) {
            //TODO: Open origin page
        }
    }
}

@Composable
private fun ReloadAccountsButton(accountsViewModel: AccountsViewModel) {
    val context = context
    val combinedStatus by remember {
        derivedStateOf {
            val states = context.allAccountManagers.map { accountsViewModel.loadingStatusFor(it).value }
            when {
                states.contains(LoadingStatus.LOADING) -> LoadingStatus.LOADING
                states.contains(LoadingStatus.FAILED) -> LoadingStatus.FAILED
                else -> LoadingStatus.PENDING
            }
        }
    }

    CPSReloadingButton(loadingStatus = combinedStatus) {
        context.allAccountManagers.forEach { accountsViewModel.reload(it) }
    }
}

@Composable
private fun AddAccountButton(accountsViewModel: AccountsViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    var chosenManager by remember { mutableStateOf<AccountManager<*>?>(null) }

    Box {
        CPSIconButton(icon = Icons.Outlined.AddBox) {
            showMenu = true
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(cpsColors.backgroundAdditional)
        ) {
            context.allAccountManagers.forEach { manager ->
                DropdownMenuItem(
                    onClick = {
                        showMenu = false
                        chosenManager = manager
                    }
                ) {
                    CPSIconButton(icon = Icons.Default.Delete) {
                        accountsViewModel.delete(manager)
                    }
                    MonospacedText(text = manager.type.name)
                }
            }
        }
        chosenManager?.ChangeSavedInfoDialog {
            chosenManager = null
        }
    }
}

@Composable
fun<U: UserInfo> AccountManager<U>.ChangeSavedInfoDialog(
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

val Context.allAccountManagers: List<AccountManager<*>>
    get() = listOf(
        CodeforcesAccountManager(this),
        AtCoderAccountManager(this),
        CodeChefAccountManager(this),
        DmojAccountManager(this),
        ACMPAccountManager(this),
        TimusAccountManager(this)
    )
