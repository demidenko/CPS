package com.example.test3.account_manager

import android.content.Context
import com.example.test3.utils.TopCoderAPI
import com.example.test3.utils.jsonCPS
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

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

        override val ratingsUpperBounds = arrayOf(
            900 to HandleColor.GRAY,
            1200 to HandleColor.GREEN,
            1500 to HandleColor.BLUE,
            2200 to HandleColor.YELLOW
        )

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

    override fun decodeFromString(str: String) = jsonCPS.decodeFromString<TopCoderUserInfo>(str)

    override fun encodeToString(info: UserInfo) = jsonCPS.encodeToString(info as TopCoderUserInfo)

    override fun getColor(info: UserInfo): Int? = with(info as TopCoderUserInfo){
        if(status != STATUS.OK || rating_algorithm == NOT_RATED) return null
        return getHandleColorARGB(info.rating_algorithm)
    }



}