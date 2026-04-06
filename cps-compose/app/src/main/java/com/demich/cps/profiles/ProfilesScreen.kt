package com.demich.cps.profiles

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenStaticTitleState
import com.demich.cps.platforms.Platform
import com.demich.cps.profiles.managers.CListProfileManager
import com.demich.cps.profiles.managers.ProfileManager
import com.demich.cps.profiles.managers.ProfileResultWithManager
import com.demich.cps.profiles.managers.flowOfExisted
import com.demich.cps.profiles.managers.platform
import com.demich.cps.profiles.managers.profileManagerOf
import com.demich.cps.profiles.managers.profilePlatforms
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.UserInfo
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
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
private fun ProfilesScreen(
    profiles: List<ProfileResultWithManager<*>>,
    onExpandProfile: (Platform) -> Unit,
    reorderEnabled: Boolean,
) {
    val context = context
    val viewModel = profilesViewModel()

    val profilePlatforms = if (reorderEnabled) profiles.map { it.platform } else null

    LazyColumn(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 10.dp)
    ) {
        itemsNotEmpty(
            items = profiles,
            key = { it.platform },
            onEmptyMessage = { Text(text = "Profiles are not defined") }
        ) { profileResultWithManager ->
            ProfilePanel(
                profileResultWithManager = profileResultWithManager,
                onReloadRequest = { viewModel.reload(profileResultWithManager.manager, context) },
                onExpandRequest = { onExpandProfile(profileResultWithManager.platform) },
                visibleOrder = profilePlatforms,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .animateItem()
            )
        }
    }
}

@Composable
fun CPSNavigator.ScreenScope<Screen.Profiles>.NavContentProfilesScreen(
    onExpandProfile: (Platform) -> Unit
) {
    var reorderEnabled by rememberSaveable { mutableStateOf(false) }
    val profilesOrder by profilesOrderState()

    screenTitle = ScreenStaticTitleState("profiles")

    menu =
        if (profilesOrder.size > 1) {
            {
                CPSDropdownMenuItem(
                    title = "Reorder",
                    icon = CPSIcons.Reorder,
                    onClick = { reorderEnabled = true }
                )
            }
        } else {
            null
        }

    ProfilesScreen(
        profiles = profilesOrder,
        onExpandProfile = onExpandProfile,
        reorderEnabled = reorderEnabled
    )

    bottomBar = profilesBottomBarBuilder(
        profiles = profilesOrder,
        reorderEnabled = reorderEnabled,
        onReorderDone = { reorderEnabled = false }
    )
}

@Composable
private fun profilesOrderState() = with(context) {
    collectAsState {
        combine(
            flow = ProfileManager.entries().flowOfExisted(this),
            flow2 = settingsUI.profilesOrder.asFlow()
        ) { profiles, order ->
            order.mapNotNull { platform ->
                profiles.find { it.platform == platform }
            }
        }
    }
}

private fun profilesBottomBarBuilder(
    profiles: List<ProfileResultWithManager<*>>,
    reorderEnabled: Boolean,
    onReorderDone: () -> Unit
): AdditionalBottomBarBuilder = {
    if (reorderEnabled) {
        CPSIconButton(
            icon = CPSIcons.ReorderDone,
            onClick = onReorderDone
        )
    } else {
        AddProfileButton(availableProfiles = profiles)
        ReloadProfilesButton(profiles = profiles)
    }
}

@Composable
private fun ReloadProfilesButton(
    profiles: List<ProfileResultWithManager<*>>
) {
    val context = context
    val viewModel = profilesViewModel()

    val loadingStatus by collectAsState {
        viewModel.flowOfLoadingStatus(ProfileManager.entries())
    }

    CPSReloadingButton(
        loadingStatus = loadingStatus,
        enabled = profiles.isNotEmpty(),
        onClick = {
            ProfileManager.entries().forEach { viewModel.reload(it, context) }
        }
    )
}

@Composable
private fun AddProfileMenuItem(platform: Platform, onSelect: () -> Unit) {
    DropdownMenuItem(
        onClick = onSelect,
        content = {
            Text(
                text = when (platform) {
                    clist -> "import from clist.by"
                    else -> platform.name
                },
                style = CPSDefaults.MonospaceTextStyle
            )
        }
    )
}

@Composable
private fun AddProfileButton(
    availableProfiles: List<ProfileResultWithManager<*>>
) {
    var showMenu by remember { mutableStateOf(false) }
    var selectedPlatform: Platform? by remember { mutableStateOf(null) }

    val scope = rememberCoroutineScope()
    val progressBarsViewModel = progressBarsViewModel()
    val clistImportIsRunning by collectAsState { progressBarsViewModel.flowOfClistImportIsRunning() }

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
            val platforms: List<Platform> = remember(availableProfiles) {
                val all = profilePlatforms
                val available = availableProfiles.map { it.platform }
                all - available + Platform.clist
            }

            platforms.forEach { platform ->
                AddProfileMenuItem(platform = platform) {
                    showMenu = false
                    selectedPlatform = platform
                }
            }
        }
    }

    selectedPlatform?.let { platform ->
        if (platform == clist) {
            CListImportDialog(
                onDismissRequest = { selectedPlatform = null }
            )
        } else {
            ChangeSavedProfileDialog(
                manager = profileManagerOf(platform),
                initial = null,
                scope = scope,
                onDismissRequest = { selectedPlatform = null }
            )
        }
    }
}

@Composable
internal fun <U: UserInfo> ChangeSavedProfileDialog(
    manager: ProfileManager<U>,
    initial: ProfileResult<U>?,
    scope: CoroutineScope,
    onDismissRequest: () -> Unit
) {
    val context = context
    DialogProfileSelector(
        manager = manager,
        initial = initial,
        onDismissRequest = onDismissRequest,
        onResult = { scope.launch { manager.profileStorage(context).setProfile(it) } }
    )
}

@Composable
private fun CListImportDialog(
    onDismissRequest: () -> Unit
) {
    val context = context
    val profilesViewModel = profilesViewModel()
    val progressBarsViewModel = progressBarsViewModel()
    DialogProfileSelector(
        manager = CListProfileManager(),
        initial = null,
        onDismissRequest = onDismissRequest,
        onResult = {
            if (it is ProfileResult.Success) {
                profilesViewModel.runClistImport(
                    cListUserInfo = it.userInfo,
                    progressBarsViewModel = progressBarsViewModel,
                    context = context
                )
            }
        }
    )
}