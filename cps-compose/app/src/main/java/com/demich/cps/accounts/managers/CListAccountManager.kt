package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.utils.CListApi
import com.demich.cps.utils.CListUtils
import com.demich.cps.utils.isPageNotFound


data class CListUserInfo(
    override val status: STATUS,
    val login: String,
    val accounts: Map<String, Pair<String, String>> = emptyMap()
): UserInfo() {
    override val userId: String
        get() = login

    override val userPageUrl: String
        get() = CListApi.urls.user(login)
}

class CListAccountManager(context: Context):
    AccountManager<CListUserInfo>(context, AccountManagers.clist),
    AccountSuggestionsProvider
{
    override val userIdTitle = "login"
    override val urlHomePage = CListApi.urls.main

    override fun emptyInfo() = CListUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String): CListUserInfo =
        CListUtils.runCatching {
            extractUserInfo(
                source = CListApi.getUserPage(login = data),
                login = data
            )
        }.getOrElse { e ->
            if (e.isPageNotFound) CListUserInfo(status = STATUS.NOT_FOUND, login = data)
            else CListUserInfo(status = STATUS.FAILED, login = data)
        }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion> =
        CListUtils.extractLoginSuggestions(source = CListApi.getUsersSearchPage(str))
            .map { AccountSuggestion(userId = it) }

    @Composable
    override fun makeOKInfoSpan(userInfo: CListUserInfo) = with(userInfo) {
        AnnotatedString("$login (${accounts.size})")
    }

    override fun getDataStore(): AccountDataStore<CListUserInfo> {
        throw IllegalAccessException("CList account manager can not provide data store")
    }

}