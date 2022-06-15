package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.contests.loaders.ContestsLoaders
import com.demich.cps.contests.loaders.ContestsReceiver
import com.demich.cps.contests.loaders.getContests
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.room.ContestsListDao
import com.demich.cps.room.contestsListDao
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.combine
import com.demich.cps.utils.edit
import kotlinx.coroutines.launch

class ContestsViewModel: ViewModel() {

    @Composable
    fun rememberLoadingStatusState(): State<LoadingStatus> = remember {
            Contest.platforms
                .map { mutableLoadingStatusFor(platform = it) }
                .combine()
        }

    @Composable
    fun getErrorsListState(): State<List<Pair<ContestsLoaders,Throwable>>>
         = remember {
            derivedStateOf {
                Contest.platforms
                    .flatMap { platform ->
                        if (mutableLoadingStatusFor(platform).value == LoadingStatus.FAILED)
                            mutableErrorsList(platform).value
                        else emptyList()
                    }
                    .distinct()
            }
        }

    private val loadingStatuses: MutableMap<Contest.Platform, MutableState<LoadingStatus>> = mutableMapOf()
    private fun mutableLoadingStatusFor(platform: Contest.Platform) =
        loadingStatuses.getOrPut(platform) { mutableStateOf(LoadingStatus.PENDING) }

    private val errors: MutableMap<Contest.Platform, MutableState<List<Pair<ContestsLoaders,Throwable>>>> = mutableMapOf()
    private fun mutableErrorsList(platform: Contest.Platform) =
        errors.getOrPut(platform) { mutableStateOf(emptyList()) }

    private suspend fun removePlatform(platform: Contest.Platform, dao: ContestsListDao) {
        dao.remove(platform)
        mutableErrorsList(platform).value = emptyList()
        mutableLoadingStatusFor(platform).value = LoadingStatus.PENDING
    }

    fun reloadEnabledPlatforms(context: Context) {
        viewModelScope.launch {
            val settings = context.settingsContests
            val enabledPlatforms = settings.enabledPlatforms()
            settings.lastReloadedPlatforms.edit { clear() }
            reload(platforms = enabledPlatforms, context = context)
        }
    }

    private suspend fun reload(platforms: Collection<Contest.Platform>, context: Context) {
        if (platforms.isEmpty()) {
            return
        }

        val settings = context.settingsContests

        settings.lastReloadedPlatforms.edit { addAll(platforms) }
        if (Contest.Platform.unknown in platforms) {
            val ids = settings.clistAdditionalResources().map { it.id }
            settings.clistLastReloadedAdditionalResources.edit { addAll(ids) }
        }

        loadContests(
            platforms = platforms,
            settings = settings,
            contestsReceiver = ContestsReceiver(
                dao = context.contestsListDao,
                getLoadingStatusState = { mutableLoadingStatusFor(it) },
                getErrorsListState = { mutableErrorsList(it) }
            )
        )
    }

    fun syncEnabledAndLastReloaded(context: Context) {
        viewModelScope.launch {
            val settings = context.settingsContests
            val enabled = settings.enabledPlatforms()
            val lastReloaded = settings.lastReloadedPlatforms()
            (lastReloaded - enabled).takeIf { it.isNotEmpty() }?.let { toRemove ->
                settings.lastReloadedPlatforms.edit { removeAll(toRemove) }
                val dao = context.contestsListDao
                toRemove.forEach { platform -> removePlatform(platform, dao) }
            }

            val toReload = (enabled - lastReloaded).toMutableSet()

            if (settings.needToReloadClistAdditional()) {
                settings.clistLastReloadedAdditionalResources.edit { clear() }
                toReload.add(Contest.Platform.unknown)
            }

            reload(platforms = toReload, context = context)
        }
    }

    private suspend fun ContestsSettingsDataStore.needToReloadClistAdditional(): Boolean {
        val enabled: Set<Int> = clistAdditionalResources().map { it.id }.toSet()
        val lastReloaded: Set<Int> = clistLastReloadedAdditionalResources()
        return enabled != lastReloaded //hope it is proper equals
    }
}

private suspend fun loadContests(
    platforms: Collection<Contest.Platform>,
    settings: ContestsSettingsDataStore,
    contestsReceiver: ContestsReceiver
) {
    if (Contest.Platform.unknown in platforms) {
        if (settings.clistAdditionalResources().isEmpty()) {
            contestsReceiver.finishSuccess(
                platform = Contest.Platform.unknown,
                contests = emptyList()
            )
            loadContests(
                platforms = platforms - Contest.Platform.unknown,
                settings = settings,
                contestsReceiver = contestsReceiver
            )
            return
        }
    }
    getContests(
        setup = settings.contestsLoadersPriorityLists().filterKeys { it in platforms },
        settings = settings,
        contestsReceiver = contestsReceiver
    )
}
