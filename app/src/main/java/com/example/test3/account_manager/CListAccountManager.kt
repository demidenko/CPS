package com.example.test3.account_manager

import android.content.Context
import com.example.test3.utils.CListAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CListAccountManager(context: Context) : AccountManager(context) {

    data class CListUserInfo(
        override var status: STATUS,
        var login: String,
        val accounts: MutableMap<String,Pair<String,String>> = mutableMapOf()
    ): UserInfo(){
        override val userID: String
            get() = login

        override fun makeInfoOKString(): String = "$login (${accounts.size})"

        override fun link(): String = "https://clist.by/coder/$login"

    }

    companion object {
        const val preferences_file_name = "clist"
    }

    override val PREFERENCES_FILE_NAME: String
        get() = preferences_file_name

    override var cachedInfo: UserInfo?
        get() = null
        set(value) {}

    override fun readInfo(): UserInfo = emptyInfo()

    override fun writeInfo(info: UserInfo): Boolean = false

    override fun emptyInfo() = CListUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String): UserInfo {
        val login = data
        val res = CListUserInfo(STATUS.FAILED, login)
        val response = CListAPI.getUser(login) ?: return res
        if(!response.isSuccessful){
            if(response.code() == 404) return res.apply{ status = STATUS.NOT_FOUND }
            return res
        }
        val s = response.body()?.string() ?: return res
        var i = 0
        while (true) {
            i = s.indexOf("<span class=\"account btn-group", i+1)
            if(i == -1) break
            var j = s.indexOf("<span class=", i+1)
            j = s.indexOf(">", j+1)
            val userName = s.substring(j+1, s.indexOf("</span",j+1))
            val r = s.indexOf("href=\"/resource/", j)
            val resource = s.substring(s.indexOf(">",r)+1, s.indexOf("</a",r))
            val l = s.lastIndexOf("fa-external-link-alt", r)
            val link = if (l!=-1 && l<r){
                s.substring(s.lastIndexOf("href=", l)+6, s.lastIndexOf("\" target=\"_blank\"", l))
            }else ""
            res.accounts[resource] = Pair(userName, link)
        }
        res.status = STATUS.OK
        return res
    }

    override suspend fun loadSuggestions(str: String): List<Triple<String, String, String>>?  = withContext(Dispatchers.IO) {
        val response = CListAPI.getUsersSearch(str) ?: return@withContext null
        val s = response.body()?.string() ?: return@withContext null
        val res = ArrayList<Triple<String,String, String>>()
        var i = 0
        while (true) {
            i = s.indexOf("<td class=\"username\">", i+1)
            if(i==-1) break
            var j = s.indexOf("<span", i)
            j = s.indexOf("<a href=", j)
            val login = s.substring(s.indexOf("\">",j)+2, s.indexOf("</a",j))
            res.add(Triple(login,"",login))
        }
        return@withContext res
    }

}