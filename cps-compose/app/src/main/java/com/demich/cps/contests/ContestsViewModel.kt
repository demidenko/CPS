package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.contestsListDao
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.contests.loading.ContestsLoadingResult
import com.demich.cps.contests.loading.asContestsReceiver
import com.demich.cps.contests.settings.differenceFrom
import com.demich.cps.contests.settings.makeSnapshot
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.LoadingStatus.FAILED
import com.demich.cps.utils.LoadingStatus.LOADING
import com.demich.cps.utils.LoadingStatus.PENDING
import com.demich.cps.utils.combine
import com.demich.cps.utils.edit
import com.demich.cps.utils.sharedViewModel
import com.demich.cps.utils.toLoadingStatus
import com.demich.cps.workers.ContestsWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@Composable
fun contestsViewModel(): ContestsViewModel = sharedViewModel()

class ContestsViewModel: ViewModel(), ContestsReloader, ContestsIdsHolder {

    fun flowOfLoadingStatus(): Flow<LoadingStatus> =
        loadingStatuses.map { it.values.combine() }

    fun flowOfLoadingErrors(): Flow<List<Pair<ContestsLoaderType,Throwable>>> =
        combine(loadingStatuses, errors) { loadingStatuses, errors ->
            loadingStatuses
                .filter { it.value == FAILED }
                .flatMap { errors[it.key] ?: emptyList() }
                .distinct()
        }

    private val loadingStatuses = MutableStateFlow(emptyMap<Contest.Platform, LoadingStatus>())
    private val errors = MutableStateFlow(emptyMap<Contest.Platform, List<Pair<ContestsLoaderType,Throwable>>>())

    private fun setLoadingStatus(platform: Contest.Platform, loadingStatus: LoadingStatus) =
        loadingStatuses.edit {
            if (loadingStatus == LOADING) check(this[platform] != LOADING)
            if (loadingStatus == PENDING) remove(platform)
            else this[platform] = loadingStatus
        }

    override fun transform(
        platform: Contest.Platform,
        flow: Flow<ContestsLoadingResult>
    ) = flow.run {
        var lastStatus: LoadingStatus = PENDING
        onStart {
            setLoadingStatus(platform, LOADING)
            errors.edit { remove(platform) }
        }.onEach { (platform, loaderType, result) ->
            result.onFailure { error ->
                errors.edit { edit(platform) { add(loaderType to error) } }
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
                contestsReceiver = context.contestsListDao.asContestsReceiver()
            )
        }
    }

    fun applyChangedSettings(context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            val infoDataStore = ContestsInfoDataStore(context)
            val snapshot = infoDataStore.settingsSnapshot() ?: return@launch
            infoDataStore.settingsSnapshot.update { null }

            val settings = context.settingsContests
            val diff = settings.makeSnapshot().differenceFrom(snapshot)

            val dao = context.contestsListDao
            diff.toRemove.forEach { platform ->
                dao.replace(platform, emptyList())
                errors.edit { remove(platform) }
                setLoadingStatus(platform, PENDING)
            }

            reload(
                platforms = diff.toReload,
                settings = settings,
                contestsReceiver = dao.asContestsReceiver()
            )
        }
    }

    private val expandedContests = MutableStateFlow(emptyMap<ContestCompositeId, Contest>())
    fun flowOfExpandedContests(): StateFlow<Map<ContestCompositeId, Contest>> = expandedContests

    override fun editIds(block: MutableMap<ContestCompositeId, Contest>.() -> Unit) =
        expandedContests.edit(block)

}

