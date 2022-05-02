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
import com.demich.cps.utils.addAll
import com.demich.cps.utils.combine
import kotlinx.coroutines.launch

class ContestsViewModel: ViewModel() {

    val loadingStatus: State<LoadingStatus>
        @Composable
        get() = remember {
            derivedStateOf {
                Contest.platforms
                    .map { mutableLoadingStatusFor(platform = it).value }
                    .combine()
            }
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
            settings.lastReloadedPlatforms(newValue = emptySet())
            reload(platforms = enabledPlatforms, context = context)
        }
    }

    private suspend fun reload(platforms: Collection<Contest.Platform>, context: Context) {
        if (platforms.isEmpty()) {
            return
        }

        val settings = context.settingsContests

        settings.lastReloadedPlatforms.addAll(platforms)
        if (Contest.Platform.unknown in platforms) {
            settings.clistLastReloadedAdditionalResources.addAll(
                values = settings.clistAdditionalResources().map { it.id }
            )
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
                settings.lastReloadedPlatforms(newValue = lastReloaded - toRemove)
                val dao = context.contestsListDao
                toRemove.forEach { platform -> removePlatform(platform, dao) }
            }

            val toReload = (enabled - lastReloaded).toMutableSet()

            if (needToReloadClistAdditional(settings)) {
                settings.clistLastReloadedAdditionalResources(newValue = emptySet())
                toReload.add(Contest.Platform.unknown)
            }

            reload(platforms = toReload, context = context)
        }
    }

    private suspend fun needToReloadClistAdditional(settings: ContestsSettingsDataStore): Boolean {
        val enabled = settings.clistAdditionalResources().map { it.id }.toSet()
        val lastReloaded = settings.clistLastReloadedAdditionalResources()
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
        //TODO: read setup from settings
        setup = platforms.associateWith { platform ->
            when (platform) {
                Contest.Platform.codeforces -> listOf(
                    ContestsLoaders.codeforces,
                    ContestsLoaders.clist
                )
                else -> listOf(ContestsLoaders.clist)
            }
        },
        settings = settings,
        contestsReceiver = contestsReceiver
    )
}
