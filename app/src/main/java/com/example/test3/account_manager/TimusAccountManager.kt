package com.example.test3.account_manager

import android.content.Context
import com.example.test3.utils.TimusAPI
import com.example.test3.utils.fromHTML
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TimusAccountManager(context: Context): AccountManager(context) {

    @Serializable
    data class TimusUserInfo(
        override var status: STATUS,
        var id: String,
        var userName: String = "",
        var rating: Int = 0,
        var solvedTasks: Int = 0,
        var placeTasks: Int = 0,
        var placeRating: Int = 0
    ): UserInfo() {
        override val userID: String
            get() = id

        override fun makeInfoOKString(): String {
            return "$userName [$solvedTasks / $rating]"
        }

        override fun link(): String = "https://timus.online/author.aspx?id=$id"
    }

    override val PREFERENCES_FILE_NAME: String
        get() = preferences_file_name

    companion object{
        const val preferences_file_name = "timus"

        var __cachedInfo: TimusUserInfo? = null
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
            res.placeTasks = s.substring(i + 1, s.indexOf(' ', i)).toInt()

            i = s.indexOf('>', s.indexOf("<TD CLASS=\"author_stats_value\"", i + 1))
            res.solvedTasks = s.substring(i + 1, s.indexOf(' ', i)).toInt()

            i = s.indexOf('>', s.indexOf("<TD CLASS=\"author_stats_value\"", i + 1))
            res.placeRating = s.substring(i + 1, s.indexOf(' ', i)).toInt()

            i = s.indexOf('>', s.indexOf("<TD CLASS=\"author_stats_value\"", i + 1))
            res.rating = s.substring(i + 1, s.indexOf(' ', i)).toInt()
        }

        res.status = STATUS.OK
        return res
    }

    override var cachedInfo: UserInfo?
        get() = __cachedInfo
        set(value) { __cachedInfo = value as TimusUserInfo }

    override fun readInfo(): TimusUserInfo = with(prefs) {
        val str = getString(preferences_key_user_info, null) ?: return@with emptyInfo().apply { status = STATUS.FAILED }
        Json.decodeFromString(str)
    }

    override fun writeInfo(info: UserInfo) = with(prefs.edit()){
        info as TimusUserInfo
        putString(preferences_key_user_info, Json.encodeToString(info))
        commit()
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
}