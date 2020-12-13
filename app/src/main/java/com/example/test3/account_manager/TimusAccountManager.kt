package com.example.test3.account_manager

import android.content.Context
import com.example.test3.utils.TimusAPI
import com.example.test3.utils.fromHTML
import com.example.test3.utils.jsonCPS
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class TimusAccountManager(context: Context): AccountManager(context) {

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

    override val PREFERENCES_FILE_NAME: String
        get() = preferences_file_name

    companion object {
        const val preferences_file_name = "timus"
    }

    override fun emptyInfo() = TimusUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String): TimusUserInfo {
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

    override fun decodeFromString(str: String) = jsonCPS.decodeFromString<TimusUserInfo>(str)

    override fun encodeToString(info: UserInfo) = jsonCPS.encodeToString(info as TimusUserInfo)

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
}