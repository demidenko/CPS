package com.demich.cps.profiles

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.profiles.managers.ProfileManager
import com.demich.cps.profiles.managers.ProfilePlatform
import com.demich.cps.profiles.managers.RatedProfileManager
import com.demich.cps.profiles.managers.profileManagerOf
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

    fun flowOfLoadingStatus(manager: ProfileManager<*>): Flow<LoadingStatus> =
        loadingStatuses.map { it[manager.platform] ?: PENDING }

    fun flowOfLoadingStatus(managers: Collection<ProfileManager<*>>): Flow<LoadingStatus> =
        loadingStatuses.map { map -> managers.mapNotNull { map[it.platform] }.combine() }

    private fun setLoadingStatus(manager: ProfileManager<*>, loadingStatus: LoadingStatus) =
        loadingStatuses.edit {
            if (loadingStatus == PENDING) remove(manager.platform)
            else this[manager.platform] = loadingStatus
        }

    fun <U: UserInfo> reload(manager: ProfileManager<U>, context: Context) {
        if (loadingStatuses.value[manager.platform] == LOADING) return
        viewModelScope.launch(Dispatchers.Default) {
            val storage = manager.profileStorage(context)
            val savedProfile = storage.profile() ?: return@launch

            setLoadingStatus(manager, LOADING)
            val profileResult = manager.fetchProfile(savedProfile.userId)

            if (profileResult is ProfileResult.Failed) {
                setLoadingStatus(manager, FAILED)
            } else {
                setLoadingStatus(manager, PENDING)
                storage.setProfile(profileResult)
            }
        }
    }

    fun <U: UserInfo> delete(manager: ProfileManager<U>, context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            setLoadingStatus(manager, PENDING)
            manager.profileStorage(context).deleteProfile()
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
                    val manager = profileManagerOf(platform)
                    //wait for loading stops
                    loadingStatuses.takeWhile { it[platform] == LOADING }.collect()
                    val savedUserId = manager.profileStorage(context).profile()?.userId
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

    private suspend fun <U: UserInfo> getAndSave(manager: ProfileManager<U>, userId: String, context: Context) {
        manager.profileStorage(context).setProfile(manager.fetchProfile(userId))
    }

    private val ratingLoader = backgroundDataLoader<List<RatingChange>>()
    fun flowOfRatingResult(manager: RatedProfileManager<*>, userId: String, key: Any) =
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