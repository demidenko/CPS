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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.accounts.managers.AccountManager
import com.demich.cps.accounts.managers.AccountManagerType
import com.demich.cps.accounts.managers.CListAccountManager
import com.demich.cps.accounts.managers.ProfileResultWithManager
import com.demich.cps.accounts.managers.accountManagerOf
import com.demich.cps.accounts.managers.allAccountManagers
import com.demich.cps.accounts.managers.flowWithProfileResult
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
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
    profiles: List<ProfileResultWithManager<out UserInfo>>,
    onExpandProfile: (AccountManagerType) -> Unit,
    reorderEnabled: Boolean,
) {
    val context = context
    val viewModel = profilesViewModel()

    val profilesTypes = if (reorderEnabled) profiles.map { it.type } else null

    LazyColumn(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 10.dp)
    ) {
        itemsNotEmpty(
            items = profiles,
            key = { it.type },
            onEmptyMessage = { Text(text = "Profiles are not defined") }
        ) { profileResultWithManager ->
            ProfilePanel(
                profileResultWithManager = profileResultWithManager,
                onReloadRequest = { viewModel.reload(profileResultWithManager.manager, context) },
                onExpandRequest = { onExpandProfile(profileResultWithManager.type) },
                visibleOrder = profilesTypes,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .animateItem()
            )
        }
    }
}

@Composable
fun NavContentProfilesScreen(
    holder: CPSNavigator.DuringCompositionHolder<Screen.Profiles>,
    onExpandProfile: (AccountManagerType) -> Unit
) {
    var reorderEnabled by rememberSaveable { mutableStateOf(false) }
    val profilesOrder by profilesOrderState()
    
    ProfilesScreen(
        profiles = profilesOrder,
        onExpandProfile = onExpandProfile,
        reorderEnabled = reorderEnabled
    )
    
    holder.menu =
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

    holder.bottomBar = profilesBottomBarBuilder(
        profiles = profilesOrder,
        reorderEnabled = reorderEnabled,
        onReorderDone = { reorderEnabled = false }
    )
    
    holder.setSubtitle("profiles")
}

@Composable
private fun profilesOrderState() = with(context) {
    collectAsState {
        combine(
            flows = allAccountManagers.map { it.flowWithProfileResult(this) }
        ) {
            it.filterNotNull()
        }.combine(settingsUI.profilesOrder.asFlow()) { profiles, order ->
            order.mapNotNull { type ->
                profiles.find { it.type == type }
            }
        }
    }
}

private fun profilesBottomBarBuilder(
    profiles: List<ProfileResultWithManager<out UserInfo>>,
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
    profiles: List<ProfileResultWithManager<out UserInfo>>
) {
    val context = context
    val viewModel = profilesViewModel()

    val loadingStatus by collectAsState {
        viewModel.flowOfLoadingStatus(allAccountManagers)
    }

    CPSReloadingButton(
        loadingStatus = loadingStatus,
        enabled = profiles.isNotEmpty(),
        onClick = {
            allAccountManagers.forEach { viewModel.reload(it, context) }
        }
    )
}

@Composable
private fun AddProfileMenuItem(type: AccountManagerType, onSelect: () -> Unit) {
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
private fun AddProfileButton(
    availableProfiles: List<ProfileResultWithManager<out UserInfo>>
) {
    var showMenu by remember { mutableStateOf(false) }
    var selectedType: AccountManagerType? by remember { mutableStateOf(null) }

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
            val types = remember(availableProfiles) {
                val allTypes = allAccountManagers.map { it.type }
                val availableTypes = availableProfiles.map { it.type }
                allTypes - availableTypes + AccountManagerType.clist
            }

            types.forEach { type ->
                AddProfileMenuItem(type = type) {
                    showMenu = false
                    selectedType = type
                }
            }
        }
    }

    selectedType?.let { type ->
        if (type == AccountManagerType.clist) {
            CListImportDialog(
                onDismissRequest = { selectedType = null }
            )
        } else {
            ChangeSavedProfileDialog(
                manager = accountManagerOf(type),
                initial = null,
                scope = scope,
                onDismissRequest = { selectedType = null }
            )
        }
    }
}

@Composable
internal fun <U: UserInfo> ChangeSavedProfileDialog(
    manager: AccountManager<U>,
    initial: ProfileResult<U>?,
    scope: CoroutineScope,
    onDismissRequest: () -> Unit
) {
    val context = context
    DialogAccountChooser(
        manager = manager,
        initial = initial,
        onDismissRequest = onDismissRequest,
        onResult = { scope.launch { manager.dataStore(context).setProfile(it) } }
    )
}

@Composable
private fun CListImportDialog(
    onDismissRequest: () -> Unit
) {
    val context = context
    val profilesViewModel = profilesViewModel()
    val progressBarsViewModel = progressBarsViewModel()
    val cListAccountManager = remember { CListAccountManager() }
    DialogAccountChooser(
        manager = cListAccountManager,
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