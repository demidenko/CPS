package com.demich.cps.profiles.managers

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.platforms.Platform
import com.demich.cps.platforms.api.clist.ClistUrls
import com.demich.cps.platforms.clients.ClistClient
import com.demich.cps.platforms.clients.isPageNotFound
import com.demich.cps.platforms.utils.ClistParser
import com.demich.cps.profiles.userinfo.ClistUserInfo
import com.demich.cps.profiles.userinfo.UserSuggestion
import com.demich.cps.ui.theme.CPSColors


class CListProfileManager :
    ProfileManager<ClistUserInfo>(),
    ProfileSuggestionsProvider
{
    override val platform: Platform get() = clist
    override val userIdTitle = "login"
    override val urlHomePage = ClistUrls.main

    override suspend fun getUserInfo(userId: String): ClistUserInfo? =
        try {
            ClistParser().extractUserInfo(
                source = ClistClient().getUserPage(login = userId),
                login = userId
            )
        } catch (it: Throwable) {
            if (it.isPageNotFound) null
            else throw it
        }

    override suspend fun getSuggestions(str: String): List<UserSuggestion> =
        ClistParser().extractLoginSuggestions(source = ClistClient().getUsersSearchPage(str))
            .map { UserSuggestion(userId = it) }

    override fun makeUserInfoSpan(userInfo: ClistUserInfo, cpsColors: CPSColors): AnnotatedString =
        with(userInfo) {
            AnnotatedString("$login (${accounts.size})")
        }

    override fun profileStorage(context: Context): ProfileStorage<ClistUserInfo> {
        error("CList profile manager can not provide data store")
    }

}