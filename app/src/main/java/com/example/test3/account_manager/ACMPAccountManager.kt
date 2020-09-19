package com.example.test3.account_manager

import android.content.Context
import com.example.test3.utils.ACMPAPI

class ACMPAccountManager(context: Context): AccountManager(context) {
    data class ACMPUserInfo(
        override var status: STATUS,
        var id: String,
        var userName: String = "",
        var rating: Int = 0,
        var solvedTasks: Int = 0,
        var place: Int = 0
    ): UserInfo() {
        override val userID: String
            get() = id

        override fun makeInfoOKString(): String {
            return "$userName [$solvedTasks / $rating]"
        }

        override fun link(): String = "https://acmp.ru/index.asp?main=user&id=$id"
    }



    override val PREFERENCES_FILE_NAME: String
        get() = preferences_file_name

    companion object{
        const val preferences_file_name = "acmp"
        const val preferences_userid = "userid"
        const val preferences_username = "username"
        const val preferences_rating = "rating"
        const val preferences_count_of_solved_tasks = "count_of_solved_tasks"
        const val preferences_place = "place"


        var __cachedInfo: ACMPUserInfo? = null
    }

    override suspend fun downloadInfo(data: String): ACMPUserInfo {
        val res = ACMPUserInfo(STATUS.FAILED, data)
        val s = ACMPAPI.getUser(data) ?: return res
        if(!s.contains("index.asp?main=status&id_mem=$data")) return res.apply { status = STATUS.NOT_FOUND }
        var i = s.indexOf("<title>")
        if(i!=-1){
            res.userName = s.substring(s.indexOf('>',i)+1, s.indexOf("</title>"))
        }
        i = s.indexOf("Решенные задачи")
        if(i!=-1){
            i = s.indexOf('(', i)
            res.solvedTasks = s.substring(i+1, s.indexOf(')',i)).toInt()
        }
        i = s.indexOf("<b class=btext>Рейтинг:")
        if(i!=-1){
            i = s.indexOf(':', i)
            res.rating = s.substring(i+2, s.indexOf('/', i)-1).toInt()
            i = s.lastIndexOf("<b class=btext>Место:", i)
            i = s.indexOf(':', i)
            res.place = s.substring(i+2, s.indexOf('/', i)-1).toInt()
        }
        res.status = STATUS.OK
        return res
    }

    override var cachedInfo: UserInfo?
        get() = __cachedInfo
        set(value) { __cachedInfo = value as ACMPUserInfo }

    override fun readInfo(): ACMPUserInfo = with(prefs){
        ACMPUserInfo(
            STATUS.valueOf(getString(preferences_status, null) ?: STATUS.FAILED.name),
            id = getString(preferences_userid, null) ?: "",
            userName = getString(preferences_username, null) ?: "",
            rating = getInt(preferences_rating, 0),
            solvedTasks = getInt(preferences_count_of_solved_tasks, 0),
            place = getInt(preferences_place, 0)
        )
    }

    override fun writeInfo(info: UserInfo) = with(prefs.edit()){
        putString(preferences_status, info.status.name)
        info as ACMPUserInfo
        putString(preferences_userid, info.id)
        putString(preferences_username, info.userName)
        putInt(preferences_rating, info.rating)
        putInt(preferences_count_of_solved_tasks, info.solvedTasks)
        putInt(preferences_place, info.place)
        commit()
    }

    override suspend fun loadSuggestions(str: String): List<Triple<String, String, String>>? {
        if(str.toIntOrNull()!=null) return null
        val s = ACMPAPI.getUserSearch(str) ?: return null
        val res = ArrayList<Triple<String,String,String>>()
        var k = s.indexOf("<table cellspacing=1 cellpadding=2 align=center class=main>")
        while(true){
            k = s.indexOf("<tr class=white>", k+1)
            if(k==-1) break
            var i = s.indexOf("<td>", k+1)
            i = s.indexOf('>',i+4)
            val userid = s.substring(s.lastIndexOf("id=",i)+3, i)
            val username = s.substring(i+1, s.indexOf("</a",i))
            i = s.indexOf("<td align=right>", i)
            i = s.indexOf("</a></td>", i)
            val tasks = s.substring(s.lastIndexOf('>',i)+1, i)
            res += Triple(username, tasks, userid)
        }
        return res
    }
}