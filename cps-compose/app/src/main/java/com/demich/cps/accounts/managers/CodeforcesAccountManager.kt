package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.NotificationChannels
import com.demich.cps.NotificationIds
import com.demich.cps.R
import com.demich.cps.accounts.SmallRatedAccountPanel
import com.demich.cps.accounts.rating_graph.RatingGraph
import com.demich.cps.accounts.rating_graph.RatingLoadButton
import com.demich.cps.accounts.rating_graph.rememberRatingGraphUIStates
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.platforms.api.*
import com.demich.cps.ui.SettingsSwitchItemWithWork
import com.demich.cps.ui.VotedRating
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.platforms.utils.CodeforcesUtils
import com.demich.cps.utils.append
import com.demich.cps.utils.jsonCPS
import com.demich.cps.workers.AccountsWorker
import com.demich.cps.workers.CodeforcesMonitorLauncherWorker
import com.demich.cps.workers.CodeforcesUpsolvingSuggestionsWorker
import com.demich.datastore_itemized.dataStoreWrapper
import kotlinx.datetime.Instant
import kotlin.text.contains


class CodeforcesAccountManager(context: Context):
    RatedAccountManager<CodeforcesUserInfo>(context, AccountManagers.codeforces),
    AccountSettingsProvider,
    UserSuggestionsProvider,
    RatingRevolutionsProvider
{

    companion object {
        private val Context.account_codeforces_dataStore by dataStoreWrapper(AccountManagers.codeforces.name)
    }

    override val urlHomePage get() = CodeforcesApi.urls.main

    override fun isValidForSearch(char: Char) = isValidForUserId(char)
    override fun isValidForUserId(char: Char) = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "._-" -> true
        else -> false
    }


    override fun emptyInfo() = CodeforcesUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String): CodeforcesUserInfo =
        CodeforcesUtils.getUserInfo(handle = data, doRedirect = true)

    override suspend fun loadSuggestions(str: String): List<UserSuggestion> =
        buildList {
            val s = CodeforcesApi.getHandleSuggestionsPage(str)!!
            CodeforcesUtils.extractHandleSuggestions(source = s) { handle ->
                add(UserSuggestion(title = handle, userId = handle))
            }
        }.asReversed()

    override suspend fun loadRatingHistory(info: CodeforcesUserInfo): List<RatingChange> =
        CodeforcesApi.getUserRatingChanges(info.handle)
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
            else -> throw HandleColor.UnknownHandleColorException(handleColor, this)
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

    @Composable
    fun makeHandleSpan(handle: String, tag: CodeforcesColorTag): AnnotatedString =
        buildAnnotatedString {
            append(handle, color = cpsColors.content)
            tag.toHandleColor()?.let { handleColor ->
                addStyle(
                    style = SpanStyle(color = colorFor(handleColor)),
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
    override fun makeRatedSpan(text: String, rating: Int): AnnotatedString =
        makeHandleSpan(
            handle = text,
            tag = CodeforcesColorTag.fromRating(rating)
        )


    @Composable
    override fun ExpandedContent(
        userInfo: CodeforcesUserInfo,
        setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
        modifier: Modifier
    ) {
        val ratingGraphUIStates = rememberRatingGraphUIStates()
        Box(modifier = modifier) {
            Column {
                SmallRatedAccountPanel(userInfo)
                if (userInfo.contribution != 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "contribution:",
                            color = cpsColors.contentAdditional,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 5.dp)
                        )
                        VotedRating(
                            rating = userInfo.contribution,
                            fontSize = 20.sp
                        )
                    }
                }
            }
            RatingGraph(
                ratingGraphUIStates = ratingGraphUIStates,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
        setBottomBarContent {
            //TODO: upsolving list button (icon = Icons.Default.FitnessCenter)
            if (userInfo.hasRating()) {
                RatingLoadButton(userInfo, ratingGraphUIStates)
            }
        }
    }

    override fun getDataStore() = accountDataStore(context.account_codeforces_dataStore)
    override fun getSettings() = CodeforcesAccountSettingsDataStore(this)

    @Composable
    override fun SettingsItems() {
        val settings = remember { getSettings() }
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

    fun notifyRatingChange(ratingChange: CodeforcesRatingChange) =
        notifyRatingChange(
            manager = this,
            notificationChannel = NotificationChannels.codeforces.rating_changes,
            notificationId = NotificationIds.codeforces_rating_changes,
            handle = ratingChange.handle,
            ratingChange = ratingChange.toRatingChange()
        )

    suspend fun applyRatingChange(ratingChange: CodeforcesRatingChange) {
        val info = getSavedInfoOrNull() ?: return

        val settings = getSettings()
        val prevRatingChangeContestId = settings.lastRatedContestId()

        if (prevRatingChangeContestId == ratingChange.contestId && info.rating == ratingChange.newRating) return

        settings.lastRatedContestId(ratingChange.contestId)

        if (prevRatingChangeContestId != null) {
            notifyRatingChange(ratingChange)
            val newInfo = CodeforcesUtils.getUserInfo(handle = info.handle, doRedirect = false)
            if (newInfo.status != STATUS.FAILED) {
                setSavedInfo(newInfo)
            } else {
                setSavedInfo(info.copy(rating = ratingChange.newRating))
            }
        }
    }

    override val ratingUpperBoundRevolutions: List<Pair<Instant, Array<Pair<HandleColor, Int>>>>
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

class CodeforcesAccountSettingsDataStore(manager: CodeforcesAccountManager):
    AccountSettingsDataStore(manager.context.account_settings_codeforces_dataStore)
{
    companion object {
        private val Context.account_settings_codeforces_dataStore
            by dataStoreWrapper(AccountManagers.codeforces.name + "_account_settings")
    }

    val observeRating = itemBoolean(name = "observe_rating", defaultValue = false)
    val lastRatedContestId = itemIntNullable(name = "last_rated_contest")

    val observeContribution = itemBoolean(name = "observe_contribution", defaultValue = false)

    val monitorEnabled = itemBoolean(name = "monitor_enabled", defaultValue = false)
    val monitorLastSubmissionId = itemLongNullable(name = "monitor_last_submission")
    val monitorCanceledContests = jsonCPS.item<List<Pair<Int,Instant>>>(name = "monitor_canceled", defaultValue = emptyList())

    val upsolvingSuggestionsEnabled = itemBoolean(name = "upsolving_suggestions", defaultValue = false)
    val upsolvingSuggestedProblems = jsonCPS.item<List<Pair<CodeforcesProblem, Instant>>>(name = "upsolving_suggested_problems_list", defaultValue = emptyList())

    override fun itemsForReset() = listOf(
        lastRatedContestId,
        monitorLastSubmissionId,
        monitorCanceledContests,
        upsolvingSuggestedProblems
    )

}