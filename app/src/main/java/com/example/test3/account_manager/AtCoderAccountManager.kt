package com.example.test3.account_manager

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.text.set
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.test3.NotificationChannels
import com.example.test3.NotificationIDs
import com.example.test3.utils.AtCoderAPI
import com.example.test3.utils.AtCoderRatingChange
import com.example.test3.utils.AtCoderURLFactory
import com.example.test3.utils.jsonCPS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class AtCoderAccountManager(context: Context)
    : RatedAccountManager<AtCoderAccountManager.AtCoderUserInfo>(context, manager_name), AccountSettingsProvider {

    companion object {
        const val manager_name = "atcoder"
        private val Context.account_atcoder_dataStore by preferencesDataStore(manager_name)
    }

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

        override fun link(): String = AtCoderURLFactory.user(handle)
    }

    override val homeURL = "https://atcoder.jp"

    override fun isValidForSearch(char: Char) = isValidForUserID(char)
    override fun isValidForUserID(char: Char): Boolean {
        return when(char){
            in 'a'..'z', in 'A'..'Z', in '0'..'9', in "_" -> true
            else -> false
        }
    }

    override fun emptyInfo() = AtCoderUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String, flags: Int): AtCoderUserInfo {
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

    override fun decodeFromString(str: String) = jsonCPS.decodeFromString<AtCoderUserInfo>(str)

    override fun encodeToString(info: AtCoderUserInfo) = jsonCPS.encodeToString(info)

    override fun getColor(info: AtCoderUserInfo): Int?  = with(info){
        if(status != STATUS.OK || rating == NOT_RATED) return null
        return getHandleColorARGB(info.rating)
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

    override suspend fun loadRatingHistory(info: AtCoderUserInfo): List<RatingChange>? {
        return AtCoderAPI.getRatingChanges(info.userID)?.map { RatingChange(it) }
    }

    override fun getRating(info: AtCoderUserInfo) = info.rating

    override val ratingsUpperBounds = arrayOf(
        400 to HandleColor.GRAY,
        800 to HandleColor.BROWN,
        1200 to HandleColor.GREEN,
        1600 to HandleColor.CYAN,
        2000 to HandleColor.BLUE,
        2400 to HandleColor.YELLOW,
        2800 to HandleColor.ORANGE
    )

    override val rankedHandleColorsList = HandleColor.rankedAtCoder

    override fun getColor(handleColor: HandleColor): Int {
        return when(handleColor){
            HandleColor.GRAY -> 0x808080
            HandleColor.BROWN -> 0x804000
            HandleColor.GREEN -> 0x008000
            HandleColor.CYAN -> 0x00C0C0
            HandleColor.BLUE -> 0x0000FF
            HandleColor.YELLOW -> 0xC0C000
            HandleColor.ORANGE -> 0xFF8000
            HandleColor.RED -> 0xFF0000
            else -> throw HandleColor.UnknownHandleColorException(handleColor)
        }
    }

    override fun makeSpan(info: AtCoderUserInfo): SpannableString {
        return SpannableString(info.handle).apply {
            getColor(info)?.let {
                set(0, length, ForegroundColorSpan(it))
            }
            if(info.rating != NOT_RATED) set(0, length, StyleSpan(Typeface.BOLD))
        }
    }

    override fun getDataStore() = AccountDataStore(context.account_atcoder_dataStore)
    override fun getSettings() = AtCoderAccountSettingsDataStore(this)

    fun notifyRatingChange(handle: String, ratingChange: AtCoderRatingChange){
        notifyRatingChange(
            context,
            NotificationChannels.atcoder_rating_changes,
            NotificationIDs.atcoder_rating_changes,
            this,
            handle,
            ratingChange.NewRating,
            ratingChange.OldRating,
            ratingChange.Place,
            AtCoderURLFactory.userContestResult(handle, ratingChange.getContestID()),
            ratingChange.EndTime
        )
    }
}

class AtCoderAccountSettingsDataStore(manager: AtCoderAccountManager)
    : AccountSettingsDataStore(manager.context.account_settings_atcoder_dataStore){

    companion object {
        private val Context.account_settings_atcoder_dataStore by preferencesDataStore(AtCoderAccountManager.manager_name + "_account_settings")
    }

    val observeRating = Item(booleanPreferencesKey("observe_rating"), false)
    val lastRatedContestID = ItemNullable(stringPreferencesKey("last_rated_contest"))

    override val keysForReset = listOf(lastRatedContestID)

}