package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.screens.AtCoderUserInfoExpandedContent
import com.demich.cps.accounts.to
import com.demich.cps.accounts.userinfo.AtCoderUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.notifications.NotificationChannelSingleId
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.api.AtCoderApi
import com.demich.cps.platforms.api.isPageNotFound
import com.demich.cps.platforms.utils.AtCoderUtils
import com.demich.cps.ui.SettingsSwitchItemWithAccountsWork
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.utils.context
import com.demich.cps.workers.AccountsWorker
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import kotlinx.coroutines.flow.Flow


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

    override suspend fun getUserInfo(data: String): AtCoderUserInfo {
        return AtCoderUtils.runCatching {
            extractUserInfo(source = AtCoderApi.getUserPage(handle = data))
        }.getOrElse { e ->
            if (e.isPageNotFound) AtCoderUserInfo(status = STATUS.NOT_FOUND, handle = data)
            else AtCoderUserInfo(status = STATUS.FAILED, handle = data)
        }
    }

    override suspend fun getSuggestions(str: String): List<UserSuggestion> =
        AtCoderUtils.extractUserSuggestions(source = AtCoderApi.getSuggestionsPage(str))

    override suspend fun getRatingChanges(userId: String): List<RatingChange> =
        AtCoderApi.getRatingChanges(handle = userId).map {
            it.toRatingChange(handle = userId)
        }

    override val ratingsUpperBounds by lazy {
        listOf(
            HandleColor.GRAY to 400,
            HandleColor.BROWN to 800,
            HandleColor.GREEN to 1200,
            HandleColor.CYAN to 1600,
            HandleColor.BLUE to 2000,
            HandleColor.YELLOW to 2400,
            HandleColor.ORANGE to 2800
        )
    }

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
    ) = AtCoderUserInfoExpandedContent(
        userInfo = userInfo,
        setBottomBarContent = setBottomBarContent,
        modifier = modifier
    )

    override fun dataStore(context: Context) = AtCoderAccountDataStore(this, context)
    override fun getSettings(context: Context) = AtCoderAccountSettingsDataStore(context)

    @Composable
    override fun SettingsItems() {
        val settings = getSettings(context)
        SettingsSwitchItemWithAccountsWork(
            item = settings.observeRating,
            title = "Rating changes observer"
        )
    }

    override fun flowOfRequiredNotificationsPermission(context: Context): Flow<Boolean> =
        AtCoderAccountSettingsDataStore(context).observeRating.flow

}

class AtCoderAccountDataStore(manager: AtCoderAccountManager, context: Context):
    RatedAccountDataStore<AtCoderUserInfo>(manager, context, context.account_atcoder_dataStore)
{
    companion object {
        private val Context.account_atcoder_dataStore by dataStoreWrapper(AccountManagerType.atcoder.name)
    }

    override val userInfo = makeUserInfoItem<AtCoderUserInfo>()

    override val ratingChangeNotificationChannel: NotificationChannelSingleId
        get() = notificationChannels.atcoder.rating_changes

    override fun AtCoderUserInfo.withNewRating(rating: Int) = copy(rating = rating)
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