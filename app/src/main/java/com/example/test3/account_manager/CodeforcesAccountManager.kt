package com.example.test3.account_manager

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.test3.*
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.JsonReader

class CodeforcesAccountManager(activity: AppCompatActivity): AccountManager(activity) {

    data class CodeforcesUserInfo(
        override var status: STATUS,
        var handle: String,
        var rating: Int = NOT_RATED
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

        var __cachedInfo: CodeforcesUserInfo? = null

        val NAMES = JsonReader.Options.of("handle", "rating")

    }

    enum class HandleColor(rgb: Int){
        GRAY(0x808080),
        GREEN(0x008000),
        CYAN(0x03A89E),
        BLUE(0x0000FF),
        VIOLET(0xAA00AA),
        ORANGE(0xFF8C00),
        RED(0xFF0000);

        val argb = (rgb + 0xFF000000).toInt()

        companion object {
            fun getColorByRating(rating: Int): HandleColor {
                return when {
                    rating < 1200 -> GRAY
                    rating < 1400 -> GREEN
                    rating < 1600 -> CYAN
                    rating < 1900 -> BLUE
                    rating < 2100 -> VIOLET
                    rating < 2400 -> ORANGE
                    else -> RED
                }
            }

            fun getColorByTag(tag: String): HandleColor? {
                return when (tag) {
                    "user-gray" -> GRAY
                    "user-green" -> GREEN
                    "user-cyan" -> CYAN
                    "user-blue" -> BLUE
                    "user-violet" -> VIOLET
                    "user-orange" -> ORANGE
                    "user-red", "user-legendary" -> RED
                    else -> null
                }
            }
        }
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

    override fun readInfo(): CodeforcesUserInfo = with(activity.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)){
        return CodeforcesUserInfo(
            STATUS.valueOf(getString(preferences_status, null) ?: STATUS.FAILED.name),
            getString(preferences_handle, "") ?: "",
            getInt(preferences_rating, NOT_RATED)
        )
    }

    override fun writeInfo(info: UserInfo) = with(activity.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE).edit()){
        putString(preferences_status, info.status.name)
        info as CodeforcesUserInfo
        putString(preferences_handle, info.handle)
        putInt(preferences_rating, info.rating)
        commit()
    }

    override fun getColor(info: UserInfo): Int? = with(info as CodeforcesUserInfo){
        if(status != STATUS.OK || rating == NOT_RATED) return null
        return HandleColor.getColorByRating(info.rating).argb
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
}