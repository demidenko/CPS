package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.accounts.SmallAccountPanelTypeArchive
import com.demich.cps.utils.TimusAPI
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup


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
            with(Jsoup.parse(s)) {
                val userName = selectFirst("h2.author_name")?.text()
                    ?: return TimusUserInfo(status = STATUS.NOT_FOUND, id = data)
                val rows = select("td.author_stats_value").map { row ->
                    row.text().let { it.substring(0, it.indexOf(" out of ")) }
                }
                return TimusUserInfo(
                    status = STATUS.OK,
                    id = data,
                    userName = userName,
                    rating = rows[3].toInt(),
                    solvedTasks = rows[1].toInt(),
                    rankTasks = rows[0].toInt(),
                    rankRating = rows[2].toInt()
                )
            }
        } catch (e: Throwable) {
            return TimusUserInfo(status = STATUS.FAILED, id = data)
        }
    }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion>? {
        if (str.toIntOrNull() != null) return emptyList()
        try {
            return Jsoup.parse(TimusAPI.getSearchPage(str))
                .selectFirst("table.ranklist")
                ?.select("td.name")
                ?.mapNotNull { nameColumn ->
                    val userId = nameColumn.selectFirst("a")
                        ?.attr("href")
                        ?.let {
                            it.substring(it.indexOf("id=")+3)
                        } ?: return@mapNotNull null
                    val tasks = nameColumn.nextElementSibling()?.nextElementSibling()?.text() ?: ""
                    AccountSuggestion(
                        userId = userId,
                        title = nameColumn.text(),
                        info = tasks
                    )
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

    override fun getDataStore() = accountDataStore(context.account_timus_dataStore)
}