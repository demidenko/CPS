package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.demich.cps.NotificationChannelLazy
import com.demich.cps.R
import com.demich.cps.accounts.SmallAccountPanelTypeRated
import com.demich.cps.makePendingIntentOpenURL
import com.demich.cps.notificationBuildAndNotify
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.ui.useOriginalColors
import com.demich.cps.utils.CPSDataStore
import com.demich.cps.utils.signedToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

enum class AccountManagers {
    codeforces,
    atcoder,
    codechef,
    dmoj,
    acmp,
    timus,
    clist
}

val Context.allAccountManagers: List<AccountManager<out UserInfo>>
    get() = listOf(
        CodeforcesAccountManager(this),
        AtCoderAccountManager(this),
        CodeChefAccountManager(this),
        DmojAccountManager(this),
        ACMPAccountManager(this),
        TimusAccountManager(this)
    )


abstract class AccountManager<U: UserInfo>(val context: Context, val type: AccountManagers) {

    abstract val userIdTitle: String
    abstract val urlHomePage: String

    protected abstract fun getDataStore(): AccountDataStore<U>
    fun flowOfInfo() = getDataStore().userInfo.flow
    fun flowOfInfoWithManager() = getDataStore().userInfo.flow.map { info -> UserInfoWithManager(info, this) }

    abstract fun emptyInfo(): U

    protected abstract suspend fun downloadInfo(data: String, flags: Int): U
    suspend fun loadInfo(data: String, flags: Int = 0): U {
        if(data.isBlank()) return emptyInfo()
        return withContext(Dispatchers.IO) {
            downloadInfo(data, flags)
        }
    }

    open fun isValidForUserId(char: Char): Boolean = true

    suspend fun getSavedInfo(): U = getDataStore().userInfo()

    suspend fun setSavedInfo(info: U) {
        val old = getSavedInfo()
        getDataStore().userInfo(info)
        if(info.userId != old.userId && this is AccountSettingsProvider) getSettings().resetRelatedData()
    }

    @Composable
    open fun colorFor(userInfo: U): Color = Color.Unspecified

    @Composable
    abstract fun makeOKInfoSpan(userInfo: U): AnnotatedString

    @Composable
    open fun Panel(userInfo: U) {}

    @Composable
    open fun BigView(
        userInfo: U,
        setBottomBarContent: (@Composable RowScope.() -> Unit) -> Unit,
        modifier: Modifier = Modifier
     ) = Box(modifier) {
        Panel(userInfo)
    }
}

abstract class RatedAccountManager<U: UserInfo>(context: Context, type: AccountManagers):
    AccountManager<U>(context, type)
{
    override val userIdTitle get() = "handle"

    abstract val ratingsUpperBounds: Array<Pair<HandleColor, Int>>
    fun getHandleColor(rating: Int): HandleColor =
        ratingsUpperBounds
            .firstOrNull { rating < it.second }?.first
            ?: HandleColor.RED

    abstract fun originalColor(handleColor: HandleColor): Color

    @Composable
    fun colorFor(handleColor: HandleColor): Color =
        if (useOriginalColors) originalColor(handleColor)
        else cpsColors.handleColor(handleColor)

    @Composable
    fun colorFor(rating: Int): Color = colorFor(handleColor = getHandleColor(rating))

    @Composable
    override fun colorFor(userInfo: U): Color {
        if (userInfo.status != STATUS.OK || getRating(userInfo) == NOT_RATED) return Color.Unspecified
        return colorFor(rating = getRating(userInfo))
    }

    @Composable
    open fun makeHandleSpan(userInfo: U): AnnotatedString =
        buildAnnotatedString {
            append(userInfo.userId)
            colorFor(userInfo).takeIf { it != Color.Unspecified  }?.let { color ->
                addStyle(
                    style = SpanStyle(color = color, fontWeight = FontWeight.Bold),
                    start = 0,
                    end = length
                )
            }
        }

    @Composable
    override fun Panel(userInfo: U) = SmallAccountPanelTypeRated(userInfo)

    abstract val rankedHandleColorsList: Array<HandleColor>
    abstract fun getRating(userInfo: U): Int

    protected open suspend fun loadRatingHistory(info: U): List<RatingChange>? = null
    suspend fun getRatingHistory(info: U): List<RatingChange>? {
        return kotlin.runCatching {
            loadRatingHistory(info)?.sortedBy { it.date }
        }.getOrNull()
    }
}

@Serializable
data class RatingChange(
    val rating: Int,
    val date: Instant,
    val title: String = "",
    val oldRating: Int? = null,
    val rank: Int? = null
)

class AccountDataStore<U: UserInfo>(
    dataStore: DataStore<Preferences>,
    val userInfo: ItemStringConvertible<U>
): CPSDataStore(dataStore)

inline fun <reified U: UserInfo> accountDataStore(dataStore: DataStore<Preferences>, emptyUserInfo: U): AccountDataStore<U> {
    return AccountDataStore(dataStore, CPSDataStore(dataStore).itemJsonConvertible(name = "user_info", defaultValue = emptyUserInfo))
}

open class AccountSettingsDataStore(dataStore: DataStore<Preferences>): CPSDataStore(dataStore) {
    protected open val keysForReset: List<DataStoreItem<*,*>> = emptyList()
    suspend fun resetRelatedData() {
        val keys = keysForReset.takeIf { it.isNotEmpty() } ?: return
        dataStore.edit { prefs ->
            keys.forEach { prefs.remove(it.key) }
        }
    }
}

enum class STATUS {
    OK,
    NOT_FOUND,
    FAILED
}
const val NOT_RATED = Int.MIN_VALUE

abstract class UserInfo {
    abstract val userId: String
    abstract val status: STATUS

    abstract fun link(): String

    fun isEmpty() = userId.isBlank()
}

data class UserInfoWithManager<U: UserInfo>(
    val userInfo: U,
    val manager: AccountManager<U>
) {
    val type: AccountManagers get() = manager.type
}

enum class HandleColor {
    GRAY,
    BROWN,
    GREEN,
    CYAN,
    BLUE,
    VIOLET,
    YELLOW,
    ORANGE,
    RED;

    companion object {
        val rankedCodeforces    = arrayOf(GRAY, GRAY, GREEN, CYAN, BLUE, VIOLET, VIOLET, ORANGE, ORANGE, RED)
        val rankedAtCoder       = arrayOf(GRAY, BROWN, GREEN, CYAN, BLUE, YELLOW, YELLOW, ORANGE, ORANGE, RED)
        val rankedTopCoder      = arrayOf(GRAY, GRAY, GREEN, GREEN, BLUE, YELLOW, YELLOW, YELLOW, YELLOW, RED)
        //TODO:
        val rankedCodeChef      = arrayOf(GRAY, GRAY, GREEN, GREEN, BLUE, VIOLET, YELLOW, YELLOW, ORANGE, RED)
        val rankedDmoj          = arrayOf(GRAY, GRAY, GREEN, GREEN, BLUE, VIOLET, VIOLET, YELLOW, YELLOW, RED)
    }

    class UnknownHandleColorException(color: HandleColor, manager: RatedAccountManager<*>):
        Throwable("Manager ${manager.type.name} does not support color ${color.name}")
}

data class AccountSuggestion(
    val title: String,
    val userId: String,
    val info: String = ""
)

interface AccountSuggestionsProvider {
    suspend fun loadSuggestions(str: String): List<AccountSuggestion>? = null
    fun isValidForSearch(char: Char): Boolean = true
}

interface AccountSettingsProvider {
    fun getSettings(): AccountSettingsDataStore

    @Composable
    fun Settings() {}
}

interface RatingRevolutionsProvider {
    //list of (last time, bounds)
    val ratingUpperBoundRevolutions: List<Pair<Instant, Array<Pair<HandleColor, Int>>>>
}

fun notifyRatingChange(
    accountManager: RatedAccountManager<*>,
    notificationChannel: NotificationChannelLazy,
    notificationId: Int,
    handle: String, newRating: Int, oldRating: Int, rank: Int, url: String? = null, time: Instant? = null
) {
    notificationBuildAndNotify(accountManager.context, notificationChannel, notificationId) {
        val decreased = newRating < oldRating
        setSmallIcon(if(decreased) R.drawable.ic_rating_down else R.drawable.ic_rating_up)
        setContentTitle("$handle new rating: $newRating")
        val difference = signedToString(newRating - oldRating)
        setContentText("$difference (rank: $rank)")
        setSubText("${accountManager.type.name} rating changes")
        color = accountManager.originalColor(accountManager.getHandleColor(newRating)).toArgb() //TODO not original but cpsColors
        if (url != null) setContentIntent(makePendingIntentOpenURL(url, accountManager.context))
        if (time != null) {
            setShowWhen(true)
            setWhen(time.toEpochMilliseconds())
        }
    }
}