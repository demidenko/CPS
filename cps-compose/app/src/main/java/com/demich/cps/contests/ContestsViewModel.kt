package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestsListDao
import com.demich.cps.contests.database.contestsListDao
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.contests.loading.ContestsReceiver
import com.demich.cps.contests.loading.asContestsReceiver
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.combine
import com.demich.cps.utils.edit
import com.demich.cps.utils.sharedViewModel
import com.demich.cps.utils.toLoadingStatus
import com.demich.cps.workers.ContestsWorker
import com.demich.datastore_itemized.edit
import com.demich.kotlin_stdlib_boost.mapToSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun contestsViewModel(): ContestsViewModel = sharedViewModel()

class ContestsViewModel: ViewModel(), ContestsReloader, ContestsIdsHolder {

    fun flowOfLoadingStatus(): Flow<LoadingStatus> =
        loadingStatuses.map { it.values.combine() }

    fun flowOfLoadingErrors(): Flow<List<Pair<ContestsLoaderType,Throwable>>> =
        combine(loadingStatuses, errors) { loadingStatuses, errors ->
            loadingStatuses
                .filter { it.value == LoadingStatus.FAILED }
                .flatMap { errors[it.key] ?: emptyList() }
                .distinct()
        }

    private val loadingStatuses = MutableStateFlow(emptyMap<Contest.Platform, LoadingStatus>())
    private val errors = MutableStateFlow(emptyMap<Contest.Platform, List<Pair<ContestsLoaderType,Throwable>>>())

    private suspend fun ContestsListDao.removePlatform(platform: Contest.Platform) {
        replace(platform, emptyList())
        errors.edit { remove(platform) }
        setLoadingStatus(platform, LoadingStatus.PENDING)
    }

    private fun setLoadingStatus(platform: Contest.Platform, loadingStatus: LoadingStatus) =
        loadingStatuses.edit {
            if (loadingStatus == LoadingStatus.LOADING) check(this[platform] != LoadingStatus.LOADING)
            if (loadingStatus == LoadingStatus.PENDING) remove(platform)
            else this[platform] = loadingStatus
        }

    private fun ContestsListDao.makeReceiver(): ContestsReceiver {
        val lastResult = mutableMapOf<Contest.Platform, Result<*>>()
        return asContestsReceiver(
            onStartLoading = { platform ->
                setLoadingStatus(platform, LoadingStatus.LOADING)
                errors.edit { remove(platform) }
            },
            onFinish = { platform ->
                setLoadingStatus(platform, lastResult.getValue(platform).toLoadingStatus())
            },
            onResult = { platform, loaderType, result ->
                result.onFailure { error ->
                    errors.edit { edit(platform) { add(loaderType to error) } }
                }
                lastResult[platform] = result
            }
        )
    }

    fun reloadEnabledPlatforms(context: Context) {
        ContestsWorker.getWork(context).enqueueIn(
            duration = ContestsWorker.workRepeatInterval,
            repeatInterval = ContestsWorker.workRepeatInterval
        )
        viewModelScope.launch(Dispatchers.IO) {
            reloadEnabledPlatforms(
                settings = context.settingsContests,
                contestsInfo = ContestsInfoDataStore(context),
                contestsReceiver = context.contestsListDao.makeReceiver()
            )
        }
    }

    fun syncEnabledAndLastReloaded(context: Context) {
        viewModelScope.launch {
            val settings = context.settingsContests
            val listInfo = ContestsInfoDataStore(context)
            val dao = context.contestsListDao
            val enabled = settings.enabledPlatforms()
            val lastReloaded = listInfo.lastReloadedPlatforms()
            (lastReloaded - enabled).takeIf { it.isNotEmpty() }?.let { toRemove ->
                listInfo.lastReloadedPlatforms.edit { removeAll(toRemove) }
                toRemove.forEach { platform -> dao.removePlatform(platform) }
            }

            val toReload = (enabled - lastReloaded).toMutableSet()

            if (needToReloadClistAdditional(settings, listInfo)) {
                listInfo.clistLastReloadedAdditionalResources.update { emptySet() }
                toReload.add(Contest.Platform.unknown)
            }

            withContext(Dispatchers.IO) {
                reload(
                    platforms = toReload,
                    settings = settings,
                    contestsInfo = listInfo,
                    contestsReceiver = dao.makeReceiver()
                )
            }
        }
    }

    private suspend fun needToReloadClistAdditional(settings: ContestsSettingsDataStore, listInfo: ContestsInfoDataStore): Boolean {
        val enabled: Set<Int> = settings.clistAdditionalResources().mapToSet { it.id }
        val lastReloaded: Set<Int> = listInfo.clistLastReloadedAdditionalResources()
        return enabled != lastReloaded //hope it is proper equals
    }

    private val expandedContests = MutableStateFlow(emptyMap<ContestCompositeId, Contest>())
    fun flowOfExpandedContests(): StateFlow<Map<ContestCompositeId, Contest>> = expandedContests

    override fun editIds(block: MutableMap<ContestCompositeId, Contest>.() -> Unit) =
        expandedContests.edit(block)

}

