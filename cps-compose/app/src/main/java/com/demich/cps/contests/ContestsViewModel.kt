package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.contests.loaders.ContestsLoaders
import com.demich.cps.contests.loaders.getContests
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.room.contestsListDao
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.addAll
import com.demich.cps.utils.getCurrentTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContestsViewModel: ViewModel() {
    var loadingStatus by mutableStateOf(LoadingStatus.PENDING)
        private set

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
        require(loadingStatus != LoadingStatus.LOADING) //TODO consider cases and solutions

        errorStateFlow.value = null

        if (platforms.isEmpty()) {
            loadingStatus = LoadingStatus.PENDING
            return
        }

        val settings = context.settingsContests
        if (Contest.Platform.unknown in platforms) {
            if (settings.clistAdditionalResources().isEmpty()) {
                return reload(
                    platforms = platforms - Contest.Platform.unknown,
                    context = context
                )
            }
        }

        loadingStatus = LoadingStatus.LOADING

        val resultsGrouped = loadContests(
            platforms = platforms,
            settings = settings
        )

        settings.lastReloadedPlatforms.addAll(platforms)
        if (Contest.Platform.unknown in platforms) {
            settings.clistLastReloadedAdditionalResources.addAll(
                values = settings.clistAdditionalResources().map { it.id }
            )
        }

        val dao = context.contestsListDao
        var anyThrowable: Throwable? = null
        platforms.forEach { platform ->
            resultsGrouped.getValue(platform).last().onFailure {
                anyThrowable = it
            }.onSuccess { contests ->
                dao.replace(
                    platform = platform,
                    contests = contests
                )
            }
        }

        loadingStatus = anyThrowable?.let {
            errorStateFlow.value = it
            LoadingStatus.FAILED
        } ?: LoadingStatus.PENDING
    }

    private suspend fun loadContests(
        platforms: Collection<Contest.Platform>,
        settings: ContestsSettingsDataStore
    ): Map<Contest.Platform, List<Result<List<Contest>>>> {
        val timeLimits = settings.contestsTimePrefs().createLimits(now = getCurrentTime())
        return getContests(
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
            timeLimits = timeLimits,
            settings = settings
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
