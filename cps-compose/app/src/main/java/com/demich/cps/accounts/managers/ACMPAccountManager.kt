package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.accounts.SmallAccountPanelTypeArchive
import com.demich.cps.utils.ACMPApi
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup

@Serializable
data class ACMPUserInfo(
    override val status: STATUS,
    val id: String,
    val userName: String = "",
    val rating: Int = 0,
    val solvedTasks: Int = 0,
    val rank: Int = 0
): UserInfo() {
    override val userId: String
        get() = id

    override fun link(): String = ACMPApi.urls.user(id.toInt())
}


class ACMPAccountManager(context: Context):
    AccountManager<ACMPUserInfo>(context, AccountManagers.acmp),
    AccountSuggestionsProvider
{

    companion object {
        private val Context.account_acmp_dataStore by preferencesDataStore(AccountManagers.acmp.name)
    }

    override val userIdTitle get() = "id"
    override val urlHomePage get() = ACMPApi.urls.main

    override fun isValidForUserId(char: Char): Boolean = char in '0'..'9'

    override fun emptyInfo() = ACMPUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String, flags: Int): ACMPUserInfo {
        try {
            return with(Jsoup.parse(ACMPApi.getUserPage(id = data.toInt()))) {
                val userName = title().trim()
                val box = body().select("h4").firstOrNull { it.text() == "Общая статистика" }?.parent()!!
                val bs = box.select("b.btext").map { it.text() }
                val solvedTasks = bs.first { it.startsWith("Решенные задачи") }.let {
                    it.substring(it.indexOf('(')+1, it.indexOf(')')).toInt()
                }
                val rating = bs.first { it.startsWith("Рейтинг:") }.let {
                    it.substring(it.indexOf(':')+2, it.indexOf('/')-1).toInt()
                }
                val rank = bs.first { it.startsWith("Место:") }.let {
                    it.substring(it.indexOf(':')+2, it.indexOf('/')-1).toInt()
                }
                ACMPUserInfo(
                    status = STATUS.OK,
                    id = data,
                    userName = userName,
                    rating = rating,
                    solvedTasks = solvedTasks,
                    rank = rank
                )
            }
        } catch (e: ACMPApi.ACMPPageNotFoundException) {
            return ACMPUserInfo(status = STATUS.NOT_FOUND, id = data)
        } catch (e: Throwable) {
            return ACMPUserInfo(status = STATUS.FAILED, id = data)
        }
    }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion> {
        if (str.toIntOrNull() != null) return emptyList()
        val s = ACMPApi.getUsersSearch(str)
        return Jsoup.parse(s).selectFirst("table.main")!!.select("tr.white").mapNotNull { row ->
            val cols = row.select("td")
            val userId = cols[1].selectFirst("a")!!
                .attr("href").removePrefix("/?main=user&id=")
            val userName = cols[1].text()
            val tasks = cols[3].text()
            AccountSuggestion(
                userId = userId,
                title = userName,
                info = tasks
            )
        }
    }

    @Composable
    override fun makeOKInfoSpan(userInfo: ACMPUserInfo): AnnotatedString =
        buildAnnotatedString {
            val words = userInfo.userName.split(' ')
            if (words.size < 3) append(userInfo.userName)
            else {
                append(words[0])
                for(i in 1 until words.size) append(" ${words[i][0]}.")
            }
            if (userInfo.solvedTasks > 0) append(" [${userInfo.solvedTasks} / ${userInfo.rating}]")
        }

    @Composable
    override fun Panel(userInfo: ACMPUserInfo) {
        if (userInfo.status == STATUS.OK) {
            SmallAccountPanelTypeArchive(
                title = userInfo.userName,
                infoArgs = listOf(
                    "solved" to userInfo.solvedTasks.toString(),
                    "rating" to userInfo.rating.toString(),
                    "rank" to userInfo.rank.toString()
                )
            )
        } else {
            SmallAccountPanelTypeArchive(
                title = userInfo.id,
                infoArgs = emptyList()
            )
        }
    }

    override fun getDataStore() = accountDataStore(context.account_acmp_dataStore)

}