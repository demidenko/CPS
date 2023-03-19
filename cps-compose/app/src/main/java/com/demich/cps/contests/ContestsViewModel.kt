package com.demich.cps.contests

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestsListDao
import com.demich.cps.contests.database.contestsListDao
import com.demich.cps.contests.loaders.ContestsLoaders
import com.demich.cps.contests.loaders.ContestsReceiver
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.combine
import com.demich.cps.utils.mapToSet
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContestsViewModel: ViewModel(), ContestsReloader {

    fun flowOfLoadingStatus(): Flow<LoadingStatus> =
            Contest.platforms.map { mutableLoadingStatusFor(it) }.combine()

    fun flowOfLoadingErrors(): Flow<List<Pair<ContestsLoaders,Throwable>>> =
        combine(
            flow = Contest.platforms.associateWith { mutableLoadingStatusFor(it) }.combine(),
            flow2 = Contest.platforms.associateWith { mutableErrorsList(it) }.combine()
        ) { loadingStatuses, errors ->
            loadingStatuses
                .filter { it.value == LoadingStatus.FAILED }
                .flatMap { errors[it.key] ?: emptyList() }
                .distinct()
        }

    private val loadingStatuses: MutableMap<Contest.Platform, MutableStateFlow<LoadingStatus>> = mutableMapOf()
    private fun mutableLoadingStatusFor(platform: Contest.Platform) =
        loadingStatuses.getOrPut(platform) { MutableStateFlow(LoadingStatus.PENDING) }

    private val errors: MutableMap<Contest.Platform, MutableStateFlow<List<Pair<ContestsLoaders,Throwable>>>> = mutableMapOf()
    private fun mutableErrorsList(platform: Contest.Platform) =
        errors.getOrPut(platform) { MutableStateFlow(emptyList()) }

    private suspend fun ContestsListDao.removePlatform(platform: Contest.Platform) {
        replace(platform, emptyList())
        mutableErrorsList(platform).value = emptyList()
        mutableLoadingStatusFor(platform).value = LoadingStatus.PENDING
    }

    private fun ContestsListDao.makeReceiver() =
        ContestsReceiver(
            dao = this,
            setLoadingStatus = { platform, loadingStatus ->
                mutableLoadingStatusFor(platform).update {
                    if (loadingStatus == LoadingStatus.LOADING) require(it != LoadingStatus.LOADING)
                    loadingStatus
                }
            },
            consumeError = { platform, loaderType, e ->
                mutableErrorsList(platform).value += loaderType to e
            },
            clearErrors = { mutableErrorsList(it).value = emptyList() }
        )

    fun reloadEnabledPlatforms(context: Context) {
        viewModelScope.launch {
            reloadEnabledPlatforms(
                settings = context.settingsContests,
                contestsReceiver = context.contestsListDao.makeReceiver()
            )
        }
    }

    fun syncEnabledAndLastReloaded(context: Context) {
        viewModelScope.launch {
            val settings = context.settingsContests
            val dao = context.contestsListDao
            val enabled = settings.enabledPlatforms()
            val lastReloaded = settings.lastReloadedPlatforms()
            (lastReloaded - enabled).takeIf { it.isNotEmpty() }?.let { toRemove ->
                settings.lastReloadedPlatforms.edit { removeAll(toRemove) }
                toRemove.forEach { platform -> dao.removePlatform(platform) }
            }

            val toReload = (enabled - lastReloaded).toMutableSet()

            if (settings.needToReloadClistAdditional()) {
                settings.clistLastReloadedAdditionalResources.update { emptySet() }
                toReload.add(Contest.Platform.unknown)
            }

            reload(
                platforms = toReload,
                settings = settings,
                contestsReceiver = dao.makeReceiver()
            )
        }
    }

    private suspend fun ContestsSettingsDataStore.needToReloadClistAdditional(): Boolean {
        val enabled: Set<Int> = clistAdditionalResources().mapToSet { it.id }
        val lastReloaded: Set<Int> = clistLastReloadedAdditionalResources()
        return enabled != lastReloaded //hope it is proper equals
    }
}

