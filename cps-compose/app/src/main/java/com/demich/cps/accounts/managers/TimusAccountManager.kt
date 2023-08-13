package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.accounts.SmallAccountPanelTypeArchive
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.accounts.userinfo.TimusUserInfo
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.platforms.api.TimusApi
import com.demich.cps.platforms.utils.TimusUtils


class TimusAccountManager(context: Context):
    AccountManager<TimusUserInfo>(context, AccountManagers.timus),
    UserSuggestionsProvider
{
    override val userIdTitle get() = "id"
    override val urlHomePage get() = TimusApi.urls.main

    override fun isValidForUserId(char: Char): Boolean = char in '0'..'9'
    override fun isValidForSearch(char: Char): Boolean = true

    override suspend fun downloadInfo(data: String): TimusUserInfo =
        TimusUtils.runCatching {
            extractUserInfo(
                source = TimusApi.getUserPage(data.toInt()),
                handle = data
            )
        }.getOrElse {
            TimusUserInfo(status = STATUS.FAILED, id = data)
        }

    override suspend fun loadSuggestions(str: String): List<UserSuggestion> {
        if (str.toIntOrNull() != null) return emptyList()
        return TimusUtils.extractUsersSuggestions(source = TimusApi.getSearchPage(str))
    }

    @Composable
    override fun makeOKInfoSpan(userInfo: TimusUserInfo) = with(userInfo) {
        AnnotatedString("$userName $solvedTasks")
    }

    @Composable
    override fun PanelContent(userInfo: TimusUserInfo) {
        if (userInfo.status == STATUS.OK) {
            SmallAccountPanelTypeArchive(
                title = userInfo.userName,
                infoArgs = listOf(
                    "solved" to userInfo.solvedTasks.toString(),
                    "rank" to userInfo.rankTasks.toString()
                )
            )
        } else {
            SmallAccountPanelTypeArchive(
                title = userInfo.id,
                infoArgs = emptyList()
            )
        }
    }

    override fun getDataStore() = simpleAccountDataStore(context)
}