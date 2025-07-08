package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.demich.cps.LocalCodeforcesAccountManager
import com.demich.cps.R
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.screens.CodeforcesUserInfoExpandedContent
import com.demich.cps.accounts.until
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.notifications.NotificationChannelSingleId
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import com.demich.cps.platforms.api.codeforces.models.CodeforcesColorTag
import com.demich.cps.platforms.api.codeforces.models.CodeforcesProblem
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRatingChange
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.platforms.utils.codeforces.CodeforcesHandle
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.cps.platforms.utils.codeforces.getHandleSuggestions
import com.demich.cps.platforms.utils.codeforces.getProfile
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.settings.SettingsContainerScope
import com.demich.cps.ui.settings.SwitchByProfilesWork
import com.demich.cps.ui.settings.SwitchByWork
import com.demich.cps.ui.theme.CPSColors
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.append
import com.demich.cps.utils.context
import com.demich.cps.utils.emptyTimedCollection
import com.demich.cps.utils.jsonCPS
import com.demich.cps.workers.CodeforcesMonitorLauncherWorker
import com.demich.cps.workers.CodeforcesUpsolvingSuggestionsWorker
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.flowOf
import com.demich.datastore_itemized.value
import com.demich.kotlin_stdlib_boost.binarySearchFirstFalse
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant


class CodeforcesAccountManager :
    RatedAccountManager<CodeforcesUserInfo>(),
    ProfileSettingsProvider,
    ProfileSuggestionsProvider,
    RatingRevolutionsProvider
{
    override val type get() = AccountManagerType.codeforces
    override val urlHomePage get() = CodeforcesUrls.main

    override fun isValidForSearch(char: Char) = isValidForUserId(char)
    override fun isValidForUserId(char: Char) = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "._-" -> true
        else -> false
    }


    override suspend fun fetchProfile(data: String): ProfileResult<CodeforcesUserInfo> =
        CodeforcesClient.getProfile(handle = data, recoverHandle = true)

    override suspend fun fetchSuggestions(str: String): List<UserSuggestion> =
        buildList {
            CodeforcesClient.getHandleSuggestions(str = str) { handle ->
                add(UserSuggestion(title = handle, userId = handle))
            }
        }.asReversed()

    override suspend fun getRatingChanges(userId: String): List<RatingChange> =
        CodeforcesClient.getUserRatingChanges(handle = userId).map { it.toRatingChange() }

    override val ratingsUpperBounds by lazy {
        listOf(
            CodeforcesColorTag.GRAY,
            CodeforcesColorTag.GREEN,
            CodeforcesColorTag.CYAN,
            CodeforcesColorTag.BLUE,
            CodeforcesColorTag.VIOLET,
            CodeforcesColorTag.ORANGE
        ).map { colorTag ->
            //TODO: bs can be optimized if iterate from orange to gray
            val rating = binarySearchFirstFalse(first = 0, last = Int.MAX_VALUE) { rating ->
                CodeforcesUtils.colorTagFrom(rating) <= colorTag
            }
            val handleColor = requireNotNull(colorTag.toHandleColor())
            handleColor until rating
        }
    }

    override val rankedHandleColors = HandleColor.rankedCodeforces

    override fun originalColor(handleColor: HandleColor): Color =
        when (handleColor) {
            HandleColor.GRAY -> Color(0xFF808080)
            HandleColor.GREEN -> Color(0xFF008000)
            HandleColor.CYAN -> Color(0xFF03A89E)
            HandleColor.BLUE -> Color(0xFF0000FF)
            HandleColor.VIOLET -> Color(0xFFAA00AA)
            HandleColor.YELLOW -> Color(0xFFBBBB00)
            HandleColor.ORANGE -> Color(0xFFFF8C00)
            HandleColor.RED -> Color(0xFFFF0000)
            else -> illegalHandleColorError(handleColor)
        }

    private fun CodeforcesColorTag.toHandleColor(): HandleColor? =
        when (this) {
            CodeforcesColorTag.GRAY -> HandleColor.GRAY
            CodeforcesColorTag.GREEN -> HandleColor.GREEN
            CodeforcesColorTag.CYAN -> HandleColor.CYAN
            CodeforcesColorTag.BLUE -> HandleColor.BLUE
            CodeforcesColorTag.VIOLET -> HandleColor.VIOLET
            CodeforcesColorTag.ORANGE -> HandleColor.ORANGE
            CodeforcesColorTag.RED, CodeforcesColorTag.LEGENDARY -> HandleColor.RED
            CodeforcesColorTag.BLACK, CodeforcesColorTag.ADMIN -> null
        }

    fun makeHandleSpan(handle: String, tag: CodeforcesColorTag, cpsColors: CPSColors): AnnotatedString =
        buildAnnotatedString {
            append(handle, color = cpsColors.content)
            tag.toHandleColor()?.let { handleColor ->
                addStyle(
                    style = SpanStyle(color = colorFor(handleColor, cpsColors)),
                    start = if (tag == CodeforcesColorTag.LEGENDARY) 1 else 0,
                    end = handle.length
                )
            }
            if (tag != CodeforcesColorTag.BLACK) {
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = 0,
                    end = handle.length
                )
            }
        }

    override fun makeRatedSpan(text: String, rating: Int, cpsColors: CPSColors): AnnotatedString =
        makeHandleSpan(
            handle = text,
            tag = CodeforcesUtils.colorTagFrom(rating),
            cpsColors = cpsColors
        )


    @Composable
    override fun ExpandedContent(
        profileResult: ProfileResult<CodeforcesUserInfo>,
        setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
        modifier: Modifier
    ) {
        CodeforcesUserInfoExpandedContent(
            profileResult = profileResult,
            setBottomBarContent = setBottomBarContent,
            modifier = modifier
        )
    }

    override fun dataStore(context: Context) = CodeforcesProfileDataStore(this, context)
    override fun getSettings(context: Context) = CodeforcesProfileSettingsDataStore(context)

    @Composable
    context(scope: SettingsContainerScope)
    override fun SettingsItems() {
        val settings = getSettings(context)
        scope.SwitchByProfilesWork(
            item = settings.observeRating,
            title = "Rating changes observer"
        )
        scope.SwitchByWork(
            item = settings.monitorEnabled,
            title = "Contest monitor",
            description = stringResource(id = R.string.cf_contest_watcher_description),
            workProvider = CodeforcesMonitorLauncherWorker
        )
        scope.SwitchByWork(
            item = settings.upsolvingSuggestionsEnabled,
            title = "Upsolving suggestions",
            workProvider = CodeforcesUpsolvingSuggestionsWorker
        )
        scope.SwitchByProfilesWork(
            item = settings.observeContribution,
            title = "Contribution changes observer"
        )
    }

    override fun flowOfRequiredNotificationsPermission(context: Context): Flow<Boolean> =
        getSettings(context).flowOf {
            observeRating.value or
            monitorEnabled.value or
            upsolvingSuggestionsEnabled.value or
            observeContribution.value
        }

    suspend fun applyRatingChange(ratingChange: CodeforcesRatingChange, context: Context) {
        dataStore(context).applyRatingChange(ratingChange = ratingChange.toRatingChange())
    }

    override val ratingUpperBoundRevolutions
        get() = listOf(
            //https://codeforces.com/blog/entry/59228
            Instant.fromEpochSeconds(1525364996L) to listOf(
                HandleColor.GRAY until 1200,
                HandleColor.GREEN until 1400,
                HandleColor.CYAN until 1600,
                HandleColor.BLUE until 1900,
                HandleColor.VIOLET until 2200,
                HandleColor.ORANGE until 2400
            ),
            //https://codeforces.com/blog/entry/20638
            Instant.fromEpochSeconds(1443721088L) to listOf(
                HandleColor.GRAY until 1200,
                HandleColor.GREEN until 1500,
                HandleColor.BLUE until 1700,
                HandleColor.VIOLET until 1900,
                HandleColor.ORANGE until 2200
            ),
            //https://codeforces.com/blog/entry/3064
            Instant.fromEpochSeconds(1320620562L) to listOf(
                HandleColor.GRAY until 1200,
                HandleColor.GREEN until 1500,
                HandleColor.BLUE until 1650,
                HandleColor.VIOLET until 1800,
                HandleColor.ORANGE until 2000
            ),
            //https://codeforces.com/blog/entry/1383
            Instant.fromEpochSeconds(1298914585L) to listOf(
                HandleColor.GRAY until 1200,
                HandleColor.GREEN until 1500,
                HandleColor.BLUE until 1650,
                HandleColor.YELLOW until 2000
            )
            //https://codeforces.com/blog/entry/126
        )

}

@Composable
@ReadOnlyComposable
fun CodeforcesHandle.toHandleSpan() =
    LocalCodeforcesAccountManager.current
        .makeHandleSpan(handle = handle, tag = colorTag, cpsColors = cpsColors)


class CodeforcesProfileDataStore(manager: CodeforcesAccountManager, context: Context):
    RatedProfileDataStore<CodeforcesUserInfo>(manager, context, context.dataStore)
{
    companion object {
        private val Context.dataStore by profileDataStoreWrapper(AccountManagerType.codeforces)
    }

    override val profileItem = makeProfileItem<CodeforcesUserInfo>()

    override val ratingChangeNotificationChannel: NotificationChannelSingleId
        get() = notificationChannels.codeforces.rating_changes

    override fun CodeforcesUserInfo.withNewRating(rating: Int) = copy(rating = rating)


    val monitorLastSubmissionId = itemLongNullable(name = "monitor_last_submission")
    val monitorCanceledContests = jsonCPS.item(name = "monitor_canceled", defaultValue = emptyTimedCollection<Int>())

    val upsolvingSuggestedProblems = jsonCPS.item(name = "upsolving_suggested_problems", defaultValue = emptyTimedCollection<CodeforcesProblem>())
}

class CodeforcesProfileSettingsDataStore(context: Context):
    ItemizedDataStore(context.dataStore)
{
    companion object {
        private val Context.dataStore by profileSettingsDataStoreWrapper(AccountManagerType.codeforces)
    }

    val observeRating = itemBoolean(name = "observe_rating", defaultValue = false)
    val observeContribution = itemBoolean(name = "observe_contribution", defaultValue = false)
    val monitorEnabled = itemBoolean(name = "monitor_enabled", defaultValue = false)
    val upsolvingSuggestionsEnabled = itemBoolean(name = "upsolving_suggestions", defaultValue = false)

}