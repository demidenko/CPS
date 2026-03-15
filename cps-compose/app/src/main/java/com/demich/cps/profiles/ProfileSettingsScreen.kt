package com.demich.cps.profiles

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
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenStaticTitleState
import com.demich.cps.profiles.managers.ProfileManager
import com.demich.cps.profiles.managers.ProfilePlatform
import com.demich.cps.profiles.managers.ProfileSettingsProvider
import com.demich.cps.profiles.managers.profileManagerOf
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.UserInfo
import com.demich.cps.ui.settings.Item
import com.demich.cps.ui.settings.SettingsColumn
import com.demich.cps.ui.settings.SettingsContainerScope
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import kotlinx.coroutines.flow.emptyFlow

@Composable
private fun ProfileSettingsScreen(
    platform: ProfilePlatform
) {
    val manager = remember(platform) { profileManagerOf(platform) }
    ProfileSettingsScreen(manager = manager)
}

@Composable
fun CPSNavigator.ScreenScope<Screen.ProfileSettings>.NavContentProfilesSettingsScreen() {
    val platform = screen.platform
    
    screenTitle = ScreenStaticTitleState("profiles", platform.name, "settings")
    
    ProfileSettingsScreen(platform)
}

@Composable
private fun <U: UserInfo> ProfileSettingsScreen(
    manager: ProfileManager<U>
) {
    val context = context
    val scope = rememberCoroutineScope()
    var showChangeDialog by remember { mutableStateOf(false) }

    val profileResult by collectItemAsState { manager.profileStorage(context).profile }

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
    manager: ProfileManager<U>,
    profileResult: ProfileResult<U>,
    onUserIdClick: () -> Unit
) {
    val context = context
    val requiredPermission by remember(manager) {
        (manager as? ProfileSettingsProvider)
            ?.flowOfRequiredNotificationsPermission(context)
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
context(scope: SettingsContainerScope)
private fun UserIdSettingsItem(
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