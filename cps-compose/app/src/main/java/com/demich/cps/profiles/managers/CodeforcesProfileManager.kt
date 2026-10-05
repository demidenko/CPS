package com.demich.cps.profiles.managers

import android.content.Context
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.demich.cps.LocalCodeforcesProfileManager
import com.demich.cps.R
import com.demich.cps.notifications.NotificationChannelSingleId
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.Platform
import com.demich.cps.platforms.api.codeforces.CodeforcesApiAccess
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import com.demich.cps.platforms.api.codeforces.getUserOrNull
import com.demich.cps.platforms.api.codeforces.models.CodeforcesProblem
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRatingChange
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.platforms.utils.codeforces.CodeforcesHandle
import com.demich.cps.platforms.utils.codeforces.CodeforcesRatingColorTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesUnratedTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesUserTag
import com.demich.cps.platforms.utils.codeforces.getHandleSuggestions
import com.demich.cps.platforms.utils.codeforces.toUserInfo
import com.demich.cps.profiles.RatingChange
import com.demich.cps.profiles.RatingColor
import com.demich.cps.profiles.screens.CodeforcesUserInfoExpandedContent
import com.demich.cps.profiles.toRatingChange
import com.demich.cps.profiles.until
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.UserSuggestion
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.settings.ApiAccessSettingsItem
import com.demich.cps.ui.settings.SettingsContainerScope
import com.demich.cps.ui.settings.SwitchByProfilesWork
import com.demich.cps.ui.settings.SwitchByWork
import com.demich.cps.ui.theme.CPSColors
import com.demich.cps.ui.theme.CPSRatingColors
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.backgroundCoroutineScope
import com.demich.cps.utils.context
import com.demich.cps.utils.emptyTimedCollection
import com.demich.cps.utils.jsonCPS
import com.demich.cps.workers.CodeforcesMonitorLauncherWorker
import com.demich.cps.workers.CodeforcesUpsolvingSuggestionsWorker
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.combine
import com.demich.datastore_itemized.edit
import com.demich.datastore_itemized.flowOf
import com.demich.datastore_itemized.value
import com.demich.kotlin_stdlib_boost.binarySearchFirstFalse
import kotlinx.coroutines.launch
import kotlin.time.Instant


class CodeforcesProfileManager :
    RatedProfileManager<CodeforcesUserInfo>(),
    ProfileSettingsProvider,
    ProfileSuggestionsProvider,
    RatingRevolutionsProvider
{
    override val platform: Platform get() = codeforces
    override val urlHomePage get() = CodeforcesUrls.main

    override fun isValidForSearch(char: Char) = isValidForUserId(char)
    override fun isValidForUserId(char: Char) = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "._-" -> true
        else -> false
    }

    override suspend fun getUserInfo(userId: String): CodeforcesUserInfo? =
        CodeforcesClient()
            .getUserOrNull(handle = userId, checkHistoricHandles = true)
            ?.toUserInfo()

    override suspend fun getSuggestions(str: String): List<UserSuggestion> =
        CodeforcesClient().getHandleSuggestions(str = str)
            .map { UserSuggestion(title = it.handle, userId = it.handle) }
            .toList()

    override suspend fun getRatingChanges(userId: String): List<RatingChange> =
        CodeforcesClient().getUserRatingChanges(handle = userId).map { it.toRatingChange() }

    override val ratingsUpperBounds by lazy(mode = NONE) {
        ratingUpperBounds()
    }

    override val rankedRatingColors = RatingColor.rankedCodeforces

    override fun platformColor(ratingColor: RatingColor): Color? =
        when (ratingColor) {
            GRAY -> Color(0xFF808080)
            GREEN -> Color(0xFF008000)
            CYAN -> Color(0xFF03A89E)
            BLUE -> Color(0xFF0000FF)
            VIOLET -> Color(0xFFAA00AA)
            YELLOW -> Color(0xFFBBBB00)
            ORANGE -> Color(0xFFFF8C00)
            RED -> Color(0xFFFF0000)
            else -> null
        }

    fun makeHandleSpan(handle: String, tag: CodeforcesUserTag, cpsColors: CPSRatingColors): AnnotatedString =
        buildAnnotatedString {
            append(handle)
            if (tag is CodeforcesRatingColorTag) {
                addStyle(
                    style = SpanStyle(color = cpsColors.colorFor(tag.toRatingColor())),
                    start = if (tag == LEGENDARY) 1 else 0,
                    end = handle.length
                )
            }
            if (tag !is CodeforcesUnratedTag) {
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
            tag = CodeforcesRatingColorTag.fromRating(rating),
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

    override fun profileStorage(context: Context) = CodeforcesProfileStorage(this, context)
    override fun settingsStorage(context: Context) = CodeforcesProfileSettingsDataStore(context)

    @Composable
    context(scope: SettingsContainerScope)
    override fun SettingsItems() {
        val context = context
        CodeforcesSettingsItems(
            settings = settingsStorage(context),
            profileStorage = profileStorage(context)
        )
    }

    override val ratingUpperBoundRevolutions
        get() = listOf(
            //https://codeforces.com/blog/entry/59228
            Instant.fromEpochSeconds(1525364996L) to listOf(
                RatingColor.GRAY until 1200,
                RatingColor.GREEN until 1400,
                RatingColor.CYAN until 1600,
                RatingColor.BLUE until 1900,
                RatingColor.VIOLET until 2200,
                RatingColor.ORANGE until 2400
            ),
            //https://codeforces.com/blog/entry/20638
            Instant.fromEpochSeconds(1443721088L) to listOf(
                RatingColor.GRAY until 1200,
                RatingColor.GREEN until 1500,
                RatingColor.BLUE until 1700,
                RatingColor.VIOLET until 1900,
                RatingColor.ORANGE until 2200
            ),
            //https://codeforces.com/blog/entry/3064
            Instant.fromEpochSeconds(1320620562L) to listOf(
                RatingColor.GRAY until 1200,
                RatingColor.GREEN until 1500,
                RatingColor.BLUE until 1650,
                RatingColor.VIOLET until 1800,
                RatingColor.ORANGE until 2000
            ),
            //https://codeforces.com/blog/entry/1383
            Instant.fromEpochSeconds(1298914585L) to listOf(
                RatingColor.GRAY until 1200,
                RatingColor.GREEN until 1500,
                RatingColor.BLUE until 1650,
                RatingColor.YELLOW until 2000
            )
            //https://codeforces.com/blog/entry/126
        )

}

private fun CodeforcesRatingColorTag.toRatingColor(): RatingColor =
    when (this) {
        GRAY -> GRAY
        GREEN -> GREEN
        CYAN -> CYAN
        BLUE -> BLUE
        VIOLET -> VIOLET
        ORANGE -> ORANGE
        RED, LEGENDARY -> RED
    }

@Composable
@ReadOnlyComposable
fun CodeforcesHandle.toHandleSpan() =
    LocalCodeforcesProfileManager.current
        .makeHandleSpan(handle = handle, tag = userTag, cpsColors = cpsColors)


class CodeforcesProfileStorage(manager: CodeforcesProfileManager, context: Context):
    RatedProfileStorage<CodeforcesUserInfo>(manager, context, context.dataStore)
{
    companion object {
        private val Context.dataStore by profileDataStoreWrapper(platform = codeforces)
    }

    override val profileItem = makeProfileItem<CodeforcesUserInfo>()

    override val ratingChangeNotificationChannel: NotificationChannelSingleId
        get() = notificationChannels.codeforces.rating_changes

    override fun CodeforcesUserInfo.copyRating(rating: Int) = copy(rating = rating)


    val monitorLastSubmissionId = itemLongNullable(name = "monitor_last_submission")
    val monitorCanceledContests = jsonCPS.item(name = "monitor_canceled", defaultValue = emptyTimedCollection<Int>())

    val upsolvingSuggestedProblems = jsonCPS.item(name = "upsolving_suggested_problems", defaultValue = emptyTimedCollection<CodeforcesProblem>())

    val apiAccessKey = itemString(name = "api_key")
    val apiAccessSecret = itemString(name = "api_secret")
    val apiAccess = combine {
        val key = apiAccessKey.value
        val secret = apiAccessSecret.value
        if (key.isBlank() && secret.isBlank()) null
        else CodeforcesApiAccess(key = key, secret = secret)
    }
}

suspend fun CodeforcesProfileStorage.applyRatingChange(ratingChange: CodeforcesRatingChange) =
    applyRatingChange(ratingChange = ratingChange.toRatingChange())

class CodeforcesProfileSettingsDataStore(context: Context):
    ItemizedDataStore(context.dataStore)
{
    companion object {
        private val Context.dataStore by profileSettingsDataStoreWrapper(platform = codeforces)
    }

    val observeRating = itemBoolean(name = "observe_rating", defaultValue = false)
    val observeContribution = itemBoolean(name = "observe_contribution", defaultValue = false)
    val monitorEnabled = itemBoolean(name = "monitor_enabled", defaultValue = false)
    val upsolvingSuggestionsEnabled = itemBoolean(name = "upsolving_suggestions", defaultValue = false)
}

fun CodeforcesProfileSettingsDataStore.flowOfNotificationsRequired() =
    flowOf {
        observeRating.value ||
        monitorEnabled.value ||
        upsolvingSuggestionsEnabled.value ||
        observeContribution.value
    }

private fun ratingUpperBounds() =
    listOf<CodeforcesRatingColorTag>(
        GRAY,
        GREEN,
        CYAN,
        BLUE,
        VIOLET,
        ORANGE
    ).map { colorTag ->
        // bs can be optimized if iterate from orange to gray
        // but it speedups whole function only from 3.5us to 2.5us
        val rating = binarySearchFirstFalse(first = 0, last = Int.MAX_VALUE) { rating ->
            CodeforcesRatingColorTag.fromRating(rating) <= colorTag
        }
        val ratingColor = checkNotNull(colorTag.toRatingColor())
        ratingColor until rating
    }

@Composable
context(scope: SettingsContainerScope)
private fun CodeforcesSettingsItems(
    settings: CodeforcesProfileSettingsDataStore,
    profileStorage: CodeforcesProfileStorage
) {
    SwitchByProfilesWork(
        item = settings.observeRating,
        title = "Rating changes observer"
    )
    SwitchByWork(
        item = settings.monitorEnabled,
        title = "Contest monitor",
        description = stringResource(id = R.string.cf_contest_watcher_description),
        workProvider = CodeforcesMonitorLauncherWorker
    )
    SwitchByWork(
        item = settings.upsolvingSuggestionsEnabled,
        title = "Upsolving suggestions",
        workProvider = CodeforcesUpsolvingSuggestionsWorker
    )
    SwitchByProfilesWork(
        item = settings.observeContribution,
        title = "Contribution changes observer"
    )
    CodeforcesApiAccessSettingsItem(
        profileStorage = profileStorage
    )
}

@Composable
context(scope: SettingsContainerScope)
private fun CodeforcesApiAccessSettingsItem(
    profileStorage: CodeforcesProfileStorage
) {
    val uriHandler = LocalUriHandler.current
    val scope = backgroundCoroutineScope

    ApiAccessSettingsItem(
        item = profileStorage.apiAccess,
        itemTitle = "Api access",
        itemSubtitle = {
            Text(
                text = when {
                    it == null -> "undefined"
                    it.key.isBlank() -> "key is empty"
                    it.secret.isBlank() -> "secret is empty"
                    else -> "ok"
                }
            )
        },
        dialogTitle = "codeforces::api",
        fields = listOf(
            "api-key" to CodeforcesApiAccess::key,
            "secret" to CodeforcesApiAccess::secret
        ),
        decode = { list ->
            CodeforcesApiAccess(key = list[0], secret = list[1])
        },
        onSave = {
            scope.launch {
                profileStorage.edit {
                    apiAccessKey.value = it.key
                    apiAccessSecret.value = it.secret
                }
            }
        },
        onHelp = {
            uriHandler.openUri(CodeforcesUrls.apiUserSettings)
        },
        checkBlock = {
            val _ = CodeforcesClient(apiAccess = it).getUserFriends(onlyOnline = false)
        }
    )
}