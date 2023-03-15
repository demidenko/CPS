package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.accounts.userinfo.ClistUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.utils.CListUtils
import com.demich.cps.data.api.ClistApi
import com.demich.cps.data.api.isPageNotFound


class CListAccountManager(context: Context):
    AccountManager<ClistUserInfo>(context, AccountManagers.clist),
    AccountSuggestionsProvider
{
    override val userIdTitle = "login"
    override val urlHomePage = ClistApi.urls.main

    override fun emptyInfo() = ClistUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String): ClistUserInfo =
        CListUtils.runCatching {
            extractUserInfo(
                source = ClistApi.getUserPage(login = data),
                login = data
            )
        }.getOrElse { e ->
            if (e.isPageNotFound) ClistUserInfo(status = STATUS.NOT_FOUND, login = data)
            else ClistUserInfo(status = STATUS.FAILED, login = data)
        }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion> =
        CListUtils.extractLoginSuggestions(source = ClistApi.getUsersSearchPage(str))
            .map { AccountSuggestion(userId = it) }

    @Composable
    override fun makeOKInfoSpan(userInfo: ClistUserInfo) = with(userInfo) {
        AnnotatedString("$login (${accounts.size})")
    }

    override fun getDataStore(): AccountDataStore<ClistUserInfo> {
        throw IllegalAccessException("CList account manager can not provide data store")
    }

}