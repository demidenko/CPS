package com.example.test3.account_manager

import android.content.Context
import android.text.SpannableString
import com.example.test3.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class CodeforcesAccountManager(context: Context): RatedAccountManager(context) {

    @Serializable
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

        override fun link(): String = CodeforcesURLFactory.user(handle)
    }

    override val PREFERENCES_FILE_NAME: String
        get() = preferences_file_name

    companion object {
        const val preferences_file_name = "codeforces"
    }

    override fun emptyInfo() = CodeforcesUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String): CodeforcesUserInfo {
        val handle = data
        val res = CodeforcesUserInfo(STATUS.FAILED, handle)
        val response = CodeforcesAPI.getUser(handle) ?: return res
        if(response.status == CodeforcesAPIStatus.FAILED){
            if(response.comment == "handles: User with handle $handle not found") return res.copy( status = STATUS.NOT_FOUND )
            return res
        }
        val info = response.result!!
        return res.copy(
            status = STATUS.OK,
            handle = info.handle,
            rating = info.rating,
            contribution = info.contribution
        )
    }

    override fun decodeFromString(str: String) = jsonCPS.decodeFromString<CodeforcesUserInfo>(str)

    override fun encodeToString(info: UserInfo) = jsonCPS.encodeToString(info as CodeforcesUserInfo)

    override fun getColor(info: UserInfo): Int? = with(info as CodeforcesUserInfo){
        if(status != STATUS.OK || rating == NOT_RATED) return null
        return getHandleColorARGB(info.rating)
    }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion>? = withContext(Dispatchers.IO){
        val s = CodeforcesAPI.getHandleSuggestions(str) ?: return@withContext null
        val res = ArrayList<AccountSuggestion>()
        s.split('\n').filter { !it.contains('=') }.forEach {
            val i = it.indexOf('|')
            if (i != -1) {
                val handle = it.substring(i + 1)
                res += AccountSuggestion(handle, "", handle)
            }
        }
        return@withContext res
    }

    override val ratingsUpperBounds = arrayOf(
        1200 to HandleColor.GRAY,
        1400 to HandleColor.GREEN,
        1600 to HandleColor.CYAN,
        1900 to HandleColor.BLUE,
        2100 to HandleColor.VIOLET,
        2400 to HandleColor.ORANGE
    )

    override val rankedHandleColorsList = HandleColor.rankedCodeforces

    override fun getColor(handleColor: HandleColor): Int {
        return when (handleColor){
            HandleColor.GRAY -> 0x808080
            HandleColor.GREEN -> 0x008000
            HandleColor.CYAN -> 0x03A89E
            HandleColor.BLUE -> 0x0000FF
            HandleColor.VIOLET -> 0xAA00AA
            HandleColor.ORANGE -> 0xFF8C00
            HandleColor.RED -> 0xFF0000
            else -> throw HandleColor.UnknownHandleColorException(handleColor)
        }
    }

    override fun makeSpan(info: UserInfo): SpannableString {
        info as CodeforcesUserInfo
        return CodeforcesUtils.makeSpan(info.handle, CodeforcesUtils.getTagByRating(info.rating), this)
    }
}