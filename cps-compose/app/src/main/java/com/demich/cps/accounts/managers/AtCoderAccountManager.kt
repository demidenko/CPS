package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.screens.AtCoderUserInfoExpandedContent
import com.demich.cps.accounts.to
import com.demich.cps.accounts.userinfo.AtCoderUserInfo
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.accounts.userinfo.toStatusUserInfo
import com.demich.cps.notifications.NotificationChannelSingleId
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.api.AtCoderApi
import com.demich.cps.platforms.api.isPageNotFound
import com.demich.cps.platforms.utils.AtCoderUtils
import com.demich.cps.ui.SettingsSwitchItemWithProfilesWork
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.utils.context
import com.demich.datastore_itemized.ItemizedDataStore
import kotlinx.coroutines.flow.Flow


class AtCoderAccountManager :
    RatedAccountManager<AtCoderUserInfo>(AccountManagerType.atcoder),
    ProfileSettingsProvider,
    ProfileSuggestionsProvider
{
    override val urlHomePage get() = AtCoderApi.urls.main

    override fun isValidForUserId(char: Char): Boolean = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "_" -> true
        else -> false
    }

    override suspend fun fetchProfile(data: String): ProfileResult<AtCoderUserInfo> {
        return AtCoderUtils.runCatching {
            ProfileResult.Success(
                userInfo = extractUserInfo(source = AtCoderApi.getUserPage(handle = data))
            )
        }.getOrElse { e ->
            if (e.isPageNotFound) ProfileResult.NotFound(data)
            else ProfileResult.Failed(data)
        }
    }

    override suspend fun fetchSuggestions(str: String): List<UserSuggestion> =
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
        profileResult: ProfileResult<AtCoderUserInfo>,
        setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
        modifier: Modifier
    ) {
        AtCoderUserInfoExpandedContent(
            profileResult = profileResult,
            setBottomBarContent = setBottomBarContent,
            modifier = modifier
        )
    }

    override fun dataStore(context: Context) = AtCoderProfileDataStore(this, context)
    override fun getSettings(context: Context) = AtCoderProfileSettingsDataStore(context)

    @Composable
    override fun SettingsItems() {
        val settings = getSettings(context)
        SettingsSwitchItemWithProfilesWork(
            item = settings.observeRating,
            title = "Rating changes observer"
        )
    }

    override fun flowOfRequiredNotificationsPermission(context: Context): Flow<Boolean> =
        AtCoderProfileSettingsDataStore(context).observeRating.flow

    override fun convert(profileResult: ProfileResult<AtCoderUserInfo>): AtCoderUserInfo =
        profileResult.toStatusUserInfo()
}

class AtCoderProfileDataStore(manager: AtCoderAccountManager, context: Context):
    RatedProfileDataStore<AtCoderUserInfo>(manager, context, context.dataStore)
{
    companion object {
        private val Context.dataStore by profileDataStoreWrapper(type = AccountManagerType.atcoder)
    }

    override val userInfo = makeUserInfoItem<AtCoderUserInfo>()
    override fun ProfileResult<AtCoderUserInfo>.convert(): AtCoderUserInfo = toStatusUserInfo()

    override val ratingChangeNotificationChannel: NotificationChannelSingleId
        get() = notificationChannels.atcoder.rating_changes

    override fun AtCoderUserInfo.withNewRating(rating: Int) = copy(rating = rating)
}

class AtCoderProfileSettingsDataStore(context: Context):
    ItemizedDataStore(context.dataStore)
{
    companion object {
        private val Context.dataStore by profileSettingsDataStoreWrapper(AccountManagerType.atcoder)
    }

    val observeRating = itemBoolean(name = "observe_rating", defaultValue = false)

}