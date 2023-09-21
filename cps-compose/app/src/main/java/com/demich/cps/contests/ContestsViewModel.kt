package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestsListDao
import com.demich.cps.contests.database.contestsListDao
import com.demich.cps.contests.loading.ContestsReceiver
import com.demich.cps.contests.loading.asContestsReceiver
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.append
import com.demich.cps.utils.combine
import com.demich.cps.utils.edit
import com.demich.cps.utils.mapToSet
import com.demich.cps.utils.sharedViewModel
import com.demich.cps.utils.toLoadingStatus
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
            if (loadingStatus == LoadingStatus.LOADING) require(this[platform] != LoadingStatus.LOADING)
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
                    errors.update { it.append(platform, loaderType to error) }
                }
                lastResult[platform] = result
            }
        )
    }

    fun reloadEnabledPlatforms(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            reloadEnabledPlatforms(
                settings = context.settingsContests,
                contestsInfo = ContestsInfoDataStore(context),
                contestsReceiver = context.contestsListDao.makeReceiver()
            )
        }
    }

    fun syncEnabledAndLastReloaded(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
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

            reload(
                platforms = toReload,
                settings = settings,
                contestsInfo = listInfo,
                contestsReceiver = dao.makeReceiver()
            )
        }
    }

    private suspend fun needToReloadClistAdditional(settings: ContestsSettingsDataStore, listInfo: ContestsInfoDataStore): Boolean {
        val enabled: Set<Int> = settings.clistAdditionalResources().mapToSet { it.id }
        val lastReloaded: Set<Int> = listInfo.clistLastReloadedAdditionalResources()
        return enabled != lastReloaded //hope it is proper equals
    }

    private val expandedItems = MutableStateFlow(emptyMap<ContestCompositeId, Contest>())
    fun flowOfExpandedContests(): StateFlow<Map<ContestCompositeId, Contest>> = expandedItems

    override fun editIds(block: MutableMap<ContestCompositeId, Contest>.() -> Unit) =
        expandedItems.edit(block)

}

