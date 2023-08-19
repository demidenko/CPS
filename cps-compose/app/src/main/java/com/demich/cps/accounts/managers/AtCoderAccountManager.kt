package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.demich.cps.accounts.HandleColor
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.accounts.SmallRatedAccountPanel
import com.demich.cps.accounts.rating_graph.RatingGraph
import com.demich.cps.accounts.rating_graph.RatingLoadButton
import com.demich.cps.accounts.rating_graph.rememberRatingGraphUIStates
import com.demich.cps.accounts.to
import com.demich.cps.accounts.userinfo.AtCoderUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.api.AtCoderApi
import com.demich.cps.platforms.api.AtCoderRatingChange
import com.demich.cps.platforms.api.isPageNotFound
import com.demich.cps.platforms.utils.AtCoderUtils
import com.demich.cps.ui.SettingsSwitchItemWithWork
import com.demich.cps.utils.context
import com.demich.cps.utils.jsonCPS
import com.demich.cps.workers.AccountsWorker
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper


class AtCoderAccountManager :
    RatedAccountManager<AtCoderUserInfo>(AccountManagerType.atcoder),
    AccountSettingsProvider,
    UserSuggestionsProvider
{
    override val urlHomePage get() = AtCoderApi.urls.main

    override fun isValidForUserId(char: Char): Boolean = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "_" -> true
        else -> false
    }

    override suspend fun downloadInfo(data: String): AtCoderUserInfo {
        return AtCoderUtils.runCatching {
            extractUserInfo(source = AtCoderApi.getUserPage(handle = data))
        }.getOrElse { e ->
            if (e.isPageNotFound) AtCoderUserInfo(status = STATUS.NOT_FOUND, handle = data)
            else AtCoderUserInfo(status = STATUS.FAILED, handle = data)
        }
    }

    override suspend fun loadSuggestions(str: String): List<UserSuggestion> =
        AtCoderUtils.extractUserSuggestions(source = AtCoderApi.getSuggestionsPage(str))

    override suspend fun loadRatingHistory(userId: String): List<RatingChange> =
        AtCoderApi.getRatingChanges(handle = userId).map {
            it.toRatingChange(handle = userId)
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
            else -> throw UnknownHandleColorException(handleColor, this)
        }

    @Composable
    override fun ExpandedContent(
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
                RatingLoadButton(userInfo, ratingGraphUIStates)
            }
        }
    }

    override fun dataStore(context: Context) = AtCoderAccountDataStore(context)
    override fun getSettings(context: Context) = AtCoderAccountSettingsDataStore(context)

    @Composable
    override fun SettingsItems() {
        val settings = getSettings(context)
        SettingsSwitchItemWithWork(
            item = settings.observeRating,
            title = "Rating changes observer",
            workGetter = AccountsWorker::getWork,
            stopWorkOnUnchecked = false
        )
    }

    fun notifyRatingChange(handle: String, ratingChange: AtCoderRatingChange, context: Context) =
        notifyRatingChange(
            channel = notificationChannels.atcoder.rating_changes,
            ratingChange = ratingChange.toRatingChange(handle),
            handle = handle,
            manager = this,
            context = context
        )
}

class AtCoderAccountDataStore(context: Context):
    AccountUniqueDataStore<AtCoderUserInfo>(context.account_atcoder_dataStore)
{
    companion object {
        private val Context.account_atcoder_dataStore by dataStoreWrapper(AccountManagerType.atcoder.name)
    }

    override val userInfo: DataStoreItem<AtCoderUserInfo?>
        get() = jsonCPS.item(name = "user_info", defaultValue = null)

    val lastRatedContestId = itemStringNullable(name = "last_rated_contest")
}

class AtCoderAccountSettingsDataStore(context: Context):
    ItemizedDataStore(context.account_settings_atcoder_dataStore)
{
    companion object {
        private val Context.account_settings_atcoder_dataStore
            by dataStoreWrapper(AccountManagerType.atcoder.name + "_account_settings")
    }

    val observeRating = itemBoolean(name = "observe_rating", defaultValue = false)

}