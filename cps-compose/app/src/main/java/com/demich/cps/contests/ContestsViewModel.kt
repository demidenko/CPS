package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.contests.loaders.ContestsLoaders
import com.demich.cps.contests.loaders.getContests
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.room.ContestsListDao
import com.demich.cps.room.contestsListDao
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.addAll
import com.demich.cps.utils.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val loadingStatuses: MutableMap<Contest.Platform, MutableState<LoadingStatus>> = mutableMapOf()

    private fun mutableLoadingStatusFor(platform: Contest.Platform): MutableState<LoadingStatus> =
        loadingStatuses.getOrPut(platform) { mutableStateOf(LoadingStatus.PENDING) }


    private val errorStateFlow = MutableStateFlow<Throwable?>(null)
    fun flowOfError() = errorStateFlow.asStateFlow()

    fun reloadEnabledPlatforms(context: Context) {
        viewModelScope.launch {
            val settings = context.settingsContests
            val enabledPlatforms = settings.enabledPlatforms()
            context.contestsListDao.run {
                Contest.platforms.forEach { platform ->
                    if (platform !in enabledPlatforms) remove(platform)
                }
            }
            settings.lastReloadedPlatforms(newValue = emptySet())
            reload(platforms = enabledPlatforms, context = context)
        }
    }

    private suspend fun reload(platforms: Collection<Contest.Platform>, context: Context) {
        errorStateFlow.value = null //TODO: consume errors

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

        println("reload ${platforms.joinToString()}")
        loadContests(
            platforms = platforms,
            settings = settings,
            contestsReceiver = ContestsReceiver(
                dao = context.contestsListDao,
                getLoadingStatusState = { mutableLoadingStatusFor(it) },
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
                context.contestsListDao.let { dao ->
                    toRemove.forEach { platform -> dao.remove(platform) }
                }
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

class ContestsReceiver(
    private val dao: ContestsListDao,
    private val getLoadingStatusState: (Contest.Platform) -> MutableState<LoadingStatus>,
) {
    fun startLoading(platform: Contest.Platform) {
        var loadingStatus by getLoadingStatusState(platform)
        require(loadingStatus != LoadingStatus.LOADING)
        loadingStatus = LoadingStatus.LOADING
    }

    fun consumeError(platform: Contest.Platform, loaderType: ContestsLoaders, e: Throwable) {

    }

    suspend fun finishSuccess(platform: Contest.Platform, contests: List<Contest>) {
        getLoadingStatusState(platform).value = LoadingStatus.PENDING
        dao.replace(platform = platform, contests = contests)
    }

    suspend fun finishFailed(platform: Contest.Platform) {
        getLoadingStatusState(platform).value = LoadingStatus.FAILED
    }
}