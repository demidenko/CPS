package com.demich.cps.accounts

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.demich.cps.R
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


@Composable
fun AccountsScreen(navController: NavController) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .background(cpsColors.background)
    ) {
        MonospacedText(
            text = stringResource(id = R.string.accounts_welcome),
            color = cpsColors.textColorAdditional,
            fontSize = 18.sp,
            modifier = Modifier.padding(6.dp)
        )
    }
}

@Composable
fun AccountsBottomBar() {
    AddAccountButton()
    ReloadAccountsButton()
}

@Composable
private fun ReloadAccountsButton() {
    CPSIconButton(icon = Icons.Default.Refresh) {
        //TODO Reload Accounts
    }
}

@Composable
private fun AddAccountButton() {
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
                    MonospacedText(text = manager.managerName)
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

private val Context.allAccountManagers: List<AccountManager<*>>
    get() = listOf(
        CodeforcesAccountManager(this),
        AtCoderAccountManager(this),
        ACMPAccountManager(this)
    )
