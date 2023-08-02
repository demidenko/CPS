package com.demich.cps.accounts

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.accounts.managers.*
import com.demich.cps.accounts.userinfo.ClistUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.ui.bottomprogressbar.ProgressBarInfo
import com.demich.cps.ui.bottomprogressbar.ProgressBarsViewModel
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.sharedViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

@Composable
fun accountsViewModel(): AccountsViewModel = sharedViewModel()

class AccountsViewModel: ViewModel() {
    private val loadingStatuses = mutableMapOf<AccountManagers, MutableStateFlow<LoadingStatus>>()

    private fun mutableLoadingStatusFor(manager: AccountManager<out UserInfo>): MutableStateFlow<LoadingStatus> =
        loadingStatuses.getOrPut(manager.type) { MutableStateFlow(LoadingStatus.PENDING) }

    fun flowOfLoadingStatus(manager: AccountManager<out UserInfo>): Flow<LoadingStatus> =
        mutableLoadingStatusFor(manager)


    fun<U: UserInfo> reload(manager: AccountManager<U>) {
        viewModelScope.launch {
            val savedInfo = manager.getSavedInfo() ?: return@launch

            val loadingStatusState = mutableLoadingStatusFor(manager)
            if (loadingStatusState.value == LoadingStatus.LOADING) return@launch

            loadingStatusState.value = LoadingStatus.LOADING
            val info = manager.loadInfo(savedInfo.userId)

            if (info.status == STATUS.FAILED) {
                loadingStatusState.value = LoadingStatus.FAILED
            } else {
                loadingStatusState.value = LoadingStatus.PENDING
                manager.setSavedInfo(info)
            }
        }
    }

    fun<U: UserInfo> delete(manager: AccountManager<U>) {
        viewModelScope.launch {
            mutableLoadingStatusFor(manager).update {
                require(it != LoadingStatus.LOADING)
                LoadingStatus.PENDING
            }
            manager.deleteSavedInfo()
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
            val managers = context.allAccountManagers
            supported.map { (type, userId) ->
                val manager = managers.first { it.type == type }
                val loadingStatusState = mutableLoadingStatusFor(manager)
                launch {
                    //wait for loading stops
                    loadingStatusState.takeWhile { it == LoadingStatus.LOADING }.collect()
                    loadingStatusState.value = LoadingStatus.LOADING
                    loadAndSave(manager, userId)
                    loadingStatusState.value = LoadingStatus.PENDING
                    progress.value++
                }
            }.joinAll()
        }
    }

    private suspend fun<U: UserInfo> loadAndSave(manager: AccountManager<U>, userId: String) {
        manager.setSavedInfo(manager.loadInfo(userId))
    }
}

private fun getManager(resource: String, userName: String, link: String): Pair<AccountManagers, String>? =
    when (resource) {
        "codeforces.com" -> AccountManagers.codeforces to userName
        "atcoder.jp" -> AccountManagers.atcoder to userName
        "codechef.com" -> AccountManagers.codechef to userName
        "dmoj.ca" -> AccountManagers.dmoj to userName
        "acm.timus.ru", "timus.online" -> {
            val userId = link.substring(link.lastIndexOf('=')+1)
            AccountManagers.timus to userId
        }
        else -> null
    }