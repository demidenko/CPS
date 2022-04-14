package com.demich.cps.accounts

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.accounts.managers.*
import com.demich.cps.ui.bottomprogressbar.ProgressBarInfo
import com.demich.cps.ui.bottomprogressbar.ProgressBarsViewModel
import com.demich.cps.utils.CListUtils
import com.demich.cps.utils.LoadingStatus
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch


class AccountsViewModel: ViewModel() {
    private val loadingStatuses: MutableMap<AccountManagers, MutableState<LoadingStatus>> = mutableMapOf()

    fun loadingStatusFor(manager: AccountManager<*>): MutableState<LoadingStatus> =
        loadingStatuses.getOrPut(manager.type) { mutableStateOf(LoadingStatus.PENDING) }


    fun<U: UserInfo> reload(manager: AccountManager<U>) {
        viewModelScope.launch {
            val savedInfo = manager.getSavedInfo()
            if (savedInfo.isEmpty()) return@launch

            var loadingStatus by loadingStatusFor(manager)
            if (loadingStatus == LoadingStatus.LOADING) return@launch

            loadingStatus = LoadingStatus.LOADING
            val info = manager.loadInfo(savedInfo.userId, 1)

            if (info.status == STATUS.FAILED) {
                loadingStatus = LoadingStatus.FAILED
            } else {
                loadingStatus = LoadingStatus.PENDING
                manager.setSavedInfo(info)
            }
        }
    }

    fun<U: UserInfo> delete(manager: AccountManager<U>) {
        viewModelScope.launch {
            var loadingStatus by loadingStatusFor(manager)
            require(loadingStatus != LoadingStatus.LOADING)
            loadingStatus = LoadingStatus.PENDING
            manager.setSavedInfo(manager.emptyInfo())
        }
    }

    fun runClistImport(
        cListUserInfo: CListUserInfo,
        progressBarsViewModel: ProgressBarsViewModel,
        context: Context
    ) {
        progressBarsViewModel.doJob(id = clistImportId, coroutineScope = viewModelScope) { progress ->
            val supported = cListUserInfo.accounts.mapNotNull { (resource, userData) ->
                CListUtils.getManager(resource, userData.first, userData.second)
            }
            progress.value = ProgressBarInfo(title = "clist import", total = supported.size)
            val managers = context.allAccountManagers
            supported.map { (type, userId) ->
                val manager = managers.first { it.type == type }
                var loadingStatus by loadingStatusFor(manager)
                launch {
                    //TODO: what if already loading??
                    loadingStatus = LoadingStatus.LOADING
                    loadAndSave(manager, userId)
                    loadingStatus = LoadingStatus.PENDING
                    progress.value++
                }
            }.joinAll()
        }
    }

    private suspend fun<U: UserInfo> loadAndSave(manager: AccountManager<U>, userId: String) {
        manager.setSavedInfo(manager.loadInfo(userId))
    }

    companion object {
        const val clistImportId = "clist_import"
    }
}