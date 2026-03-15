package com.demich.cps.profiles

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenStaticTitleState
import com.demich.cps.profiles.managers.ProfileManager
import com.demich.cps.profiles.managers.ProfilePlatform
import com.demich.cps.profiles.managers.profileManagerOf
import com.demich.cps.profiles.userinfo.UserInfo
import com.demich.cps.profiles.userinfo.userInfoOrNull
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.dialogs.CPSDeleteDialog
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.openUrlInBrowser
import kotlinx.coroutines.launch

@Composable
fun CPSNavigator.ScreenScope<Screen.ProfileExpanded>.NavContentProfilesExpandedScreen(
    onOpenSettings: () -> Unit,
    navigateBack: () -> Unit
) {
    val platform = screen.platform
    val manager = remember(platform) { profileManagerOf(platform) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    screenTitle = ScreenStaticTitleState("profiles", platform.name)

    menu = profileExpandedMenuBuilder(
        platform = platform,
        onShowDeleteDialog = { showDeleteDialog = true },
        onOpenSettings = onOpenSettings
    )

    ProfileExpandedContent(
        manager = manager,
        setBottomBarContent = { bottomBar = it }
    )

    if (showDeleteDialog) {
        val context = context
        val viewModel = profilesViewModel()

        CPSDeleteDialog(
            title = "Delete $platform profile?",
            onConfirmRequest = {
                viewModel.delete(manager, context)
                navigateBack()
            },
            onDismissRequest = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun <U: UserInfo> ProfileExpandedContent(
    manager: ProfileManager<U>,
    setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit
) {
    val context = context
    val profileResult by collectItemAsState { manager.profileStorage(context).profile }
    profileResult?.let {
        manager.ExpandedContent(
            profileResult = it,
            setBottomBarContent = setBottomBarContent,
            modifier = Modifier
                .padding(all = 10.dp)
                .fillMaxSize()
        )
    }
}

private fun profileExpandedMenuBuilder(
    platform: ProfilePlatform,
    onOpenSettings: () -> Unit,
    onShowDeleteDialog: () -> Unit
): CPSMenuBuilder = {
    val context = context
    val scope = rememberCoroutineScope()

    CPSDropdownMenuItem(title = "Delete", icon = CPSIcons.Delete, onClick = onShowDeleteDialog)
    CPSDropdownMenuItem(title = "Settings", icon = CPSIcons.Settings, onClick = onOpenSettings)
    CPSDropdownMenuItem(title = "Origin", icon = CPSIcons.Origin) {
        scope.launch {
            profileManagerOf(platform)
                .profileStorage(context).profile()
                ?.userInfoOrNull()
                ?.userPageUrl
                ?.let { url -> context.openUrlInBrowser(url) }
        }
    }
}