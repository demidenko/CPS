package com.example.test3.account_manager

import androidx.appcompat.app.AppCompatActivity
import com.example.test3.*
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.JsonReader
import java.lang.Exception

class CodeforcesAccountManager(activity: AppCompatActivity): AccountManager(activity), ColoredHandles {

    data class CodeforcesUserInfo(
        override var status: STATUS,
        var handle: String,
        var rating: Int = NOT_RATED,
        var contribution: Int = 0
    ): UserInfo() {
        override val userID: String
            get() = handle

        override fun makeInfoOKString(): String {
            return if(rating == NOT_RATED) "$handle [not rated]" else "$handle $rating"
        }
    }

    override val PREFERENCES_FILE_NAME: String
        get() = preferences_file_name

    companion object{
        const val preferences_file_name = "codeforces"
        const val preferences_handle = "handle"
        const val preferences_rating = "rating"
        const val preferences_contribution = "contribution"

        var __cachedInfo: CodeforcesUserInfo? = null

        val NAMES = JsonReader.Options.of("handle", "rating", "contribution")

    }

    override suspend fun downloadInfo(data: String): CodeforcesUserInfo {
        val handle = data
        return try {
            val res = CodeforcesUserInfo(STATUS.FAILED, handle)
            with(JsonReaderFromURL("https://codeforces.com/api/user.info?handles=$handle") ?: return res) {
                readObject {
                    if(nextString("status") == "FAILED") return res.apply { status = STATUS.NOT_FOUND }
                    nextName()
                    readArrayOfObjects {
                        while (hasNext()) {
                            when (selectName(NAMES)) {
                                0 -> res.handle = nextString()
                                1 -> res.rating = nextInt()
                                2 -> res.contribution = nextInt()
                                else -> skipNameAndValue()
                            }
                        }
                    }
                }
                res.apply { status = STATUS.OK }
            }
        }catch (e: JsonEncodingException){
            CodeforcesUserInfo(STATUS.FAILED, handle)
        }catch (e: JsonDataException){
            CodeforcesUserInfo(STATUS.FAILED, handle)
        }
    }

    override var cachedInfo: UserInfo?
        get() = __cachedInfo
        set(value) { __cachedInfo = value as CodeforcesUserInfo }

    override fun readInfo(): CodeforcesUserInfo = with(prefs){
        CodeforcesUserInfo(
            STATUS.valueOf(getString(preferences_status, null) ?: STATUS.FAILED.name),
            handle = getString(preferences_handle, null) ?: "",
            rating = getInt(preferences_rating, NOT_RATED),
            contribution = getInt(preferences_contribution, 0)
        )
    }

    override fun writeInfo(info: UserInfo) = with(prefs.edit()){
        putString(preferences_status, info.status.name)
        info as CodeforcesUserInfo
        putString(preferences_handle, info.handle)
        putInt(preferences_rating, info.rating)
        putInt(preferences_contribution, info.contribution)
        commit()
    }

    override fun getColor(info: UserInfo): Int? = with(info as CodeforcesUserInfo){
        if(status != STATUS.OK || rating == NOT_RATED) return null
        return getHandleColor(info.rating).getARGB(this@CodeforcesAccountManager)
    }

    override suspend fun loadSuggestions(str: String): List<Pair<String, String>>? {
        val s = readURLData("https://codeforces.com/data/handles?q=$str") ?: return null
        val res = ArrayList<Pair<String, String>>()
        s.split('\n').filter { !it.contains('=') }.forEach {
            val i = it.indexOf('|')
            if (i != -1) {
                val handle = it.substring(i + 1)
                res += Pair(handle, handle)
            }
        }
        return res
    }

    override fun getHandleColor(rating: Int): HandleColor {
        return when {
            rating < 1200 -> HandleColor.GRAY
            rating < 1400 -> HandleColor.GREEN
            rating < 1600 -> HandleColor.CYAN
            rating < 1900 -> HandleColor.BLUE
            rating < 2100 -> HandleColor.VIOLET
            rating < 2400 -> HandleColor.ORANGE
            else -> HandleColor.RED
        }
    }

    override fun getColor(tag: HandleColor): Int {
        return when (tag){
            HandleColor.GRAY -> 0x808080
            HandleColor.GREEN -> 0x008000
            HandleColor.CYAN -> 0x03A89E
            HandleColor.BLUE -> 0x0000FF
            HandleColor.VIOLET -> 0xAA00AA
            HandleColor.ORANGE -> 0xFF8C00
            HandleColor.RED -> 0xFF0000
            else -> throw HandleColor.UnknownHandleColorException(tag)
        }
    }

    fun getHandleColorByTag(tag: String): Int? {
        return when (tag) {
            "user-gray" -> HandleColor.GRAY
            "user-green" -> HandleColor.GREEN
            "user-cyan" -> HandleColor.CYAN
            "user-blue" -> HandleColor.BLUE
            "user-violet" -> HandleColor.VIOLET
            "user-orange" -> HandleColor.ORANGE
            "user-red", "user-legendary" -> HandleColor.RED
            else -> null
        }?.getARGB(this)
    }
}