package com.demich.cps.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.AccountManager
import com.demich.cps.accounts.managers.AccountManagerType
import com.demich.cps.accounts.managers.AccountSettingsProvider
import com.demich.cps.accounts.managers.accountManagerOf
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.ui.SettingsColumn
import com.demich.cps.ui.SettingsItem
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberWith
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun ProfileSettingsScreen(
    type: AccountManagerType
) {
    val manager = remember(type) { accountManagerOf(type) }
    ProfileSettingsScreen(manager = manager)
}

@Composable
private fun <U: UserInfo> ProfileSettingsScreen(
    manager: AccountManager<U>
) {
    val context = context
    val scope = rememberCoroutineScope()
    var showChangeDialog by remember { mutableStateOf(false) }

    val userInfo by collectAsState { manager.dataStore(context).flowOfInfo() }

    userInfo?.let {
        UserInfoSettings(
            manager = manager,
            userInfo = it,
            onUserIdClick = { showChangeDialog = true }
        )

        if (showChangeDialog) {
            ChangeSavedInfoDialog(
                manager = manager,
                initialUserInfo = userInfo,
                scope = scope,
                onDismissRequest = { showChangeDialog = false }
            )
        }
    }
}

@Composable
private fun <U: UserInfo> UserInfoSettings(
    manager: AccountManager<U>,
    userInfo: UserInfo,
    onUserIdClick: () -> Unit
) {
    val requiredPermission by rememberWith(context) {
        (manager as? AccountSettingsProvider)
            ?.flowOfRequiredNotificationsPermission(this)
            ?: emptyFlow()
    }.collectAsState(initial = false)

    SettingsColumn(requiredNotificationsPermission = requiredPermission) {
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