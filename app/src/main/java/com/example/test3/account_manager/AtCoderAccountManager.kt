package com.example.test3.account_manager

import android.content.Context
import com.example.test3.utils.AtCoderAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AtCoderAccountManager(context: Context): AccountManager(context) {

    @Serializable
    data class AtCoderUserInfo(
        override var status: STATUS,
        var handle: String,
        var rating: Int = NOT_RATED
    ): UserInfo(){
        override val userID: String
            get() = handle

        override fun makeInfoOKString(): String {
            return if(rating == NOT_RATED) "$handle [not rated]" else "$handle $rating"
        }

        override fun link(): String = "https://atcoder.jp/users/$handle"
    }

    override val PREFERENCES_FILE_NAME: String
        get() = preferences_file_name

    companion object : ColoredHandles {
        const val preferences_file_name = "atcoder"

        var __cachedInfo: AtCoderUserInfo? = null



        override fun getHandleColor(rating: Int): HandleColor {
            return when{
                rating < 400 -> HandleColor.GRAY
                rating < 800 -> HandleColor.BROWN
                rating < 1200 -> HandleColor.GREEN
                rating < 1600 -> HandleColor.CYAN
                rating < 2000 -> HandleColor.BLUE
                rating < 2400 -> HandleColor.YELLOW
                rating < 2800 -> HandleColor.ORANGE
                else -> HandleColor.RED
            }
        }

        override fun getColor(tag: HandleColor): Int {
            return when(tag){
                HandleColor.GRAY -> 0x808080
                HandleColor.BROWN -> 0x804000
                HandleColor.GREEN -> 0x008000
                HandleColor.CYAN -> 0x00C0C0
                HandleColor.BLUE -> 0x0000FF
                HandleColor.YELLOW -> 0xC0C000
                HandleColor.ORANGE -> 0xFF8000
                HandleColor.RED -> 0xFF0000
                else -> throw HandleColor.UnknownHandleColorException(tag)
            }
        }
    }

    override fun emptyInfo() = AtCoderUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String): UserInfo {
        val handle = data
        val res = AtCoderUserInfo(STATUS.FAILED, handle)
        val response = AtCoderAPI.getUser(handle) ?: return res
        if(!response.isSuccessful){
            if(response.code() == 404) return res.apply{ status = STATUS.NOT_FOUND }
            return res
        }
        val s = response.body()?.string() ?: return res
        var i = s.lastIndexOf("class=\"username\"")
        i = s.indexOf("</span", i)
        res.handle = s.substring(s.lastIndexOf('>',i)+1, i)
        i = s.indexOf("<th class=\"no-break\">Rating</th>")
        if(i!=-1){
            i = s.indexOf("</span", i)
            res.rating = s.substring(s.lastIndexOf('>',i)+1, i).toInt()
        }else res.rating = NOT_RATED
        res.status = STATUS.OK
        return res
    }

    override var cachedInfo: UserInfo?
        get() = __cachedInfo
        set(value) { __cachedInfo = value as AtCoderUserInfo }

    override fun readInfo(): AtCoderUserInfo = with(prefs){
        val str = getString(preferences_key_user_info, null) ?: return@with emptyInfo().apply { status = STATUS.FAILED }
        Json.decodeFromString(str)
    }

    override fun writeInfo(info: UserInfo) = with(prefs.edit()){
        info as AtCoderUserInfo
        putString(preferences_key_user_info, Json.encodeToString(info))
        commit()
    }

    override fun getColor(info: UserInfo): Int?  = with(info as AtCoderUserInfo){
        if(status != STATUS.OK || rating == NOT_RATED) return null
        return getHandleColor(info.rating).getARGB(Companion)
    }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion>? = withContext(Dispatchers.IO) {
        val response = AtCoderAPI.getRankingSearch("*$str*") ?: return@withContext null
        val s = response.body()?.string() ?: return@withContext null
        val res = ArrayList<AccountSuggestion>()
        var i = s.indexOf("<div class=\"table-responsive\">")
        while(true){
            i = s.indexOf("<td class=\"no-break\">", i+1)
            if(i==-1) break
            var j = s.indexOf("</span></a>", i)
            val handle = s.substring(s.lastIndexOf('>', j-1)+1, j)
            j = s.indexOf("<td", j+1)
            j = s.indexOf("<td", j+1)
            val rating = s.substring(s.indexOf("<b>",j)+3, s.indexOf("</b>",j))
            res += AccountSuggestion(handle, rating, handle)
        }
        return@withContext res
    }
}