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
import com.demich.cps.R
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.screens.CodeforcesUserInfoExpandedContent
import com.demich.cps.accounts.to
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.api.CodeforcesColorTag
import com.demich.cps.platforms.api.CodeforcesProblem
import com.demich.cps.platforms.api.CodeforcesRatingChange
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.cps.ui.SettingsSwitchItemWithWork
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.theme.CPSColors
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.append
import com.demich.cps.utils.context
import com.demich.cps.utils.jsonCPS
import com.demich.cps.workers.AccountsWorker
import com.demich.cps.workers.CodeforcesMonitorLauncherWorker
import com.demich.cps.workers.CodeforcesUpsolvingSuggestionsWorker
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import kotlinx.datetime.Instant
import kotlin.text.contains


class CodeforcesAccountManager :
    RatedAccountManager<CodeforcesUserInfo>(AccountManagerType.codeforces),
    AccountSettingsProvider,
    UserSuggestionsProvider,
    RatingRevolutionsProvider
{
    override val urlHomePage get() = CodeforcesApi.urls.main

    override fun isValidForSearch(char: Char) = isValidForUserId(char)
    override fun isValidForUserId(char: Char) = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "._-" -> true
        else -> false
    }


    override suspend fun getUserInfo(data: String): CodeforcesUserInfo =
        CodeforcesUtils.getUserInfo(handle = data, doRedirect = true)

    override suspend fun getSuggestions(str: String): List<UserSuggestion> =
        buildList {
            val s = CodeforcesApi.getHandleSuggestionsPage(str)
            CodeforcesUtils.extractHandleSuggestions(source = s) { handle ->
                add(UserSuggestion(title = handle, userId = handle))
            }
        }.asReversed()

    override suspend fun getRatingChanges(userId: String): List<RatingChange> =
        CodeforcesApi.getUserRatingChanges(handle = userId)
            .map(CodeforcesRatingChange::toRatingChange)

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
            HandleColor.YELLOW -> Color(0xFFBBBB00)
            HandleColor.ORANGE -> Color(0xFFFF8C00)
            HandleColor.RED -> Color(0xFFFF0000)
            else -> throw UnknownHandleColorException(handleColor, this)
        }

    private fun CodeforcesColorTag.toHandleColor(): HandleColor? {
        return when (this) {
            CodeforcesColorTag.GRAY -> HandleColor.GRAY
            CodeforcesColorTag.GREEN -> HandleColor.GREEN
            CodeforcesColorTag.CYAN -> HandleColor.CYAN
            CodeforcesColorTag.BLUE -> HandleColor.BLUE
            CodeforcesColorTag.VIOLET -> HandleColor.VIOLET
            CodeforcesColorTag.ORANGE -> HandleColor.ORANGE
            CodeforcesColorTag.RED, CodeforcesColorTag.LEGENDARY -> HandleColor.RED
            else -> null
        }
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

    @Composable
    @ReadOnlyComposable
    fun makeHandleSpan(handle: String, tag: CodeforcesColorTag): AnnotatedString =
        makeHandleSpan(handle = handle, tag = tag, cpsColors = cpsColors)

    override fun makeRatedSpan(text: String, rating: Int, cpsColors: CPSColors): AnnotatedString =
        makeHandleSpan(
            handle = text,
            tag = CodeforcesColorTag.fromRating(rating),
            cpsColors = cpsColors
        )


    @Composable
    override fun ExpandedContent(
        userInfo: CodeforcesUserInfo,
        setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
        modifier: Modifier
    ) = CodeforcesUserInfoExpandedContent(
        userInfo = userInfo,
        setBottomBarContent = setBottomBarContent,
        modifier = modifier
    )

    override fun dataStore(context: Context) = CodeforcesAccountDataStore(context)
    override fun getSettings(context: Context) = CodeforcesAccountSettingsDataStore(context)

    @Composable
    override fun SettingsItems() {
        val settings = getSettings(context)
        SettingsSwitchItemWithWork(
            item = settings.observeRating,
            title = "Rating changes observer",
            workGetter = AccountsWorker::getWork,
            stopWorkOnUnchecked = false
        )
        SettingsSwitchItemWithWork(
            item = settings.monitorEnabled,
            title = "Contest monitor",
            description = stringResource(id = R.string.cf_contest_watcher_description),
            workGetter = CodeforcesMonitorLauncherWorker::getWork
        )
        SettingsSwitchItemWithWork(
            item = settings.upsolvingSuggestionsEnabled,
            title = "Upsolving suggestions",
            workGetter = CodeforcesUpsolvingSuggestionsWorker::getWork
        )
        SettingsSwitchItemWithWork(
            item = settings.observeContribution,
            title = "Contribution changes observer",
            workGetter = AccountsWorker::getWork,
            stopWorkOnUnchecked = false
        )
    }

    private fun notifyRatingChange(ratingChange: CodeforcesRatingChange, context: Context) =
        notifyRatingChange(
            channel = notificationChannels.codeforces.rating_changes,
            ratingChange = ratingChange.toRatingChange(),
            handle = ratingChange.handle,
            manager = this,
            context = context
        )

    suspend fun applyRatingChange(ratingChange: CodeforcesRatingChange, context: Context) {
        val dataStore = dataStore(context)
        val info = dataStore.getSavedInfo() ?: return

        val prevRatingChangeContestId = dataStore.lastRatedContestId()

        if (prevRatingChangeContestId == ratingChange.contestId && info.rating == ratingChange.newRating) return

        dataStore.lastRatedContestId(ratingChange.contestId)

        if (prevRatingChangeContestId != null) {
            notifyRatingChange(ratingChange, context)
            val newInfo = CodeforcesUtils.getUserInfo(handle = info.handle, doRedirect = false)
            if (newInfo.status != STATUS.FAILED) {
                dataStore.setSavedInfo(newInfo)
            } else {
                dataStore.setSavedInfo(info.copy(rating = ratingChange.newRating))
            }
        }
    }

    override val ratingUpperBoundRevolutions
        get() = listOf(
            //https://codeforces.com/blog/entry/59228
            Instant.fromEpochSeconds(1525364996L) to arrayOf(
                HandleColor.GRAY to 1200,
                HandleColor.GREEN to 1400,
                HandleColor.CYAN to 1600,
                HandleColor.BLUE to 1900,
                HandleColor.VIOLET to 2200,
                HandleColor.ORANGE to 2400
            ),
            //https://codeforces.com/blog/entry/20638
            Instant.fromEpochSeconds(1443721088L) to arrayOf(
                HandleColor.GRAY to 1200,
                HandleColor.GREEN to 1500,
                HandleColor.BLUE to 1700,
                HandleColor.VIOLET to 1900,
                HandleColor.ORANGE to 2200
            ),
            //https://codeforces.com/blog/entry/3064
            Instant.fromEpochSeconds(1320620562L) to arrayOf(
                HandleColor.GRAY to 1200,
                HandleColor.GREEN to 1500,
                HandleColor.BLUE to 1650,
                HandleColor.VIOLET to 1800,
                HandleColor.ORANGE to 2000
            ),
            //https://codeforces.com/blog/entry/1383
            Instant.fromEpochSeconds(1298914585L) to arrayOf(
                HandleColor.GRAY to 1200,
                HandleColor.GREEN to 1500,
                HandleColor.BLUE to 1650,
                HandleColor.YELLOW to 2000
            )
            //https://codeforces.com/blog/entry/126
        )
}

class CodeforcesAccountDataStore(context: Context):
    AccountUniqueDataStore<CodeforcesUserInfo>(context.account_codeforces_dataStore)
{
    companion object {
        private val Context.account_codeforces_dataStore by dataStoreWrapper(AccountManagerType.codeforces.name)
    }

    override val userInfo = makeUserInfoItem<CodeforcesUserInfo>()

    val lastRatedContestId = itemIntNullable(name = "last_rated_contest")

    val monitorLastSubmissionId = itemLongNullable(name = "monitor_last_submission")
    val monitorCanceledContests = jsonCPS.itemList<Pair<Int,Instant>>(name = "monitor_canceled")

    val upsolvingSuggestedProblems = jsonCPS.itemList<Pair<CodeforcesProblem, Instant>>(name = "upsolving_suggested_problems_list")
}

class CodeforcesAccountSettingsDataStore(context: Context):
    ItemizedDataStore(context.account_settings_codeforces_dataStore)
{
    companion object {
        private val Context.account_settings_codeforces_dataStore
            by dataStoreWrapper(AccountManagerType.codeforces.name + "_account_settings")
    }

    val observeRating = itemBoolean(name = "observe_rating", defaultValue = false)
    val observeContribution = itemBoolean(name = "observe_contribution", defaultValue = false)
    val monitorEnabled = itemBoolean(name = "monitor_enabled", defaultValue = false)
    val upsolvingSuggestionsEnabled = itemBoolean(name = "upsolving_suggestions", defaultValue = false)

}