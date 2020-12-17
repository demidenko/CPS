package com.example.test3.account_manager

import android.app.NotificationManager
import android.content.Context
import android.text.SpannableString
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import com.example.test3.NotificationChannels
import com.example.test3.NotificationIDs
import com.example.test3.SettingsDelegate
import com.example.test3.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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


    override fun isValidForSearch(char: Char) = isValidForUserID(char)
    override fun isValidForUserID(char: Char): Boolean {
        return when(char){
            in 'a'..'z', in 'A'..'Z', in '0'..'9', in "._-" -> true
            else -> false
        }
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

    override suspend fun getRatingHistory(info: UserInfo): List<RatingChange>? {
        info as CodeforcesUserInfo
        val response = CodeforcesAPI.getUserRatingChanges(info.handle) ?: return null
        if(response.status!=CodeforcesAPIStatus.OK) return null
        return response.result?.map { RatingChange(it) }
    }

    override fun getRating(info: UserInfo) = (info as CodeforcesUserInfo).rating

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

    override val dataStore by lazy { context.accountDataStoreCodeforces }
    override fun getSettings() = CodeforcesAccountSettingsDataStore(context, PREFERENCES_FILE_NAME)

    fun notifyRatingChange(m: NotificationManager, ratingChange: CodeforcesRatingChange){
        notifyRatingChange(
            context,
            m,
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
}

val Context.accountDataStoreCodeforces by SettingsDelegate { AccountDataStore(it, CodeforcesAccountManager.preferences_file_name) }

class CodeforcesAccountSettingsDataStore(context: Context, name: String): AccountSettingsDataStore(context, name){

    companion object {
        private val KEY_OBS_RATING = preferencesKey<Boolean>("observe_rating")
        private val KEY_LAST_RATED_CONTEST = preferencesKey<Int>("last_rated_contest")

        private val KEY_OBS_CONTRIBUTION = preferencesKey<Boolean>("observe_contribution")

        private val KEY_CONTEST_WATCH = preferencesKey<Boolean>("contest_watch")
        private val KEY_CONTEST_WATCH_LAST_SUBMISSION = preferencesKey<Long>("contest_watch_last_submission")
        private val KEY_CONTEST_WATCH_CANCELED = preferencesKey<String>("contest_watch_canceled")
    }

    override suspend fun resetRelatedData() {
        setLastRatedContestID(-1)
        setContestWatchLastSubmissionID(-1)
        setContestWatchCanceled(emptyList())
    }

    suspend fun getObserveRating() = dataStore.data.first()[KEY_OBS_RATING] ?: false
    suspend fun setObserveRating(flag: Boolean){
        dataStore.edit { it[KEY_OBS_RATING] = flag }
    }

    suspend fun getLastRatedContestID() = dataStore.data.first()[KEY_LAST_RATED_CONTEST] ?: -1
    suspend fun setLastRatedContestID(contestID: Int){
        dataStore.edit { it[KEY_LAST_RATED_CONTEST] = contestID }
    }

    suspend fun getObserveContribution() = dataStore.data.first()[KEY_OBS_CONTRIBUTION] ?: false
    suspend fun setObserveContribution(flag: Boolean){
        dataStore.edit { it[KEY_OBS_CONTRIBUTION] = flag }
    }

    suspend fun getContestWatchEnabled() = dataStore.data.first()[KEY_CONTEST_WATCH] ?: false
    suspend fun setContestWatchEnabled(flag: Boolean){
        dataStore.edit { it[KEY_CONTEST_WATCH] = flag }
    }

    suspend fun getContestWatchLastSubmissionID() = dataStore.data.first()[KEY_CONTEST_WATCH_LAST_SUBMISSION] ?: -1
    suspend fun setContestWatchLastSubmissionID(submissionID: Long){
        dataStore.edit { it[KEY_CONTEST_WATCH_LAST_SUBMISSION] = submissionID }
    }

    suspend fun getContestWatchCanceled(): List<Pair<Int,Long>> {
        val str = dataStore.data.first()[KEY_CONTEST_WATCH_CANCELED] ?: return emptyList()
        return jsonCPS.decodeFromString(str)
    }
    suspend fun setContestWatchCanceled(list: List<Pair<Int,Long>>){
        dataStore.edit { it[KEY_CONTEST_WATCH_CANCELED] = jsonCPS.encodeToString(list) }
    }
}