package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.NotificationChannels
import com.demich.cps.NotificationIds
import com.demich.cps.accounts.SmallRatedAccountPanel
import com.demich.cps.accounts.rating_graph.RatingGraph
import com.demich.cps.accounts.rating_graph.RatingLoadButton
import com.demich.cps.accounts.rating_graph.rememberRatingGraphUIStates
import com.demich.cps.ui.*
import com.demich.cps.utils.AtCoderApi
import com.demich.cps.utils.AtCoderRatingChange
import com.demich.cps.workers.AccountsWorker
import com.demich.datastore_itemized.dataStoreWrapper
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup

@Serializable
data class AtCoderUserInfo(
    override val status: STATUS,
    override val handle: String,
    override val rating: Int = NOT_RATED
): RatedUserInfo() {
    override val userPageUrl: String
        get() = AtCoderApi.urls.user(handle)
}

class AtCoderAccountManager(context: Context):
    RatedAccountManager<AtCoderUserInfo>(context, AccountManagers.atcoder),
    AccountSettingsProvider
{
    companion object {
        private val Context.account_atcoder_dataStore by dataStoreWrapper(AccountManagers.atcoder.name)
    }

    override val urlHomePage get() = AtCoderApi.urls.main

    override fun isValidForUserId(char: Char): Boolean = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "_" -> true
        else -> false
    }

    override fun emptyInfo() = AtCoderUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String, flags: Int): AtCoderUserInfo {
        try {
            val s = AtCoderApi.getUserPage(handle = data)
            return with(Jsoup.parse(s)) {
                val handle = expectFirst("a.username").text()
                val rating = select("th.no-break").find { it.text() == "Rating" }
                    ?.nextElementSibling()
                    ?.text()?.toInt() ?: NOT_RATED
                AtCoderUserInfo(status = STATUS.OK, handle = handle, rating = rating)
            }
        } catch (e: Throwable) {
            if (e is ClientRequestException && e.response.status == HttpStatusCode.NotFound) {
                return AtCoderUserInfo(status = STATUS.NOT_FOUND, handle = data)
            }
            return AtCoderUserInfo(status = STATUS.FAILED, handle = data)
        }
    }

    override suspend fun loadRatingHistory(info: AtCoderUserInfo): List<RatingChange> =
        AtCoderApi.getRatingChanges(handle = info.handle).map {
            it.toRatingChange(handle = info.handle)
        }

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
    override fun BigView(
        userInfo: AtCoderUserInfo,
        setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
        modifier: Modifier
    ) {
        val ratingGraphUIStates = rememberRatingGraphUIStates()
        Box(modifier = modifier) {
            SmallRatedAccountPanel(userInfo)
            RatingGraph(
                ratingGraphUIStates = ratingGraphUIStates,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
        setBottomBarContent {
            if (userInfo.hasRating()) {
                RatingLoadButton(ratingGraphUIStates)
            }
        }
    }

    override fun getDataStore() = accountDataStore(context.account_atcoder_dataStore)
    override fun getSettings() = AtCoderAccountSettingsDataStore(this)

    @Composable
    override fun SettingsContent() {
        val settings = remember { getSettings() }
        SettingsSwitchItemWithWork(
            item = settings.observeRating,
            title = "Rating changes observer",
            workGetter = AccountsWorker::getWork,
            stopWorkOnUnchecked = false
        )
    }

    fun notifyRatingChange(handle: String, ratingChange: AtCoderRatingChange) = notifyRatingChange(
        manager = this,
        notificationChannel = NotificationChannels.atcoder.rating_changes,
        notificationId = NotificationIds.atcoder_rating_changes,
        handle = handle,
        ratingChange = ratingChange.toRatingChange(handle)
    )
}

class AtCoderAccountSettingsDataStore(manager: AtCoderAccountManager):
    AccountSettingsDataStore(manager.context.account_settings_atcoder_dataStore)
{
    companion object {
        private val Context.account_settings_atcoder_dataStore
            by dataStoreWrapper(AccountManagers.atcoder.name + "_account_settings")
    }

    val observeRating = itemBoolean(name = "observe_rating", defaultValue = false)
    val lastRatedContestId = itemStringNullable(name = "last_rated_contest")

    override fun itemsForReset() = listOf(lastRatedContestId)

}