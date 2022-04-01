package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.accounts.SmallAccountPanelTypeArchive
import com.demich.cps.utils.TimusAPI
import com.demich.cps.utils.fromHTML
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

    override fun link(): String = TimusAPI.URLFactory.user(id.toInt())
}

class TimusAccountManager(context: Context):
    AccountManager<TimusUserInfo>(context, AccountManagers.timus),
    AccountSuggestionsProvider
{
    companion object {
        private val Context.account_timus_dataStore by preferencesDataStore(AccountManagers.timus.name)
    }

    override val userIdTitle get() = "id"
    override val urlHomePage get() = TimusAPI.URLFactory.main

    override fun isValidForUserId(char: Char): Boolean = char in '0'..'9'
    override fun isValidForSearch(char: Char): Boolean = true

    override fun emptyInfo() = TimusUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String, flags: Int): TimusUserInfo {
        try {
            val s = TimusAPI.getUserPage(data.toInt())
            var i = s.indexOf("<H2 CLASS=\"author_name\">")
            if (i == -1) return TimusUserInfo(status = STATUS.NOT_FOUND, id = data)
            i = s.indexOf("<TITLE>")
            val userName = s.substring(s.indexOf('>',i)+1, s.indexOf('@',i)-1)
            i = s.indexOf("<TD CLASS=\"author_stats_value\"", i+1)
            require(i != -1)
            i = s.indexOf('>', i)
            val rankTasks = s.substring(i + 1, s.indexOf(' ', i)).toInt()
            i = s.indexOf('>', s.indexOf("<TD CLASS=\"author_stats_value\"", i + 1))
            val solvedTasks = s.substring(i + 1, s.indexOf(' ', i)).toInt()
            i = s.indexOf('>', s.indexOf("<TD CLASS=\"author_stats_value\"", i + 1))
            val rankRating = s.substring(i + 1, s.indexOf(' ', i)).toInt()
            i = s.indexOf('>', s.indexOf("<TD CLASS=\"author_stats_value\"", i + 1))
            val rating = s.substring(i + 1, s.indexOf(' ', i)).toInt()
            return TimusUserInfo(
                status = STATUS.OK,
                id = data,
                userName = userName,
                rating = rating,
                solvedTasks = solvedTasks,
                rankTasks = rankTasks,
                rankRating = rankRating
            )
        } catch (e: Throwable) {
            return TimusUserInfo(status = STATUS.FAILED, id = data)
        }
    }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion>? {
        if (str.toIntOrNull() != null) return emptyList()
        try {
            val s = TimusAPI.getSearchPage(str)
            return buildList {
                var i = s.indexOf("CLASS=\"ranklist\"")
                require(i != -1)
                while(true) {
                    i = s.indexOf("<TD CLASS=\"name\">", i)
                    if (i == -1) break
                    i = s.indexOf("?id=", i+1)
                    val userid = s.substring(i+4, s.indexOf('"',i))
                    val username = fromHTML(s.substring(s.indexOf('>', i) + 1, s.indexOf("</A", i))).toString()
                    i = s.indexOf("<TD>", i+1)
                    i = s.indexOf("<TD>", i+1)
                    val tasks = s.substring(i+4, s.indexOf("</TD",i))
                    add(AccountSuggestion(username, tasks, userid))
                }
            }
        } catch (e: Throwable) {
            return null
        }
    }

    @Composable
    override fun makeOKInfoSpan(userInfo: TimusUserInfo): AnnotatedString =
        buildAnnotatedString {
            require(userInfo.status == STATUS.OK)
            append(userInfo.userName)
            append(' ')
            append(userInfo.solvedTasks.toString())
        }

    @Composable
    override fun Panel(userInfo: TimusUserInfo) {
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

    override fun getDataStore() = accountDataStore(context.account_timus_dataStore, emptyInfo())
}