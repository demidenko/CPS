package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.demich.cps.contests.database.ContestPlatform
import com.demich.cps.contests.database.contestsRepository
import com.demich.cps.contests.database.toContestPlatform
import com.demich.cps.contests.fetching.ContestsFetchResult
import com.demich.cps.contests.fetching.ContestsFetchSource
import com.demich.cps.contests.loading_engine.collectTo
import com.demich.cps.contests.settings.ContestsFetchSettingsSnapshotDiff
import com.demich.cps.contests.settings.differenceFrom
import com.demich.cps.contests.settings.fetchSettingsSnapshot
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.combineLoadingStatus
import com.demich.cps.utils.edit
import com.demich.cps.utils.sharedViewModel
import com.demich.cps.utils.uniqueLaunch
import com.demich.cps.workers.ContestsWorker
import com.demich.kotlin_stdlib_boost.emptyEnumSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex

@Composable
fun contestsViewModel(): ContestsViewModel = sharedViewModel()

class ContestsViewModel: ViewModel() {

    fun flowOfLoadingStatus(): Flow<LoadingStatus> =
        fetchResults.map { it.values.combineLoadingStatus { it.loadingStatus } }

    fun flowOfLoadingErrors(): Flow<List<Pair<ContestsFetchSource?, Throwable>>> =
        fetchResults.map {
            it.values.flatMap {
                if (it.loadingStatus == FAILED) it.errors else emptyList()
            }
        }

    private val fetchResults = MutableStateFlow(emptyMap<ContestPlatform, FetchTrack>())

    private inline fun editFetchResults(platform: ContestPlatform, block: (FetchTrack) -> FetchTrack) {
        fetchResults.edit {
            val current = getOrElse(platform) { FetchTrack(loadingStatus = PENDING, errors = emptyList()) }
            set(platform, block(current))
        }
    }

    private fun Flow<ContestsFetchResult>.trackLoadingStatus(platform: ContestPlatform): Flow<ContestsFetchResult> {
        return onStart {
            editFetchResults(platform) {
                check(it.loadingStatus != LOADING)
                FetchTrack(LOADING, emptyList())
            }
        }.onEach { (platform, source, result) ->
            result.onFailure { error ->
                editFetchResults(platform) {
                    it.copy(errors = it.errors + Pair(source, error))
                }
            }
        }.onCompletion {
            editFetchResults(platform) {
                it.copy(loadingStatus = if (it.errors.isEmpty()) PENDING else FAILED)
            }
        }
    }

    private suspend fun Map<ContestPlatform, Flow<ContestsFetchResult>>.collectToRepository(context: Context) =
        mapValues { it.value.trackLoadingStatus(platform = it.key) }
        .collectTo(repository = context.contestsRepository)

    private val reloadEnabledPlatformsMutex = Mutex()
    fun reloadEnabledPlatforms(context: Context) {
        uniqueLaunch(mutex = reloadEnabledPlatformsMutex) {
            ContestsWorker.getWork(context).enqueueInRepeatInterval()

            context.settingsContests.contestsFetchFlows()
                .collectToRepository(context = context)
        }
    }

    private val applyChangedSettingsMutex = Mutex()
    fun applyChangedSettings(context: Context) {
        uniqueLaunch(mutex = applyChangedSettingsMutex) {
            val infoDataStore = ContestsInfoDataStore(context)
            val snapshot = infoDataStore.settingsSnapshot() ?: return@uniqueLaunch
            infoDataStore.settingsSnapshot.setValue(null)

            val settings = context.settingsContests
            val diff = settings.fetchSettingsSnapshot().differenceFrom(snapshot)

            val fakeResults = diff.toRemove.map {
                val platform = it.toContestPlatform()
                val result = ContestsFetchResult(
                    platform = platform,
                    fetchSource = null,
                    result = Result.success(emptyList())
                )
                platform to flowOf(result)
            }

            settings.contestsFetchFlows(platforms = diff.contestPlatformsToReload())
                .plus(pairs = fakeResults)
                .collectToRepository(context = context)
        }
    }
}

private fun ContestsFetchSettingsSnapshotDiff.contestPlatformsToReload(): Set<ContestPlatform> =
    emptyEnumSet<ContestPlatform>().apply {
        toReload.forEach { add(it.toContestPlatform()) }
        if (clistReload) add(unknown)
    }

private data class FetchTrack(
    val loadingStatus: LoadingStatus,
    val errors: List<Pair<ContestsFetchSource?, Throwable>>
)