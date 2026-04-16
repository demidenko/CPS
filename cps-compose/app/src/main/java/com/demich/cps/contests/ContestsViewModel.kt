package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.contests.database.ContestPlatform
import com.demich.cps.contests.database.contestsRepository
import com.demich.cps.contests.database.toContestPlatform
import com.demich.cps.contests.loading.ContestsFetchResult
import com.demich.cps.contests.loading.ContestsFetchSource
import com.demich.cps.contests.loading.asContestsReceiver
import com.demich.cps.contests.settings.ContestsSettingsSnapshotDiff
import com.demich.cps.contests.settings.differenceFrom
import com.demich.cps.contests.settings.makeSnapshot
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.combine
import com.demich.cps.utils.edit
import com.demich.cps.utils.sharedViewModel
import com.demich.cps.utils.toLoadingStatus
import com.demich.cps.workers.ContestsWorker
import com.demich.kotlin_stdlib_boost.emptyEnumSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@Composable
fun contestsViewModel(): ContestsViewModel = sharedViewModel()

class ContestsViewModel: ViewModel(), ContestsReloader {

    fun flowOfLoadingStatus(): Flow<LoadingStatus> =
        loadingStatuses.map { it.values.combine() }

    fun flowOfLoadingErrors(): Flow<List<Pair<ContestsFetchSource,Throwable>>> =
        combine(loadingStatuses, errors) { loadingStatuses, errors ->
            loadingStatuses
                .filter { it.value == FAILED }
                .flatMap { errors[it.key] ?: emptyList() }
                .distinct()
        }

    private val loadingStatuses = MutableStateFlow(emptyMap<ContestPlatform, LoadingStatus>())
    private val errors = MutableStateFlow(emptyMap<ContestPlatform, List<Pair<ContestsFetchSource,Throwable>>>())

    private fun setLoadingStatus(platform: ContestPlatform, loadingStatus: LoadingStatus) =
        loadingStatuses.edit {
            if (loadingStatus == LOADING) check(this[platform] != LOADING)
            if (loadingStatus == PENDING) remove(platform)
            else this[platform] = loadingStatus
        }

    override fun transform(
        platform: ContestPlatform,
        flow: Flow<ContestsFetchResult>
    ) = flow.run {
        var lastStatus: LoadingStatus = PENDING
        onStart {
            setLoadingStatus(platform, LOADING)
            errors.edit { remove(platform) }
        }.onEach { (platform, source, result) ->
            result.onFailure { error ->
                errors.edit { edit(platform) { add(source to error) } }
            }
            lastStatus = result.toLoadingStatus()
        }.onCompletion {
            setLoadingStatus(platform, lastStatus)
        }
    }

    fun reloadEnabledPlatforms(context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            ContestsWorker.getWork(context).enqueueInRepeatInterval()
            reloadEnabledPlatforms(
                settings = context.settingsContests,
                contestsReceiver = context.contestsRepository.asContestsReceiver()
            )
        }
    }

    fun applyChangedSettings(context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            val infoDataStore = ContestsInfoDataStore(context)
            val snapshot = infoDataStore.settingsSnapshot() ?: return@launch
            infoDataStore.settingsSnapshot.setValue(null)

            val settings = context.settingsContests
            val diff = settings.makeSnapshot().differenceFrom(snapshot)

            val repository = context.contestsRepository
            diff.toRemove.forEach {
                // TODO: replace it to fake fetch flows
                val platform = it.toContestPlatform()
                repository.setContests(platform, emptyList())
                errors.edit { remove(platform) }
                setLoadingStatus(platform, PENDING)
            }

            reload(
                platforms = diff.contestPlatformsToReload(),
                settings = settings,
                contestsReceiver = repository.asContestsReceiver()
            )
        }
    }
}

private fun ContestsSettingsSnapshotDiff.contestPlatformsToReload(): Set<ContestPlatform> =
    emptyEnumSet<ContestPlatform>().apply {
        toReload.forEach { add(it.toContestPlatform()) }
        if (clistReload) add(unknown)
    }