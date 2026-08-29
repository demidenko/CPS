package com.demich.cps.profiles

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.fetchstate.FetchResult
import com.demich.cps.fetchstate.fetchFlowOf
import com.demich.cps.platforms.Platform
import com.demich.cps.platforms.utils.toProfileResult
import com.demich.cps.profiles.managers.ProfileManager
import com.demich.cps.profiles.managers.profileManagerOf
import com.demich.cps.profiles.userinfo.ClistUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.UserInfo
import com.demich.cps.ui.bottomprogressbar.ProgressBarsViewModel
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.combine
import com.demich.cps.utils.edit
import com.demich.cps.utils.joinAllWithProgress
import com.demich.cps.utils.sharedViewModel
import com.demich.cps.utils.toLoadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

@Composable
fun profilesViewModel(): ProfilesViewModel = sharedViewModel()

class ProfilesViewModel: ViewModel() {
    private val loadingStatuses = MutableStateFlow(emptyMap<Platform, LoadingStatus>())

    fun flowOfLoadingStatus(manager: ProfileManager<*>): Flow<LoadingStatus> =
        loadingStatuses.map { it[manager.platform] ?: PENDING }

    fun flowOfLoadingStatus(): Flow<LoadingStatus> =
        loadingStatuses.map { it.values.combine() }

    private fun setLoadingStatus(manager: ProfileManager<*>, loadingStatus: LoadingStatus) =
        loadingStatuses.edit {
            if (loadingStatus == PENDING) remove(manager.platform)
            else set(key = manager.platform, value = loadingStatus)
        }

    fun <U: UserInfo> reload(manager: ProfileManager<U>, context: Context) {
        if (loadingStatuses.value[manager.platform] == LOADING) return
        viewModelScope.launch(Dispatchers.Default) {
            val storage = manager.profileStorage(context)
            val userId = storage.profile()?.userId ?: return@launch

            fetchFlowOf { manager.getUserInfo(userId) }.collect {
                if (it is FetchResult) {
                    val profileResult = it.toProfileResult(userId)
                    if (profileResult !is ProfileResult.Failed) {
                        storage.setProfile(profileResult, reset = false)
                    }
                }
                setLoadingStatus(manager, it.toLoadingStatus())
            }
        }
    }

    fun delete(manager: ProfileManager<*>, context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            manager.profileStorage(context).deleteProfile()
            setLoadingStatus(manager, PENDING)
        }
    }

    fun runClistImport(
        cListUserInfo: ClistUserInfo,
        progressBarsViewModel: ProgressBarsViewModel,
        context: Context
    ) {
        progressBarsViewModel.doJob(id = clistImportId, coroutineScope = viewModelScope) {
            val supported = cListUserInfo.accounts.mapNotNull { (resource, userData) ->
                getManager(resource, userData.first, userData.second)
            }
            supported.map { (platform, userId) ->
                suspend {
                    replaceProfile(
                        manager = profileManagerOf(platform),
                        userId = userId,
                        context = context
                    )
                }
            }.joinAllWithProgress(title = "clist import") {
                send(it)
            }
        }
    }

    private suspend fun <U: UserInfo> replaceProfile(
        manager: ProfileManager<U>,
        userId: String,
        context: Context
    ) {
        //wait for loading stops
        loadingStatuses.takeWhile { it[manager.platform] == LOADING }.collect()
        val storage = manager.profileStorage(context)
        val savedUserId = storage.profile()?.userId
        if (userId.equals(savedUserId, ignoreCase = true)) {
            //if userId is same just reload to prevent replace by Failure
            reload(manager, context)
        } else {
            fetchFlowOf { manager.getUserInfo(userId) }.collect {
                when (it) {
                    is FetchResult -> {
                        storage.setProfile(it.toProfileResult(userId))
                        // PENDING even on Failure
                        setLoadingStatus(manager, PENDING)
                    }
                    else -> setLoadingStatus(manager, it.toLoadingStatus())
                }
            }
        }
    }
}

private const val clistImportId = "clist_import"

fun ProgressBarsViewModel.flowOfClistImportIsRunning(): Flow<Boolean> =
    flowOfProgresses.map { clistImportId in it }

private fun getManager(resource: String, userName: String, link: String): Pair<Platform, String>? =
    when (resource) {
        "codeforces.com" -> Platform.codeforces to userName
        "atcoder.jp" -> Platform.atcoder to userName
        "codechef.com" -> Platform.codechef to userName
        "dmoj.ca" -> Platform.dmoj to userName
        "acm.timus.ru", "timus.online" -> {
            val userId = link.substring(link.lastIndexOf('=')+1)
            Platform.timus to userId
        }
        else -> null
    }