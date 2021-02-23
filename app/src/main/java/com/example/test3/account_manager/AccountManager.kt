package com.example.test3.account_manager

import android.app.NotificationManager
import android.content.Context
import android.text.SpannableString
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.asLiveData
import com.example.test3.*
import com.example.test3.utils.AtCoderRatingChange
import com.example.test3.utils.CodeforcesRatingChange
import com.example.test3.utils.SettingsDataStore
import com.example.test3.utils.signedToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit


abstract class AccountManager(val context: Context) {

    abstract val userIDName: String

    abstract val PREFERENCES_FILE_NAME: String
    protected open val dataStore = AccountDataStore(context, PREFERENCES_FILE_NAME)
    val dataStoreLive by lazy{ dataStore.getLiveData() }

    open fun getSettings() = AccountSettingsDataStore(context, PREFERENCES_FILE_NAME)

    abstract fun emptyInfo(): UserInfo

    protected abstract suspend fun downloadInfo(data: String, flags: Int): UserInfo
    suspend fun loadInfo(data: String, flags: Int = 0): UserInfo {
        if(data.isBlank()) return emptyInfo()
        return withContext(Dispatchers.IO){
            downloadInfo(data, flags)
        }
    }

    open suspend fun loadSuggestions(str: String): List<AccountSuggestion>? = null
    open val isProvidesSuggestions = true

    protected abstract fun decodeFromString(str: String): UserInfo
    protected abstract fun encodeToString(info: UserInfo): String

    suspend fun getSavedInfo(): UserInfo {
        val str = dataStore.getString() ?: return emptyInfo()
        return decodeFromString(str)
    }
    suspend fun setSavedInfo(info: UserInfo) {
        val old = getSavedInfo()
        dataStore.putString(encodeToString(info))
        if(info.userID != old.userID) getSettings().resetRelatedData()
    }

    open fun getColor(info: UserInfo): Int? = null

    open fun isValidForSearch(char: Char): Boolean = true
    open fun isValidForUserID(char: Char): Boolean = true
}


abstract class RatedAccountManager(context: Context) : AccountManager(context){
    abstract fun getColor(handleColor: HandleColor): Int
    abstract val ratingsUpperBounds: Array<Pair<Int, HandleColor>>

    fun getHandleColor(rating: Int): HandleColor {
        return ratingsUpperBounds.find { (bound, color) ->
            rating < bound
        }?.second ?: HandleColor.RED
    }

    fun getHandleColorARGB(rating: Int): Int {
        return getHandleColor(rating).getARGB(this)
    }

    abstract fun makeSpan(info: UserInfo): SpannableString

    override val userIDName = "handle"

    abstract val rankedHandleColorsList: Array<HandleColor>
    abstract fun getRating(info: UserInfo): Int
    fun getOrder(info: UserInfo): Double {
        val rating = getRating(info)
        if(rating == NOT_RATED) return -1.0
        val handleColor = getHandleColor(rating)
        if(handleColor == HandleColor.RED) return 1e9
        val i = rankedHandleColorsList.indexOfFirst { handleColor == it }
        val j = rankedHandleColorsList.indexOfLast { handleColor == it }
        ratingsUpperBounds.indexOfFirst { it.second == handleColor }.let { pos ->
            val lower = if(pos>0) ratingsUpperBounds[pos-1].first else 0
            val upper = ratingsUpperBounds[pos].first
            val blockLength = (upper - lower).toDouble() / (j-i+1)
            return i + (rating - lower) / blockLength
        }
    }

    protected open suspend fun loadRatingHistory(info: UserInfo): List<RatingChange>? = null
    suspend fun getRatingHistory(info: UserInfo): List<RatingChange>? = loadRatingHistory(info)?.sortedBy { it.timeSeconds }
}

data class RatingChange(
    val rating: Int,
    val timeSeconds: Long
){
    constructor(ratingChange: CodeforcesRatingChange): this(
        ratingChange.newRating,
        ratingChange.ratingUpdateTimeSeconds
    )

    constructor(ratingChange: AtCoderRatingChange): this(
        ratingChange.NewRating,
        ratingChange.EndTime
    )
}

class AccountDataStore(context: Context, name: String): SettingsDataStore(context, name) {
    companion object {
        val KEY_USER_INFO = stringPreferencesKey("user_info")
    }

    fun getLiveData() = dataStore.data.asLiveData()

    suspend fun getString(): String? = dataStore.data.first()[KEY_USER_INFO]

    suspend fun putString(str: String){
        dataStore.edit {
            it[KEY_USER_INFO] = str
        }
    }
}

open class AccountSettingsDataStore(context: Context, name: String): SettingsDataStore(context, "${name}_account_settings") {
    open suspend fun resetRelatedData(){}
}

enum class STATUS{
    OK,
    NOT_FOUND,
    FAILED
}
const val NOT_RATED = Int.MIN_VALUE

abstract class UserInfo{
    abstract val userID: String
    abstract var status: STATUS

    protected abstract fun makeInfoOKString(): String
    fun makeInfoString(): String {
        return when(status){
            STATUS.FAILED -> "Error on load: $userID"
            STATUS.NOT_FOUND -> "Not found: $userID"
            else -> makeInfoOKString()
        }
    }

    abstract fun link(): String
}


enum class HandleColor(private val rgb: Int) {
    GRAY(0x888888),
    BROWN(0x80461B),
    GREEN(0x009000),
    CYAN(0x00A89E),
    BLUE(0x3F68F0),
    VIOLET(0xB04ECC),
    YELLOW(0xCCCC00),
    ORANGE(0xFB8000),
    RED(0xED301D);

    companion object {
        val rankedCodeforces    = arrayOf(GRAY, GRAY, GREEN, CYAN, BLUE, VIOLET, VIOLET, ORANGE, ORANGE, RED)
        val rankedAtCoder       = arrayOf(GRAY, BROWN, GREEN, CYAN, BLUE, YELLOW, YELLOW, ORANGE, ORANGE, RED)
        val rankedTopCoder      = arrayOf(GRAY, GRAY, GREEN, GREEN, BLUE, YELLOW, YELLOW, YELLOW, YELLOW, RED)
    }

    fun getARGB(manager: RatedAccountManager, realColor: Boolean): Int {
        return ((if(realColor) manager.getColor(this) else rgb) + 0xFF000000).toInt()
    }

    fun getARGB(manager: RatedAccountManager) = getARGB(manager, manager.context.getUseRealColors())

    class UnknownHandleColorException(color: HandleColor): Exception("${color.name} is invalid color for manager ")
}

data class AccountSuggestion(
    val title: String,
    val info: String,
    val userId: String
)


fun notifyRatingChange(
    context: Context,
    notificationManager: NotificationManager,
    notificationChannel: NotificationChannelLazy,
    notificationID: Int,
    accountManager: RatedAccountManager,
    handle: String, newRating: Int, oldRating: Int, rank: Int, url: String? = null, timeSeconds: Long? = null
){
    val n = notificationBuilder(context, notificationChannel).apply {
        val decreased = newRating < oldRating
        setSmallIcon(if(decreased) R.drawable.ic_rating_down else R.drawable.ic_rating_up)
        setContentTitle("$handle new rating: $newRating")
        val difference = signedToString(newRating - oldRating)
        setContentText("$difference (rank: $rank)")
        setSubText("${accountManager.PREFERENCES_FILE_NAME} rating changes")
        color = accountManager.getHandleColorARGB(newRating)
        url?.let {
            setContentIntent(makePendingIntentOpenURL(url, context))
        }
        timeSeconds?.let {
            setShowWhen(true)
            setWhen(TimeUnit.SECONDS.toMillis(timeSeconds))
        }
    }
    notificationManager.notify(notificationID, n.build())
}