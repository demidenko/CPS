package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.accounts.userinfo.ClistUserInfo
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.platforms.api.clients.ClistClient
import com.demich.cps.platforms.api.clients.ClistUrls
import com.demich.cps.platforms.api.isPageNotFound
import com.demich.cps.platforms.utils.ClistUtils
import com.demich.cps.ui.theme.CPSColors


class CListAccountManager :
    AccountManager<ClistUserInfo>(),
    ProfileSuggestionsProvider
{
    override val type get() = AccountManagerType.clist
    override val userIdTitle = "login"
    override val urlHomePage = ClistUrls.main

    override suspend fun fetchProfile(data: String): ProfileResult<ClistUserInfo> =
        ClistUtils.runCatching {
            ProfileResult.Success(
                userInfo = extractUserInfo(
                    source = ClistClient.getUserPage(login = data),
                    login = data
                )
            )
        }.getOrElse { e ->
            if (e.isPageNotFound) ProfileResult.NotFound(data)
            else ProfileResult.Failed(data)
        }

    override suspend fun fetchSuggestions(str: String): List<UserSuggestion> =
        ClistUtils.extractLoginSuggestions(source = ClistClient.getUsersSearchPage(str))
            .map { UserSuggestion(userId = it) }

    override fun makeUserInfoSpan(userInfo: ClistUserInfo, cpsColors: CPSColors): AnnotatedString =
        with(userInfo) {
            AnnotatedString("$login (${accounts.size})")
        }

    override fun dataStore(context: Context): ProfileDataStore<ClistUserInfo> {
        error("CList account manager can not provide data store")
    }

}