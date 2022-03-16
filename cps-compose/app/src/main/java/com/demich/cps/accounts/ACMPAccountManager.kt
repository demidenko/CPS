package com.demich.cps.accounts

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.utils.ACMPAPI
import com.demich.cps.utils.ACMPUtils
import kotlinx.serialization.Serializable

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

    override fun link(): String = ACMPUtils.URLFactory.user(id.toInt())
}


class ACMPAccountManager(context: Context):
    AccountManager<ACMPUserInfo>(context, manager_name),
    AccountSuggestionsProvider
{

    companion object {
        const val manager_name = "acmp"
        private val Context.account_acmp_dataStore by preferencesDataStore(manager_name)
    }

    override val userIdTitle get() = "id"
    override val urlHomePage = "https://acmp.ru"

    override fun isValidForUserId(char: Char): Boolean = char in '0'..'9'

    override fun emptyInfo() = ACMPUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String, flags: Int): ACMPUserInfo {
        try {
            val s = ACMPAPI.getUserPage(id = data.toInt())
            var i = s.indexOf("<title>")
            require(i != -1)
            val userName = s.substring(s.indexOf('>',i)+1, s.indexOf("</title>")).trim()
            i = s.indexOf("Решенные задачи")
            require(i != -1)
            i = s.indexOf('(', i)
            val solvedTasks = s.substring(i+1, s.indexOf(')',i)).toInt()
            i = s.indexOf("<b class=btext>Рейтинг:")
            require(i != -1)
            i = s.indexOf(':', i)
            val rating = s.substring(i+2, s.indexOf('/', i)-1).toInt()
            i = s.lastIndexOf("<b class=btext>Место:", i)
            i = s.indexOf(':', i)
            val rank = s.substring(i+2, s.indexOf('/', i)-1).toInt()
            return ACMPUserInfo(
                status = STATUS.OK,
                id = data,
                userName = userName,
                rating = rating,
                solvedTasks = solvedTasks,
                rank = rank
            )
        } catch (e: ACMPAPI.ACMPPageNotFoundException) {
            return ACMPUserInfo(status = STATUS.NOT_FOUND, id = data)
        } catch (e: Throwable) {
            return ACMPUserInfo(status = STATUS.FAILED, id = data)
        }
    }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion>? {
        if (str.toIntOrNull() != null) return emptyList()
        try {
            val s = ACMPAPI.getUsersSearch(str)
            return buildList {
                var k = s.indexOf("<table cellspacing=1 cellpadding=2 align=center class=main>")
                while(true) {
                    k = s.indexOf("<tr class=white>", k+1)
                    if (k == -1) break
                    var i = s.indexOf("<td>", k+1)
                    i = s.indexOf('>',i+4)
                    val userId = s.substring(s.lastIndexOf("id=",i)+3, i)
                    val userName = s.substring(i+1, s.indexOf("</a",i))
                    i = s.indexOf("<td align=right>", i)
                    i = s.indexOf("</a></td>", i)
                    val tasks = s.substring(s.lastIndexOf('>',i)+1, i)
                    add(AccountSuggestion(userName, tasks, userId))
                }
            }
        } catch (e: Throwable) {
            return null
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
            if (userInfo.solvedTasks > 0) {
                append(' ')
                append("[${userInfo.solvedTasks} / ${userInfo.rating}]")
            }
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

    override fun getDataStore() = accountDataStore(context.account_acmp_dataStore, emptyInfo())

}