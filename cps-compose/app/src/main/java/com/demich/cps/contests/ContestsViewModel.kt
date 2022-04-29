package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                Contest.getPlatforms().forEach { platform ->
                    if (platform !in enabledPlatforms) remove(platform)
                }
            }
            settings.lastLoadedPlatforms(newValue = emptySet())
            reload(platforms = enabledPlatforms, context = context)
        }
    }

    private suspend fun reload(platforms: Collection<Contest.Platform>, context: Context) {
        if (platforms.isEmpty()) return

        errorStateFlow.value = null
        loadingStatus = LoadingStatus.LOADING

        kotlin.runCatching {
            val settings = context.settingsContests
            val contests = CListApi.getContests(
                apiAccess = settings.clistApiAccess(),
                platforms = platforms,
                startTime = getCurrentTime() - 7.days
            ).mapAndFilterResult()
            settings.lastLoadedPlatforms.addAll(platforms)
            contests
        }.onSuccess { contests: List<Contest> ->
            val grouped = contests.groupBy { it.platform }
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
            val lastReloaded = settings.lastLoadedPlatforms()
            val toReload = enabled.filter { it !in lastReloaded }
            val toRemove = lastReloaded.filter { it !in enabled }
            settings.lastLoadedPlatforms(newValue = lastReloaded - toRemove /* = enabled - toReload*/)
            context.contestsListDao.let { dao ->
                toRemove.forEach { platform -> dao.remove(platform) }
                reload(platforms = toReload, context = context)
            }
        }
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