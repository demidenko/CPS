package com.example.test3.account_manager

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.text.set
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.test3.NotificationChannels
import com.example.test3.NotificationIDs
import com.example.test3.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString


@Serializable
data class CodeforcesUserInfo(
    override var status: STATUS,
    var handle: String,
    var rating: Int = NOT_RATED,
    var contribution: Int = 0,
    val lastOnlineTimeSeconds: Long = -1
): UserInfo() {
    constructor(codeforcesUser: CodeforcesUser): this(
        status = STATUS.OK,
        handle = codeforcesUser.handle,
        rating = codeforcesUser.rating,
        contribution = codeforcesUser.contribution,
        lastOnlineTimeSeconds = codeforcesUser.lastOnlineTimeSeconds
    )

    override val userID: String
        get() = handle

    override fun makeInfoOKString(): String {
        return if(rating == NOT_RATED) "$handle [not rated]" else "$handle $rating"
    }

    override fun link(): String = CodeforcesURLFactory.user(handle)
}


class CodeforcesAccountManager(context: Context): RatedAccountManager<CodeforcesUserInfo>(context, manager_name), AccountSettingsProvider {

    companion object {
        const val manager_name = "codeforces"
        private val Context.account_codeforces_dataStore by preferencesDataStore(manager_name)
    }

    override val homeURL = "https://codeforces.com"

    override fun isValidForSearch(char: Char) = isValidForUserID(char)
    override fun isValidForUserID(char: Char): Boolean {
        return when(char){
            in 'a'..'z', in 'A'..'Z', in '0'..'9', in "._-" -> true
            else -> false
        }
    }


    override fun emptyInfo() = CodeforcesUserInfo(STATUS.NOT_FOUND, "")



    override suspend fun downloadInfo(data: String, flags: Int): CodeforcesUserInfo {
        val handle = data
        val res = CodeforcesUserInfo(STATUS.FAILED, handle)
        val response = CodeforcesAPI.getUser(handle) ?: return res
        if(response.status == CodeforcesAPIStatus.FAILED){
            if(response.isHandleNotFound() == handle){
                if((flags and 1) != 0){
                    val (realHandle, status) = CodeforcesUtils.getRealHandle(handle)
                    return when(status){
                        STATUS.OK -> downloadInfo(realHandle, 0)
                        STATUS.NOT_FOUND -> res.copy( status = STATUS.NOT_FOUND )
                        STATUS.FAILED -> res
                    }
                }
                return res.copy( status = STATUS.NOT_FOUND )
            }
            return res
        }
        return CodeforcesUserInfo(response.result!!)
    }

    override fun decodeFromString(str: String) = jsonCPS.decodeFromString<CodeforcesUserInfo>(str)

    override fun encodeToString(info: CodeforcesUserInfo) = jsonCPS.encodeToString(info)

    override fun getColor(info: CodeforcesUserInfo): Int? = with(info){
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

    override suspend fun loadRatingHistory(info: CodeforcesUserInfo): List<RatingChange>? {
        val response = CodeforcesAPI.getUserRatingChanges(info.handle) ?: return null
        if(response.status!=CodeforcesAPIStatus.OK) return null
        return response.result?.map { RatingChange(it) }
    }

    override fun getRating(info: CodeforcesUserInfo) = info.rating

    override val ratingsUpperBounds = arrayOf(
        1200 to HandleColor.GRAY,
        1400 to HandleColor.GREEN,
        1600 to HandleColor.CYAN,
        1900 to HandleColor.BLUE,
        2100 to HandleColor.VIOLET,
        2400 to HandleColor.ORANGE
    )

    override val rankedHandleColorsList = HandleColor.rankedCodeforces

    fun makeSpan(handle: String, tag: CodeforcesUtils.ColorTag) = SpannableString(handle).apply {
        CodeforcesUtils.getHandleColorByTag(tag)?.getARGB(this@CodeforcesAccountManager)?.let { argb ->
            set(
                if(tag == CodeforcesUtils.ColorTag.LEGENDARY) 1 else 0,
                handle.length,
                ForegroundColorSpan(argb)
            )
        }
        if(tag != CodeforcesUtils.ColorTag.BLACK) set(0, handle.length, StyleSpan(Typeface.BOLD))
    }

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

    override fun makeSpan(info: CodeforcesUserInfo): SpannableString {
        return makeSpan(info.handle, CodeforcesUtils.getTagByRating(info.rating))
    }

    override fun getDataStore() = AccountDataStore(context.account_codeforces_dataStore)
    override fun getSettings() = CodeforcesAccountSettingsDataStore(this)

    fun notifyRatingChange(ratingChange: CodeforcesRatingChange){
        notifyRatingChange(
            context,
            NotificationChannels.codeforces_rating_changes,
            NotificationIDs.codeforces_rating_changes,
            this,
            ratingChange.handle,
            ratingChange.newRating,
            ratingChange.oldRating,
            ratingChange.rank,
            CodeforcesURLFactory.contestsWith(ratingChange.handle),
            ratingChange.ratingUpdateTimeSeconds
        )
    }

    suspend fun applyRatingChange(ratingChange: CodeforcesRatingChange){
        val info = getSavedInfo()

        val settings = getSettings()
        val prevRatingChangeContestID = settings.lastRatedContestID()

        if(prevRatingChangeContestID == ratingChange.contestId && info.rating == ratingChange.newRating) return

        settings.lastRatedContestID(ratingChange.contestId)

        if(prevRatingChangeContestID != null) {
            notifyRatingChange(ratingChange)
            val newInfo = loadInfo(info.handle)
            if(newInfo.status!=STATUS.FAILED){
                setSavedInfo(newInfo)
            }else{
                setSavedInfo(info.copy(rating = ratingChange.newRating))
            }
        }
    }

    override val ratingUpperBoundRevolutions: List<Pair<Long, Array<Pair<Int, HandleColor>>>> =
        listOf(
            //https://codeforces.com/blog/entry/59228
            1525364996L to arrayOf(
                1200 to HandleColor.GRAY,
                1400 to HandleColor.GREEN,
                1600 to HandleColor.CYAN,
                1900 to HandleColor.BLUE,
                2200 to HandleColor.VIOLET,
                2400 to HandleColor.ORANGE
            ),
            //https://codeforces.com/blog/entry/20638
            1443721088L to arrayOf(
                1200 to HandleColor.GRAY,
                1500 to HandleColor.GREEN,
                1700 to HandleColor.BLUE,
                1900 to HandleColor.VIOLET,
                2200 to HandleColor.ORANGE
            ),
            //https://codeforces.com/blog/entry/3064
            1320620562L to arrayOf(
                1200 to HandleColor.GRAY,
                1500 to HandleColor.GREEN,
                1650 to HandleColor.BLUE,
                1800 to HandleColor.VIOLET,
                2000 to HandleColor.ORANGE
            ),
            //https://codeforces.com/blog/entry/1383
            1298914585L to arrayOf(
                1200 to HandleColor.GRAY,
                1500 to HandleColor.GREEN,
                1650 to HandleColor.BLUE,
                2000 to HandleColor.YELLOW
            )
            //https://codeforces.com/blog/entry/126
        )
}

class CodeforcesAccountSettingsDataStore(manager: CodeforcesAccountManager)
    : AccountSettingsDataStore(manager.context.account_settings_codeforces_dataStore){

    companion object {
        private val Context.account_settings_codeforces_dataStore by preferencesDataStore(CodeforcesAccountManager.manager_name + "_account_settings")
    }

    val observeRating = Item(booleanPreferencesKey("observe_rating"), false)
    val lastRatedContestID = ItemNullable(intPreferencesKey("last_rated_contest"))

    val observeContribution = Item(booleanPreferencesKey("observe_contribution"), false)

    val contestWatchEnabled = Item(booleanPreferencesKey("contest_watch"), false)
    val contestWatchLastSubmissionID = ItemNullable(longPreferencesKey("contest_watch_last_submission"))
    val contestWatchStartedContestID = ItemNullable(intPreferencesKey("contest_watch_started_contest"))
    private val KEY_CONTEST_WATCH_CANCELED = stringPreferencesKey("contest_watch_canceled")

    override val keysForReset get() = listOf(
        lastRatedContestID.key,
        contestWatchLastSubmissionID.key,
        contestWatchStartedContestID.key,
        KEY_CONTEST_WATCH_CANCELED
    )

    suspend fun getContestWatchCanceled(): List<Pair<Int,Long>> {
        val str = dataStore.data.first()[KEY_CONTEST_WATCH_CANCELED] ?: return emptyList()
        return jsonCPS.decodeFromString(str)
    }
    suspend fun setContestWatchCanceled(list: List<Pair<Int,Long>>){
        dataStore.edit { it[KEY_CONTEST_WATCH_CANCELED] = jsonCPS.encodeToString(list) }
    }
}