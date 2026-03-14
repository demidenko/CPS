package com.demich.cps.profiles

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.profiles.managers.AccountManager
import com.demich.cps.profiles.managers.ProfilePlatform
import com.demich.cps.profiles.managers.RatedAccountManager
import com.demich.cps.profiles.managers.RatingChange
import com.demich.cps.profiles.managers.accountManagerOf
import com.demich.cps.profiles.userinfo.ClistUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.UserInfo
import com.demich.cps.ui.bottomprogressbar.ProgressBarsViewModel
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.backgroundDataLoader
import com.demich.cps.utils.combine
import com.demich.cps.utils.edit
import com.demich.cps.utils.joinAllWithProgress
import com.demich.cps.utils.sharedViewModel
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
    private val loadingStatuses = MutableStateFlow(emptyMap<ProfilePlatform, LoadingStatus>())

    fun flowOfLoadingStatus(manager: AccountManager<out UserInfo>): Flow<LoadingStatus> =
        loadingStatuses.map { it[manager.platform] ?: PENDING }

    fun flowOfLoadingStatus(managers: Collection<AccountManager<out UserInfo>>): Flow<LoadingStatus> =
        loadingStatuses.map { map -> managers.mapNotNull { map[it.platform] }.combine() }

    private fun setLoadingStatus(manager: AccountManager<out UserInfo>, loadingStatus: LoadingStatus) =
        loadingStatuses.edit {
            if (loadingStatus == PENDING) remove(manager.platform)
            else this[manager.platform] = loadingStatus
        }

    fun <U: UserInfo> reload(manager: AccountManager<U>, context: Context) {
        if (loadingStatuses.value[manager.platform] == LOADING) return
        viewModelScope.launch(Dispatchers.Default) {
            val dataStore = manager.dataStore(context)
            val savedProfile = dataStore.profile() ?: return@launch

            setLoadingStatus(manager, LOADING)
            val profileResult = manager.fetchProfile(savedProfile.userId)

            if (profileResult is ProfileResult.Failed) {
                setLoadingStatus(manager, FAILED)
            } else {
                setLoadingStatus(manager, PENDING)
                dataStore.setProfile(profileResult)
            }
        }
    }

    fun <U: UserInfo> delete(manager: AccountManager<U>, context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            setLoadingStatus(manager, PENDING)
            manager.dataStore(context).deleteProfile()
        }
    }

    fun runClistImport(
        cListUserInfo: ClistUserInfo,
        progressBarsViewModel: ProgressBarsViewModel,
        context: Context
    ) {
        progressBarsViewModel.doJob(id = ProgressBarsViewModel.clistImportId, coroutineScope = viewModelScope) { progress ->
            val supported = cListUserInfo.accounts.mapNotNull { (resource, userData) ->
                getManager(resource, userData.first, userData.second)
            }
            supported.map { (platform, userId) ->
                suspend {
                    val manager = accountManagerOf(platform)
                    //wait for loading stops
                    loadingStatuses.takeWhile { it[platform] == LOADING }.collect()
                    val savedUserId = manager.dataStore(context).profile()?.userId
                    if (userId.equals(savedUserId, ignoreCase = true)) {
                        //if userId is same just reload to prevent replace by FAILED
                        reload(manager, context)
                    } else {
                        setLoadingStatus(manager, LOADING)
                        getAndSave(manager, userId, context)
                        setLoadingStatus(manager, PENDING)
                    }
                }
            }.joinAllWithProgress(title = "clist import") {
                progress(it)
            }
        }
    }

    private suspend fun <U: UserInfo> getAndSave(manager: AccountManager<U>, userId: String, context: Context) {
        manager.dataStore(context).setProfile(manager.fetchProfile(userId))
    }

    private val ratingLoader = backgroundDataLoader<List<RatingChange>>()
    fun flowOfRatingResult(manager: RatedAccountManager<*>, userId: String, key: Long) =
        ratingLoader.execute(id = "$userId#$key") { manager.getRatingChangeHistory(userId) }
}

private fun getManager(resource: String, userName: String, link: String): Pair<ProfilePlatform, String>? =
    when (resource) {
        "codeforces.com" -> ProfilePlatform.codeforces to userName
        "atcoder.jp" -> ProfilePlatform.atcoder to userName
        "codechef.com" -> ProfilePlatform.codechef to userName
        "dmoj.ca" -> ProfilePlatform.dmoj to userName
        "acm.timus.ru", "timus.online" -> {
            val userId = link.substring(link.lastIndexOf('=')+1)
            ProfilePlatform.timus to userId
        }
        else -> null
    }