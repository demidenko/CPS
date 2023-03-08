package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.accounts.SmallAccountPanelTypeArchive
import com.demich.cps.utils.TimusUtils
import com.demich.cps.data.api.TimusApi
import com.demich.datastore_itemized.dataStoreWrapper
import kotlinx.serialization.Serializable


@Serializable
data class TimusUserInfo(
    override val status: STATUS,
    val id: String,
    val userName: String = "",
    val rating: Int = 0,
    val solvedTasks: Int = 0,
    val rankTasks: Int = 0,
    val rankRating: Int = 0
): UserInfo() {
    override val userId: String
        get() = id

    override val userPageUrl: String
        get() = TimusApi.urls.user(id.toInt())
}

class TimusAccountManager(context: Context):
    AccountManager<TimusUserInfo>(context, AccountManagers.timus),
    AccountSuggestionsProvider
{
    companion object {
        private val Context.account_timus_dataStore by dataStoreWrapper(AccountManagers.timus.name)
    }

    override val userIdTitle get() = "id"
    override val urlHomePage get() = TimusApi.urls.main

    override fun isValidForUserId(char: Char): Boolean = char in '0'..'9'
    override fun isValidForSearch(char: Char): Boolean = true

    override fun emptyInfo() = TimusUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String): TimusUserInfo =
        TimusUtils.runCatching {
            extractUserInfo(
                source = TimusApi.getUserPage(data.toInt()),
                handle = data
            )
        }.getOrElse {
            TimusUserInfo(status = STATUS.FAILED, id = data)
        }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion> {
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

    override fun getDataStore() = accountDataStore(context.account_timus_dataStore)
}