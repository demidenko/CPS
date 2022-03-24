package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.NotificationChannels
import com.demich.cps.NotificationIds
import com.demich.cps.utils.InstantAsSecondsSerializer
import com.demich.cps.utils.codeforces.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


@Serializable
@Immutable
data class CodeforcesUserInfo(
    override val status: STATUS,
    val handle: String,
    val rating: Int = NOT_RATED,
    val contribution: Int = 0,
    @Serializable(with = InstantAsSecondsSerializer::class)
    val lastOnlineTime: Instant = Instant.DISTANT_PAST
): UserInfo() {
    constructor(codeforcesUser: CodeforcesUser): this(
        status = STATUS.OK,
        handle = codeforcesUser.handle,
        rating = codeforcesUser.rating,
        contribution = codeforcesUser.contribution,
        lastOnlineTime = codeforcesUser.lastOnlineTime
    )

    override val userId: String
        get() = handle

    override fun link(): String = CodeforcesAPI.URLFactory.user(handle)
}


class CodeforcesAccountManager(context: Context):
    RatedAccountManager<CodeforcesUserInfo>(context, manager_name),
    AccountSettingsProvider,
    AccountSuggestionsProvider,
    RatingRevolutionsProvider
{

    companion object {
        const val manager_name = "codeforces"
        private val Context.account_codeforces_dataStore by preferencesDataStore(manager_name)
    }

    override val urlHomePage get() = CodeforcesAPI.URLFactory.main

    override fun isValidForSearch(char: Char) = isValidForUserId(char)
    override fun isValidForUserId(char: Char) = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "._-" -> true
        else -> false
    }


    override fun emptyInfo() = CodeforcesUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String, flags: Int): CodeforcesUserInfo {
        try {
            return CodeforcesUserInfo(CodeforcesAPI.getUser(handle = data))
        } catch (e: Throwable) {
            if (e is CodeforcesAPIErrorResponse && e.isHandleNotFound() == data) {
                if((flags and 1) != 0) {
                    val (realHandle, status) = CodeforcesUtils.getRealHandle(handle = data)
                    return when(status) {
                        STATUS.OK -> downloadInfo(data = realHandle, flags = 0)
                        else -> CodeforcesUserInfo(status = status, handle = data)
                    }
                } else {
                    return CodeforcesUserInfo(status = STATUS.NOT_FOUND, handle = data)
                }
            } else {
                return CodeforcesUserInfo(status = STATUS.FAILED, handle = data)
            }
        }
    }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion>? {
        val s = CodeforcesAPI.getHandleSuggestions(str) ?: return null
        return withContext(Dispatchers.IO) {
            buildList {
                s.splitToSequence('\n').filter { !it.contains('=') }.forEach {
                    val i = it.indexOf('|')
                    if (i != -1) {
                        val handle = it.substring(i + 1)
                        add(AccountSuggestion(handle, "", handle))
                    }
                }
            }
        }
    }

    override suspend fun loadRatingHistory(info: CodeforcesUserInfo): List<RatingChange>? =
        kotlin.runCatching {
            CodeforcesAPI.getUserRatingChanges(info.handle)
        }.getOrNull()?.map {
            RatingChange(
                rating = it.newRating,
                date = it.ratingUpdateTime
            )
        }

    override fun getRating(userInfo: CodeforcesUserInfo) = userInfo.rating

    override val ratingsUpperBounds = arrayOf(
        HandleColor.GRAY to 1200,
        HandleColor.GREEN to 1400,
        HandleColor.CYAN to 1600,
        HandleColor.BLUE to 1900,
        HandleColor.VIOLET to 2100,
        HandleColor.ORANGE to 2400
    )

    override val rankedHandleColorsList = HandleColor.rankedCodeforces

    override fun originalColor(handleColor: HandleColor): Color =
        when (handleColor) {
            HandleColor.GRAY -> Color(0xFF808080)
            HandleColor.GREEN -> Color(0xFF008000)
            HandleColor.CYAN -> Color(0xFF03A89E)
            HandleColor.BLUE -> Color(0xFF0000FF)
            HandleColor.VIOLET -> Color(0xFFAA00AA)
            HandleColor.ORANGE -> Color(0xFFFF8C00)
            HandleColor.RED -> Color(0xFFFF0000)
            else -> throw HandleColor.UnknownHandleColorException(handleColor, this)
        }

    @Composable
    fun makeHandleSpan(handle: String, tag: CodeforcesUtils.ColorTag): AnnotatedString =
        buildAnnotatedString {
            append(handle)
            CodeforcesUtils.getHandleColorByTag(tag)?.let { handleColor ->
                addStyle(
                    style = SpanStyle(color = colorFor(handleColor)),
                    start = if(tag == CodeforcesUtils.ColorTag.LEGENDARY) 1 else 0,
                    end = handle.length
                )
            }
            if (tag != CodeforcesUtils.ColorTag.BLACK) {
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = 0,
                    end = handle.length
                )
            }
        }

    @Composable
    override fun makeHandleSpan(userInfo: CodeforcesUserInfo): AnnotatedString =
        makeHandleSpan(
            handle = userInfo.handle,
            tag = CodeforcesUtils.getTagByRating(userInfo.rating)
        )

    @Composable
    override fun makeOKInfoSpan(userInfo: CodeforcesUserInfo): AnnotatedString =
        buildAnnotatedString {
            require(userInfo.status == STATUS.OK)
            append(makeHandleSpan(userInfo.copy(
                handle = userInfo.handle
                        + " "
                        + (userInfo.rating.takeIf { it != NOT_RATED }?.toString() ?: "[not rated]")
            )))
        }

    override fun getDataStore() = accountDataStore(context.account_codeforces_dataStore, emptyInfo())
    override fun getSettings() = CodeforcesAccountSettingsDataStore(this)

    fun notifyRatingChange(ratingChange: CodeforcesRatingChange) = notifyRatingChange(
        this,
        NotificationChannels.codeforces_rating_changes,
        NotificationIds.codeforces_rating_changes,
        ratingChange.handle,
        ratingChange.newRating,
        ratingChange.oldRating,
        ratingChange.rank,
        CodeforcesAPI.URLFactory.contestsWith(ratingChange.handle),
        ratingChange.ratingUpdateTime
    )

    suspend fun applyRatingChange(ratingChange: CodeforcesRatingChange) {
        val info = getSavedInfo()

        val settings = getSettings()
        val prevRatingChangeContestId = settings.lastRatedContestId()

        if(prevRatingChangeContestId == ratingChange.contestId && info.rating == ratingChange.newRating) return

        settings.lastRatedContestId(ratingChange.contestId)

        if(prevRatingChangeContestId != null) {
            notifyRatingChange(ratingChange)
            val newInfo = loadInfo(info.handle)
            if(newInfo.status!= STATUS.FAILED) {
                setSavedInfo(newInfo)
            } else {
                setSavedInfo(info.copy(rating = ratingChange.newRating))
            }
        }
    }

    override val ratingUpperBoundRevolutions: List<Pair<Instant, Array<Pair<Int, HandleColor>>>>
        get() = listOf(
            //https://codeforces.com/blog/entry/59228
            Instant.fromEpochSeconds(1525364996L) to arrayOf(
                1200 to HandleColor.GRAY,
                1400 to HandleColor.GREEN,
                1600 to HandleColor.CYAN,
                1900 to HandleColor.BLUE,
                2200 to HandleColor.VIOLET,
                2400 to HandleColor.ORANGE
            ),
            //https://codeforces.com/blog/entry/20638
            Instant.fromEpochSeconds(1443721088L) to arrayOf(
                1200 to HandleColor.GRAY,
                1500 to HandleColor.GREEN,
                1700 to HandleColor.BLUE,
                1900 to HandleColor.VIOLET,
                2200 to HandleColor.ORANGE
            ),
            //https://codeforces.com/blog/entry/3064
            Instant.fromEpochSeconds(1320620562L) to arrayOf(
                1200 to HandleColor.GRAY,
                1500 to HandleColor.GREEN,
                1650 to HandleColor.BLUE,
                1800 to HandleColor.VIOLET,
                2000 to HandleColor.ORANGE
            ),
            //https://codeforces.com/blog/entry/1383
            Instant.fromEpochSeconds(1298914585L) to arrayOf(
                1200 to HandleColor.GRAY,
                1500 to HandleColor.GREEN,
                1650 to HandleColor.BLUE,
                2000 to HandleColor.YELLOW
            )
            //https://codeforces.com/blog/entry/126
        )
}

class CodeforcesAccountSettingsDataStore(manager: CodeforcesAccountManager):
    AccountSettingsDataStore(manager.context.account_settings_codeforces_dataStore)
{
    companion object {
        private val Context.account_settings_codeforces_dataStore by preferencesDataStore(
            CodeforcesAccountManager.manager_name + "_account_settings")
    }

    val observeRating = Item(booleanPreferencesKey("observe_rating"), false)
    val lastRatedContestId = ItemNullable(intPreferencesKey("last_rated_contest"))

    val observeContribution = Item(booleanPreferencesKey("observe_contribution"), false)

    val contestWatchEnabled = Item(booleanPreferencesKey("contest_watch"), false)
    val contestWatchLastSubmissionId = ItemNullable(longPreferencesKey("contest_watch_last_submission"))
    val contestWatchStartedContestId = ItemNullable(intPreferencesKey("contest_watch_started_contest"))
    val contestWatchCanceled = itemJsonConvertible<List<Pair<Int,Instant>>>(name = "contest_watch_canceled", defaultValue = emptyList())

    val upsolvingSuggestionsEnabled = Item(booleanPreferencesKey("upsolving_suggestions"), false)
    val upsolvingSuggestedProblems = itemJsonConvertible<List<Pair<Int,String>>>(name = "upsolving_suggested_problems_list", defaultValue = emptyList())

    override val keysForReset get() = listOf(
        lastRatedContestId,
        contestWatchLastSubmissionId,
        contestWatchStartedContestId,
        contestWatchCanceled,
        upsolvingSuggestedProblems
    )

}