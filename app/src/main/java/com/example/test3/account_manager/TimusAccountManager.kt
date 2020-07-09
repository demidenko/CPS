package com.example.test3.account_manager

import android.content.Context
import com.example.test3.utils.fromHTML
import com.example.test3.utils.readURLData

class TimusAccountManager(context: Context): AccountManager(context) {
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
    }

    override val PREFERENCES_FILE_NAME: String
        get() = preferences_file_name

    companion object{
        const val preferences_file_name = "timus"
        const val preferences_userid = "userid"
        const val preferences_username = "username"
        const val preferences_rating = "rating"
        const val preferences_count_of_solved_tasks = "count_of_solved_tasks"
        const val preferences_place_by_tasks = "place_tasks"
        const val preferences_place_by_rating = "place_rating"


        var __cachedInfo: TimusUserInfo? = null
    }

    override suspend fun downloadInfo(data: String): TimusUserInfo {
        val res = TimusUserInfo(STATUS.FAILED, data)
        val s = readURLData("https://timus.online/author.aspx?id=$data&locale=en") ?: return res
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
        TimusUserInfo(
            STATUS.valueOf(getString(preferences_status, null) ?: STATUS.FAILED.name),
            id = getString(preferences_userid, null) ?: "",
            userName = getString(preferences_username, null) ?: "",
            rating = getInt(preferences_rating, 0),
            solvedTasks = getInt(preferences_count_of_solved_tasks, 0),
            placeTasks = getInt(preferences_place_by_tasks, 0),
            placeRating = getInt(preferences_place_by_rating, 0)
        )
    }

    override fun writeInfo(info: UserInfo) = with(prefs.edit()){
        putString(preferences_status, info.status.name)
        info as TimusUserInfo
        putString(preferences_userid, info.id)
        putString(preferences_username, info.userName)
        putInt(preferences_rating, info.rating)
        putInt(preferences_count_of_solved_tasks, info.solvedTasks)
        putInt(preferences_place_by_tasks, info.placeTasks)
        putInt(preferences_place_by_rating, info.placeRating)
        commit()
    }

    override suspend fun loadSuggestions(str: String): List<Pair<String, String>>? {
        if(str.toIntOrNull()!=null) return null
        val s = readURLData("https://timus.online/search.aspx?Str=$str") ?: return null
        var i = s.indexOf("CLASS=\"ranklist\"")
        if(i==-1) return null
        val res = ArrayList<Pair<String,String>>()
        while(true){
            i = s.indexOf("<TD CLASS=\"name\">", i)
            if(i==-1) break
            i = s.indexOf("?id=", i+1)
            val userid = s.substring(i+4, s.indexOf('"',i))
            val username = fromHTML(s.substring(s.indexOf('>', i) + 1, s.indexOf("</A", i)))
            res += Pair("$username $userid", userid)
        }
        return res
    }
}