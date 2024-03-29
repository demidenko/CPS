package com.example.test3.account_manager

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.test3.utils.TimusAPI
import com.example.test3.utils.fromHTML
import kotlinx.serialization.Serializable

class TimusAccountManager(context: Context):
    AccountManager<TimusAccountManager.TimusUserInfo>(context, manager_name),
    AccountSuggestionsProvider
{

    companion object {
        const val manager_name = "timus"
        private val Context.account_timus_dataStore by preferencesDataStore(manager_name)
    }

    @Serializable
    data class TimusUserInfo(
        override var status: STATUS,
        var id: String,
        var userName: String = "",
        var rating: Int = 0,
        var solvedTasks: Int = 0,
        var rankTasks: Int = 0,
        var rankRating: Int = 0
    ): UserInfo() {
        override val userID: String
            get() = id

        override fun makeInfoOKString(): String {
            return "$userName [$solvedTasks / $rating]"
        }

        override fun link(): String = "https://timus.online/author.aspx?id=$id"
    }

    override val userIDName = "id"
    override val homeURL = "https://timus.online"

    override fun isValidForUserID(char: Char): Boolean {
        return char in '0'..'9'
    }

    override fun emptyInfo() = TimusUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String, flags: Int): TimusUserInfo {
        val res = TimusUserInfo(STATUS.FAILED, data)
        val s = TimusAPI.getUser(data) ?: return res
        var i = s.indexOf("<H2 CLASS=\"author_name\">")
        if(i==-1) return res.apply { status = STATUS.NOT_FOUND }
        i = s.indexOf("<TITLE>")
        res.userName = s.substring(s.indexOf('>',i)+1, s.indexOf('@',i)-1)

        i = s.indexOf("<TD CLASS=\"author_stats_value\"", i+1)
        if(i!=-1) {
            i = s.indexOf('>', i)
            res.rankTasks = s.substring(i + 1, s.indexOf(' ', i)).toInt()

            i = s.indexOf('>', s.indexOf("<TD CLASS=\"author_stats_value\"", i + 1))
            res.solvedTasks = s.substring(i + 1, s.indexOf(' ', i)).toInt()

            i = s.indexOf('>', s.indexOf("<TD CLASS=\"author_stats_value\"", i + 1))
            res.rankRating = s.substring(i + 1, s.indexOf(' ', i)).toInt()

            i = s.indexOf('>', s.indexOf("<TD CLASS=\"author_stats_value\"", i + 1))
            res.rating = s.substring(i + 1, s.indexOf(' ', i)).toInt()
        }

        res.status = STATUS.OK
        return res
    }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion>? {
        if(str.toIntOrNull()!=null) return null
        val s = TimusAPI.getUserSearch(str) ?: return null
        var i = s.indexOf("CLASS=\"ranklist\"")
        if(i==-1) return null
        val res = ArrayList<AccountSuggestion>()
        while(true){
            i = s.indexOf("<TD CLASS=\"name\">", i)
            if(i==-1) break
            i = s.indexOf("?id=", i+1)
            val userid = s.substring(i+4, s.indexOf('"',i))
            val username = fromHTML(s.substring(s.indexOf('>', i) + 1, s.indexOf("</A", i))).toString()
            i = s.indexOf("<TD>", i+1)
            i = s.indexOf("<TD>", i+1)
            val tasks = s.substring(i+4, s.indexOf("</TD",i))
            res += AccountSuggestion(username, tasks, userid)
        }
        return res
    }

    override fun getDataStore() = accountDataStore(context.account_timus_dataStore, emptyInfo())
}