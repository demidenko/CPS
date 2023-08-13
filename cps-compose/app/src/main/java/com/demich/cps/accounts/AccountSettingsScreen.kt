package com.demich.cps.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.AccountManager
import com.demich.cps.accounts.managers.AccountManagers
import com.demich.cps.accounts.managers.AccountSettingsProvider
import com.demich.cps.accounts.managers.allAccountManagers
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.ui.SettingsColumn
import com.demich.cps.ui.SettingsItem
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect

@Composable
fun AccountSettingsScreen(
    type: AccountManagers
) {
    val context = context
    val scope = rememberCoroutineScope()
    var showChangeDialog by remember { mutableStateOf(false) }

    val manager = remember(type) { context.allAccountManagers.first { it.type == type } }
    val userInfo by rememberCollect { manager.dataStore(context).flowOfInfo() }

    userInfo?.let {
        UserInfoSettings(
            manager = manager,
            userInfo = it,
            onUserIdClick = { showChangeDialog = true }
        )
    }

    if (showChangeDialog) {
        manager.ChangeSavedInfoDialog(
            scope = scope,
            onDismissRequest = { showChangeDialog = false }
        )
    }
}

@Composable
private fun UserInfoSettings(
    manager: AccountManager<out UserInfo>,
    userInfo: UserInfo,
    onUserIdClick: () -> Unit
) {
    SettingsColumn {
        UserIdSettingsItem(
            userId = userInfo.userId,
            userIdTitle = manager.userIdTitle,
            modifier = Modifier.clickable(onClick = onUserIdClick)
        )
        if (manager is AccountSettingsProvider) {
            manager.SettingsItems()
        }
    }
}

@Composable
private fun UserIdSettingsItem(
    userId: String,
    userIdTitle: String,
    modifier: Modifier = Modifier
) {
    SettingsItem(modifier = modifier) {
        Column {
            Text(
                text = "$userIdTitle:",
                color = cpsColors.contentAdditional,
                fontSize = 18.sp
            )
            Text(
                text = userId,
                fontSize = 26.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}