package com.demich.cps.accounts

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.accounts.managers.AccountManager
import com.demich.cps.accounts.managers.AccountManagerType
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.RatingChange
import com.demich.cps.accounts.managers.accountManagerOf
import com.demich.cps.accounts.userinfo.ClistUserInfo
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.ui.bottomprogressbar.ProgressBarsViewModel
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.LoadingStatus.FAILED
import com.demich.cps.utils.LoadingStatus.LOADING
import com.demich.cps.utils.LoadingStatus.PENDING
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
    private val loadingStatuses = MutableStateFlow(emptyMap<AccountManagerType, LoadingStatus>())

    fun flowOfLoadingStatus(manager: AccountManager<out UserInfo>): Flow<LoadingStatus> =
        loadingStatuses.map { it[manager.type] ?: PENDING }

    fun flowOfLoadingStatus(managers: Collection<AccountManager<out UserInfo>>): Flow<LoadingStatus> =
        loadingStatuses.map { map -> managers.mapNotNull { map[it.type] }.combine() }

    private fun setLoadingStatus(manager: AccountManager<out UserInfo>, loadingStatus: LoadingStatus) =
        loadingStatuses.edit {
            if (loadingStatus == PENDING) remove(manager.type)
            else this[manager.type] = loadingStatus
        }

    fun <U: UserInfo> reload(manager: AccountManager<U>, context: Context) {
        if (loadingStatuses.value[manager.type] == LOADING) return
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
            supported.map { (type, userId) ->
                suspend {
                    val manager = accountManagerOf(type)
                    //wait for loading stops
                    loadingStatuses.takeWhile { it[type] == LOADING }.collect()
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

private fun getManager(resource: String, userName: String, link: String): Pair<AccountManagerType, String>? =
    when (resource) {
        "codeforces.com" -> AccountManagerType.codeforces to userName
        "atcoder.jp" -> AccountManagerType.atcoder to userName
        "codechef.com" -> AccountManagerType.codechef to userName
        "dmoj.ca" -> AccountManagerType.dmoj to userName
        "acm.timus.ru", "timus.online" -> {
            val userId = link.substring(link.lastIndexOf('=')+1)
            AccountManagerType.timus to userId
        }
        else -> null
    }