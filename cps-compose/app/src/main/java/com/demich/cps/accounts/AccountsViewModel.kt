package com.demich.cps.accounts

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.accounts.managers.AccountManager
import com.demich.cps.accounts.managers.AccountManagerType
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.RatingChange
import com.demich.cps.accounts.managers.allAccountManagers
import com.demich.cps.accounts.userinfo.ClistUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.ui.bottomprogressbar.ProgressBarInfo
import com.demich.cps.ui.bottomprogressbar.ProgressBarsViewModel
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.backgroundDataLoader
import com.demich.cps.utils.edit
import com.demich.cps.utils.sharedViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

@Composable
fun accountsViewModel(): AccountsViewModel = sharedViewModel()

class AccountsViewModel: ViewModel() {
    private val loadingStatuses = MutableStateFlow(emptyMap<AccountManagerType, LoadingStatus>())

    fun flowOfLoadingStatus(manager: AccountManager<out UserInfo>): Flow<LoadingStatus> =
        loadingStatuses.map { it[manager.type] ?: LoadingStatus.PENDING }

    private fun setLoadingStatus(manager: AccountManager<out UserInfo>, loadingStatus: LoadingStatus) =
        loadingStatuses.edit {
            if (loadingStatus == LoadingStatus.PENDING) remove(manager.type)
            else this[manager.type] = loadingStatus
        }

    fun<U: UserInfo> reload(manager: AccountManager<U>, context: Context) {
        if (loadingStatuses.value[manager.type] == LoadingStatus.LOADING) return
        viewModelScope.launch {
            val dataStore = manager.dataStore(context)
            val savedInfo = dataStore.getSavedInfo() ?: return@launch

            setLoadingStatus(manager, LoadingStatus.LOADING)
            val info = manager.loadInfo(savedInfo.userId)

            if (info.status == STATUS.FAILED) {
                setLoadingStatus(manager, LoadingStatus.FAILED)
            } else {
                setLoadingStatus(manager, LoadingStatus.PENDING)
                dataStore.setSavedInfo(info)
            }
        }
    }

    fun<U: UserInfo> delete(manager: AccountManager<U>, context: Context) {
        viewModelScope.launch {
            setLoadingStatus(manager, LoadingStatus.PENDING)
            manager.dataStore(context).deleteSavedInfo()
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
            progress.value = ProgressBarInfo(title = "clist import", total = supported.size)
            supported.map { (type, userId) ->
                val manager = allAccountManagers.first { it.type == type }
                launch {
                    //wait for loading stops
                    loadingStatuses.takeWhile { it[type] == LoadingStatus.LOADING }.collect()
                    if (userId.equals(manager.dataStore(context).getSavedInfo()?.userId, ignoreCase = true)) {
                        //if userId is same just reload to prevent replace by FAILED
                        reload(manager, context)
                    } else {
                        setLoadingStatus(manager, LoadingStatus.LOADING)
                        loadAndSave(manager, userId, context)
                        setLoadingStatus(manager, LoadingStatus.PENDING)
                    }
                    progress.value++
                }
            }.joinAll()
        }
    }

    private suspend fun<U: UserInfo> loadAndSave(manager: AccountManager<U>, userId: String, context: Context) {
        manager.dataStore(context).setSavedInfo(manager.loadInfo(userId))
    }

    private val ratingLoader = backgroundDataLoader<List<RatingChange>>()
    fun flowOfRatingResult(manager: RatedAccountManager<*>, userId: String, key: Int) =
        ratingLoader.execute(id = "$userId#$key") { manager.getRatingHistory(userId) }
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