package com.example.test3.account_manager

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.text.set
import androidx.datastore.preferences.preferencesDataStore
import com.example.test3.utils.TopCoderAPI
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

class TopCoderAccountManager(context: Context):
    RatedAccountManager<TopCoderAccountManager.TopCoderUserInfo>(context, manager_name)
{

    companion object {
        const val manager_name = "topcoder"
        private val Context.account_topcoder_dataStore by preferencesDataStore(manager_name)

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

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

    override val homeURL = "https://www.topcoder.com/community/competitive-programming"

    override fun isValidForSearch(char: Char) = isValidForUserID(char)
    override fun isValidForUserID(char: Char): Boolean {
        return when(char){
            in 'a'..'z', in 'A'..'Z', in '0'..'9', in " _.[]{}" -> true
            else -> false
        }
    }

    override fun emptyInfo() = TopCoderUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String, flags: Int): TopCoderUserInfo {
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

    override fun getColor(info: TopCoderUserInfo): Int? = with(info){
        if(status != STATUS.OK || rating_algorithm == NOT_RATED) return null
        return getHandleColorARGB(info.rating_algorithm)
    }

    override suspend fun loadRatingHistory(info: TopCoderUserInfo): List<RatingChange>? {
        val response = TopCoderAPI.getStatsHistory(info.handle) ?: return null
        if(!response.success || response.status!=200) return null
        return response.content[0].DATA_SCIENCE.SRM.history.map { topCoderRatingChange ->
            RatingChange(topCoderRatingChange)
        }
    }

    override fun getRating(info: TopCoderUserInfo) = info.rating_algorithm

    override val ratingsUpperBounds = arrayOf(
        900 to HandleColor.GRAY,
        1200 to HandleColor.GREEN,
        1500 to HandleColor.BLUE,
        2200 to HandleColor.YELLOW
    )

    override val rankedHandleColorsList = HandleColor.rankedTopCoder

    override fun getColor(handleColor: HandleColor): Int {
        return when(handleColor){
            HandleColor.GRAY -> 0x999999
            HandleColor.GREEN -> 0x00A900
            HandleColor.BLUE -> 0x6666FE
            HandleColor.YELLOW -> 0xDDCC00
            HandleColor.RED -> 0xEE0000
            else -> throw HandleColor.UnknownHandleColorException(handleColor)
        }
    }

    override fun makeSpan(info: TopCoderUserInfo): SpannableString {
        return SpannableString(info.handle).apply {
            getColor(info)?.let {
                set(0, length, ForegroundColorSpan(it))
            }
            if(info.rating_algorithm != NOT_RATED) set(0, length, StyleSpan(Typeface.BOLD))
        }
    }

    override fun getDataStore() = accountDataStore(context.account_topcoder_dataStore, emptyInfo())
}