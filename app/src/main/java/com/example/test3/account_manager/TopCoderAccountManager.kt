package com.example.test3.account_manager

import android.content.Context
import com.example.test3.utils.TopCoderAPI
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TopCoderAccountManager(context: Context): AccountManager(context) {

    @Serializable
    data class TopCoderUserInfo(
        override var status: STATUS,
        var handle: String,
        var rating_algorithm: Int = NOT_RATED,
        var rating_marathon: Int = NOT_RATED
    ) : UserInfo(){
        override val userID: String
            get() = handle

        override fun makeInfoOKString(): String {
            return if(rating_algorithm == NOT_RATED) "$handle [not rated]" else "$handle $rating_algorithm"
        }

        override fun link(): String = "https://www.topcoder.com/members/$handle"
    }

    override val PREFERENCES_FILE_NAME: String
        get() = preferences_file_name

    companion object : ColoredHandles {
        const val preferences_file_name = "topcoder"

        var __cachedInfo: TopCoderUserInfo? = null


        override fun getHandleColor(rating: Int): HandleColor {
            return when{
                rating < 900 -> HandleColor.GRAY
                rating < 1200 -> HandleColor.GREEN
                rating < 1500 -> HandleColor.BLUE
                rating < 2200 -> HandleColor.YELLOW
                else -> HandleColor.RED
            }
        }

        override fun getColor(tag: HandleColor): Int {
            return when(tag){
                HandleColor.GRAY -> 0x999999
                HandleColor.GREEN -> 0x00A900
                HandleColor.BLUE -> 0x6666FE
                HandleColor.YELLOW -> 0xDDCC00
                HandleColor.RED -> 0xEE0000
                else -> throw HandleColor.UnknownHandleColorException(tag)
            }
        }
    }

    override fun emptyInfo() = TopCoderUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String): UserInfo {
        val handle = data
        val res = TopCoderUserInfo(STATUS.FAILED, handle)

        val apiResult = TopCoderAPI.getUser(handle) ?: return res
        apiResult.error?.let {
            if(it.name == "Not Found") return res.apply{ status = STATUS.NOT_FOUND }
            return res
        }

        res.handle = apiResult.handle
        apiResult.ratingSummary.forEach { ratingSummary ->
            when (ratingSummary.name) {
                "Algorithm" -> res.rating_algorithm = ratingSummary.rating
                "Marathon Match" -> res.rating_marathon = ratingSummary.rating
            }
        }

        return res.apply { status = STATUS.OK }
    }

    override var cachedInfo: UserInfo?
        get() = __cachedInfo
        set(value) { __cachedInfo = value as TopCoderUserInfo }

    override fun readInfo(): TopCoderUserInfo = with(prefs){
        val str = getString(preferences_key_user_info, null) ?: return@with emptyInfo().apply { status = STATUS.FAILED }
        Json.decodeFromString(str)
    }

    override fun writeInfo(info: UserInfo) = with(prefs.edit()){
        info as TopCoderUserInfo
        putString(preferences_key_user_info, Json.encodeToString(info))
        commit()
    }

    override fun getColor(info: UserInfo): Int? = with(info as TopCoderUserInfo){
        if(status != STATUS.OK || rating_algorithm == NOT_RATED) return null
        return getHandleColor(info.rating_algorithm).getARGB(Companion)
    }



}