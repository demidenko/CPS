package com.example.test3.account_manager

import android.content.Context
import android.text.SpannableString
import androidx.annotation.ColorRes
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.test3.NotificationChannelLazy
import com.example.test3.R
import com.example.test3.makePendingIntentOpenURL
import com.example.test3.notificationBuilder
import com.example.test3.ui.getUseRealColors
import com.example.test3.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit


abstract class AccountManager<U: UserInfo>(val context: Context, val managerName: String) {

    abstract val userIDName: String
    abstract val homeURL: String

    protected abstract fun getDataStore(): AccountDataStore<U>
    fun flowOfInfo() = getDataStore().userInfo.flow.map { info -> this to info }

    abstract fun emptyInfo(): U

    protected abstract suspend fun downloadInfo(data: String, flags: Int): U
    suspend fun loadInfo(data: String, flags: Int = 0): U {
        if(data.isBlank()) return emptyInfo()
        return withContext(Dispatchers.IO){
            downloadInfo(data, flags)
        }
    }

    open suspend fun loadSuggestions(str: String): List<AccountSuggestion>? = null
    open val isProvidesSuggestions = true

    suspend fun getSavedInfo(): U = getDataStore().userInfo()

    suspend fun setSavedInfo(info: U) {
        val old = getSavedInfo()
        getDataStore().userInfo(info)
        if(info.userID != old.userID && this is AccountSettingsProvider) getSettings().resetRelatedData()
    }

    open fun getColor(info: U): Int? = null

    open fun isValidForSearch(char: Char): Boolean = true
    open fun isValidForUserID(char: Char): Boolean = true
}

interface AccountSettingsProvider {
    fun getSettings(): AccountSettingsDataStore
}

abstract class RatedAccountManager<U: UserInfo>(context: Context, managerName: String) : AccountManager<U>(context, managerName){
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

    abstract fun makeSpan(info: U): SpannableString

    override val userIDName = "handle"

    abstract val rankedHandleColorsList: Array<HandleColor>
    abstract fun getRating(info: U): Int
    fun getOrder(info: U): Double {
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

    protected open suspend fun loadRatingHistory(info: U): List<RatingChange>? = null
    suspend fun getRatingHistory(info: U): List<RatingChange>? = loadRatingHistory(info)?.sortedBy { it.timeSeconds }

    //list of (last time, bounds)
    open val ratingUpperBoundRevolutions: List<Pair<Long, Array<Pair<Int, HandleColor>>>> = emptyList()
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

class AccountDataStore<U: UserInfo>(
    dataStore: DataStore<Preferences>,
    val userInfo: ItemStringConvertible<U>
) : CPSDataStore(dataStore)

inline fun <reified U: UserInfo> accountDataStore(dataStore: DataStore<Preferences>, emptyUserInfo: U): AccountDataStore<U> {
    return AccountDataStore(dataStore, CPSDataStore(dataStore).itemJsonConvertible(jsonCPS, "user_info", emptyUserInfo))
}

open class AccountSettingsDataStore(dataStore: DataStore<Preferences>) : CPSDataStore(dataStore) {
    protected open val keysForReset: List<CPSDataStoreItem<*,*>> = emptyList()
    suspend fun resetRelatedData() {
        val keys = keysForReset.takeIf { it.isNotEmpty() } ?: return
        dataStore.edit { prefs ->
            keys.forEach { prefs.remove(it.key) }
        }
    }
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

    fun isEmpty() = userID.isBlank()
}


enum class HandleColor(@ColorRes private val resid: Int) {
    GRAY(R.color.GRAY),
    BROWN(R.color.BROWN),
    GREEN(R.color.GREEN),
    CYAN(R.color.CYAN),
    BLUE(R.color.BLUE),
    VIOLET(R.color.VIOLET),
    YELLOW(R.color.YELLOW),
    ORANGE(R.color.ORANGE),
    RED(R.color.RED);

    companion object {
        val rankedCodeforces    = arrayOf(GRAY, GRAY, GREEN, CYAN, BLUE, VIOLET, VIOLET, ORANGE, ORANGE, RED)
        val rankedAtCoder       = arrayOf(GRAY, BROWN, GREEN, CYAN, BLUE, YELLOW, YELLOW, ORANGE, ORANGE, RED)
        val rankedTopCoder      = arrayOf(GRAY, GRAY, GREEN, GREEN, BLUE, YELLOW, YELLOW, YELLOW, YELLOW, RED)
    }

    fun getARGB(manager: RatedAccountManager<*>, realColor: Boolean): Int {
        return if(realColor) (manager.getColor(this) + 0xFF000000).toInt()
            else getColorFromResource(manager.context,resid)
    }

    fun getARGB(manager: RatedAccountManager<*>) = getARGB(manager, manager.context.getUseRealColors())

    class UnknownHandleColorException(color: HandleColor): Exception("${color.name} is invalid color for manager ")
}

data class AccountSuggestion(
    val title: String,
    val info: String,
    val userId: String
)


fun notifyRatingChange(
    context: Context,
    notificationChannel: NotificationChannelLazy,
    notificationID: Int,
    accountManager: RatedAccountManager<*>,
    handle: String, newRating: Int, oldRating: Int, rank: Int, url: String? = null, timeSeconds: Long? = null
){
    val n = notificationBuilder(context, notificationChannel).apply {
        val decreased = newRating < oldRating
        setSmallIcon(if(decreased) R.drawable.ic_rating_down else R.drawable.ic_rating_up)
        setContentTitle("$handle new rating: $newRating")
        val difference = signedToString(newRating - oldRating)
        setContentText("$difference (rank: $rank)")
        setSubText("${accountManager.managerName} rating changes")
        color = accountManager.getHandleColorARGB(newRating)
        url?.let {
            setContentIntent(makePendingIntentOpenURL(url, context))
        }
        timeSeconds?.let {
            setShowWhen(true)
            setWhen(TimeUnit.SECONDS.toMillis(timeSeconds))
        }
    }
    NotificationManagerCompat.from(context).notify(notificationID, n.build())
}