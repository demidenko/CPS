package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.accounts.userinfo.ClistUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.platforms.api.ClistApi
import com.demich.cps.platforms.api.isPageNotFound
import com.demich.cps.platforms.utils.ClistUtils


class CListAccountManager(context: Context):
    AccountManager<ClistUserInfo>(context, AccountManagers.clist),
    UserSuggestionsProvider
{
    override val userIdTitle = "login"
    override val urlHomePage = ClistApi.urls.main

    override fun emptyInfo() = ClistUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String): ClistUserInfo =
        ClistUtils.runCatching {
            extractUserInfo(
                source = ClistApi.getUserPage(login = data),
                login = data
            )
        }.getOrElse { e ->
            if (e.isPageNotFound) ClistUserInfo(status = STATUS.NOT_FOUND, login = data)
            else ClistUserInfo(status = STATUS.FAILED, login = data)
        }

    override suspend fun loadSuggestions(str: String): List<UserSuggestion> =
        ClistUtils.extractLoginSuggestions(source = ClistApi.getUsersSearchPage(str))
            .map { UserSuggestion(userId = it) }

    @Composable
    override fun makeOKInfoSpan(userInfo: ClistUserInfo) = with(userInfo) {
        AnnotatedString("$login (${accounts.size})")
    }

    override fun getDataStore(): AccountDataStore<ClistUserInfo> {
        throw IllegalAccessException("CList account manager can not provide data store")
    }

}