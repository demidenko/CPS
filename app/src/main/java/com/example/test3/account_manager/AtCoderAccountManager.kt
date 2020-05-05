package com.example.test3.account_manager

import androidx.appcompat.app.AppCompatActivity
import com.example.test3.readURLData

class AtCoderAccountManager(activity: AppCompatActivity): AccountManager(activity), ColoredHandles{

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
    }

    override val PREFERENCES_FILE_NAME: String
        get() = preferences_file_name

    companion object{
        const val preferences_file_name = "atcoder"
        const val preferences_handle = "handle"
        const val preferences_rating = "rating"

        var __cachedInfo: AtCoderUserInfo? = null
    }


    override suspend fun downloadInfo(data: String): UserInfo {
        val handle = data
        val res = AtCoderUserInfo(STATUS.FAILED, handle)
        val s = readURLData("https://atcoder.jp/users/$handle") ?: return res
        var i = s.lastIndexOf("class=\"username\"")
        if(i==-1) return res.apply { status = STATUS.NOT_FOUND }
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
        AtCoderUserInfo(
            STATUS.valueOf(getString(preferences_status, null) ?: STATUS.FAILED.name),
            handle = getString(preferences_handle, null) ?: "",
            rating = getInt(preferences_rating, NOT_RATED)
        )
    }

    override fun writeInfo(info: UserInfo) = with(prefs.edit()){
        putString(preferences_status, info.status.name)
        info as AtCoderUserInfo
        putString(preferences_handle, info.handle)
        putInt(preferences_rating, info.rating)
        commit()
    }

    override fun getColor(info: UserInfo): Int?  = with(info as AtCoderUserInfo){
        if(status != STATUS.OK || rating == NOT_RATED) return null
        return getHandleColor(info.rating).getARGB(this@AtCoderAccountManager)
    }

    override suspend fun loadSuggestions(str: String): List<Pair<String, String>>? {
        val s = readURLData("https://atcoder.jp/ranking/all?f.UserScreenName=*$str*") ?: return null
        val res = ArrayList<Pair<String, String>>()
        var i = s.indexOf("<div class=\"table-responsive\">")
        while(true){
            i = s.indexOf("<td class=\"no-break\">", i+1)
            if(i==-1) break
            var j = s.indexOf("</span></a>", i)
            val handle = s.substring(s.lastIndexOf('>', j-1)+1, j)
            j = s.indexOf("<td", j+1)
            j = s.indexOf("<td", j+1)
            val rating = s.substring(s.indexOf("<b>",j)+3, s.indexOf("</b>",j))
            res += Pair("$handle $rating", handle)
        }
        return res
    }

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