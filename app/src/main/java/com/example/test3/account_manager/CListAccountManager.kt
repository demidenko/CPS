package com.example.test3.account_manager

import android.content.Context
import com.example.test3.utils.CListAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CListAccountManager(context: Context) : AccountManager<CListAccountManager.CListUserInfo>(context, preferences_file_name) {

    companion object {
        const val preferences_file_name = "clist"
    }

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

    override val userIDName = "login"
    override val homeURL = "https://clist.by"

    override fun decodeFromString(str: String) = emptyInfo()

    override fun encodeToString(info: CListUserInfo) = ""

    override fun emptyInfo() = CListUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String, flags: Int): CListUserInfo {
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
            var j = s.indexOf("href=\"/resource/", i)
            val l = s.indexOf("\"", s.indexOf("title=\"", j))
            val resource = s.substring(l+1, s.indexOf("\"",l+1)).removeSuffix("/")
            j = s.indexOf("<span class=", j+1)
            var r = s.indexOf("</span>", j)
            val userName = s.substring(s.indexOf(">", j)+1, r)
            j = s.indexOf("fa-external-link-alt", j)
            r = s.indexOf("</span>", r+1)
            val link = if (j!=-1 && j<r){
                s.substring(s.lastIndexOf("href=", j)+6, s.lastIndexOf("\" target=\"_blank\"", j))
            }else ""
            res.accounts[resource] = Pair(userName, link)
        }
        res.status = STATUS.OK
        return res
    }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion>?  = withContext(Dispatchers.IO) {
        val response = CListAPI.getUsersSearch(str) ?: return@withContext null
        val s = response.body()?.string() ?: return@withContext null
        val res = ArrayList<AccountSuggestion>()
        var i = 0
        while (true) {
            i = s.indexOf("<td class=\"username\">", i+1)
            if(i==-1) break
            var j = s.indexOf("<span", i)
            j = s.indexOf("<a href=", j)
            val login = s.substring(s.indexOf("\">",j)+2, s.indexOf("</a",j))
            res.add(AccountSuggestion(login,"",login))
        }
        return@withContext res
    }

    override fun getDataStore(): AccountDataStore {
        throw IllegalAccessException("CList account manager can not provide data store")
    }

}