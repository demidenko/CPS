package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.room.contestsListDao
import com.demich.cps.utils.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days

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

        loadingStatus = LoadingStatus.LOADING

        kotlin.runCatching {
            val settings = context.settingsContests
            val now = getCurrentTime()

            val getClistAdditionalResourceIds = suspend {
                settings.clistAdditionalResources().map { it.id }.toSet()
            }

            val contests = CListApi.getContests(
                apiAccess = settings.clistApiAccess(),
                platforms = platforms,
                maxStartTime = now + 120.days,
                minEndTime = now - 7.days,
                includeResourceIds = getClistAdditionalResourceIds
            ).mapAndFilterResult()
                .filter { it.duration < 32.days } //TODO: setup max duration in settings

            settings.lastReloadedPlatforms.addAll(platforms)
            if (Contest.Platform.unknown in platforms) {
                settings.clistLastReloadedAdditionalResources(newValue = getClistAdditionalResourceIds())
            }

            contests.groupBy { it.platform }
        }.onSuccess { grouped ->
            context.contestsListDao.let { dao ->
                platforms.forEach { platform ->
                    dao.replace(platform = platform, contests = grouped[platform] ?: emptyList())
                }
            }
            loadingStatus = LoadingStatus.PENDING
        }.onFailure {
            loadingStatus = LoadingStatus.FAILED
            errorStateFlow.value = it
        }
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

private fun Collection<ClistContest>.mapAndFilterResult(): List<Contest> {
    return mapNotNull {
        val contest = Contest(it)
        when (contest.platform) {
            Contest.Platform.atcoder -> {
                if (it.host == "atcoder.jp")
                    contest.copy(title = contest.title.replace("（", " (").replace('）',')'))
                else null
            }
            else -> contest
        }
    }
}