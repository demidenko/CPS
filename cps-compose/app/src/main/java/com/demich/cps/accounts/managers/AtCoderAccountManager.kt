package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.NotificationChannels
import com.demich.cps.NotificationIds
import com.demich.cps.utils.AtCoderAPI
import com.demich.cps.utils.AtCoderRatingChange
import kotlinx.serialization.Serializable

@Serializable
data class AtCoderUserInfo(
    override val status: STATUS,
    val handle: String,
    val rating: Int = NOT_RATED
): UserInfo() {
    override val userId: String
        get() = handle

    override fun link(): String = AtCoderAPI.URLFactory.user(handle)
}

class AtCoderAccountManager(context: Context):
    RatedAccountManager<AtCoderUserInfo>(context, manager_name),
    AccountSettingsProvider
{
    companion object {
        const val manager_name = "atcoder"
        private val Context.account_atcoder_dataStore by preferencesDataStore(manager_name)
    }

    override val urlHomePage get() = AtCoderAPI.URLFactory.main

    override fun isValidForUserId(char: Char): Boolean = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "_" -> true
        else -> false
    }

    override fun emptyInfo() = AtCoderUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String, flags: Int): AtCoderUserInfo {
        try {
            val s = AtCoderAPI.getUserPage(handle = data)
            var i = s.lastIndexOf("class=\"username\"")
            i = s.indexOf("</span", i)
            val handle = s.substring(s.lastIndexOf('>',i)+1, i)
            i = s.indexOf("<th class=\"no-break\">Rating</th>")
            val rating = if (i == -1) NOT_RATED
            else {
                i = s.indexOf("</span", i)
                s.substring(s.lastIndexOf('>',i)+1, i).toInt()
            }
            return AtCoderUserInfo(status = STATUS.OK, handle = handle, rating = rating)
        } catch (e: AtCoderAPI.AtCoderPageNotFoundException) {
            return AtCoderUserInfo(status = STATUS.NOT_FOUND, handle = data)
        } catch (e: Throwable) {
            return AtCoderUserInfo(status = STATUS.FAILED, handle = data)
        }
    }

    override suspend fun loadRatingHistory(info: AtCoderUserInfo): List<RatingChange>? =
        AtCoderAPI.getRatingChanges(info.userId)?.map {
            RatingChange(
                rating = it.NewRating,
                date = it.EndTime
            )
        }

    override fun getRating(userInfo: AtCoderUserInfo) = userInfo.rating

    override val ratingsUpperBounds = arrayOf(
        HandleColor.GRAY to 400,
        HandleColor.BROWN to 800,
        HandleColor.GREEN to 1200,
        HandleColor.CYAN to 1600,
        HandleColor.BLUE to 2000,
        HandleColor.YELLOW to 2400,
        HandleColor.ORANGE to 2800
    )

    override val rankedHandleColorsList = HandleColor.rankedAtCoder

    override fun originalColor(handleColor: HandleColor): Color =
        when(handleColor) {
            HandleColor.GRAY -> Color(0xFF808080)
            HandleColor.BROWN -> Color(0xFF804000)
            HandleColor.GREEN -> Color(0xFF008000)
            HandleColor.CYAN -> Color(0xFF00C0C0)
            HandleColor.BLUE -> Color(0xFF0000FF)
            HandleColor.YELLOW -> Color(0xFFC0C000)
            HandleColor.ORANGE -> Color(0xFFFF8000)
            HandleColor.RED -> Color(0xFFFF0000)
            else -> throw HandleColor.UnknownHandleColorException(handleColor, this)
        }

    @Composable
    override fun makeOKInfoSpan(userInfo: AtCoderUserInfo): AnnotatedString =
        buildAnnotatedString {
            require(userInfo.status == STATUS.OK)
            withStyle(SpanStyle(
                color = colorFor(userInfo),
                fontWeight = if (userInfo.rating != NOT_RATED) FontWeight.Bold else null
            )) {
                append(userInfo.handle)
                append(' ')
                if (userInfo.rating != NOT_RATED) append(userInfo.rating.toString())
                else append("[not rated]")
            }
        }

    override fun getDataStore() = accountDataStore(context.account_atcoder_dataStore, emptyInfo())
    override fun getSettings() = AtCoderAccountSettingsDataStore(this)

    fun notifyRatingChange(handle: String, ratingChange: AtCoderRatingChange) = notifyRatingChange(
        this,
        NotificationChannels.atcoder_rating_changes,
        NotificationIds.atcoder_rating_changes,
        handle,
        ratingChange.NewRating,
        ratingChange.OldRating,
        ratingChange.Place,
        AtCoderAPI.URLFactory.userContestResult(handle, ratingChange.getContestId()),
        ratingChange.EndTime
    )
}

class AtCoderAccountSettingsDataStore(manager: AtCoderAccountManager):
    AccountSettingsDataStore(manager.context.account_settings_atcoder_dataStore)
{
    companion object {
        private val Context.account_settings_atcoder_dataStore
            by preferencesDataStore(AtCoderAccountManager.manager_name + "_account_settings")
    }

    val observeRating = Item(booleanPreferencesKey("observe_rating"), false)
    val lastRatedContestId = ItemNullable(stringPreferencesKey("last_rated_contest"))

    override val keysForReset = listOf(lastRatedContestId)

}