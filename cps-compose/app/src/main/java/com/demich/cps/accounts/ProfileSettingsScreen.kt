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
import com.demich.cps.accounts.managers.ProfileSettingsProvider
import com.demich.cps.accounts.managers.accountManagerOf
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.ui.settings.Item
import com.demich.cps.ui.settings.SettingsColumn
import com.demich.cps.ui.settings.SettingsContainerScope
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberFrom
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

    val profileResult by collectItemAsState { manager.dataStore(context).profile }

    profileResult?.let {
        ProfileSettingsItems(
            manager = manager,
            profileResult = it,
            onUserIdClick = { showChangeDialog = true }
        )

        if (showChangeDialog) {
            ChangeSavedProfileDialog(
                manager = manager,
                initial = it,
                scope = scope,
                onDismissRequest = { showChangeDialog = false }
            )
        }
    }
}

@Composable
private fun <U: UserInfo> ProfileSettingsItems(
    manager: AccountManager<U>,
    profileResult: ProfileResult<U>,
    onUserIdClick: () -> Unit
) {
    val requiredPermission by rememberFrom(context) {
        (manager as? ProfileSettingsProvider)
            ?.flowOfRequiredNotificationsPermission(it)
            ?: emptyFlow()
    }.collectAsState(initial = false)

    SettingsColumn(requiredNotificationsPermission = requiredPermission) {
        UserIdSettingsItem(
            userId = profileResult.userId,
            userIdTitle = manager.userIdTitle,
            modifier = Modifier.clickable(onClick = onUserIdClick)
        )
        if (manager is ProfileSettingsProvider) {
            manager.SettingsItems()
        }
    }
}

@Composable
private fun SettingsContainerScope.UserIdSettingsItem(
    userId: String,
    userIdTitle: String,
    modifier: Modifier = Modifier
) {
    Item(modifier = modifier) {
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