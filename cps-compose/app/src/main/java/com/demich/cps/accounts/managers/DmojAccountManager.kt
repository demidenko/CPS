package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.accounts.SmallRatedAccountPanel
import com.demich.cps.accounts.rating_graph.RatingGraph
import com.demich.cps.accounts.rating_graph.RatingLoadButton
import com.demich.cps.accounts.rating_graph.rememberRatingGraphUIStates
import com.demich.cps.accounts.userinfo.DmojUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.platforms.api.DmojApi
import com.demich.cps.platforms.api.DmojRatingChange
import com.demich.cps.platforms.api.isPageNotFound
import com.demich.cps.utils.append
import com.demich.datastore_itemized.dataStoreWrapper


class DmojAccountManager(context: Context):
    RatedAccountManager<DmojUserInfo>(context, AccountManagers.dmoj),
    UserSuggestionsProvider
{
    companion object {
        private val Context.account_dmoj_dataStore by dataStoreWrapper(AccountManagers.dmoj.name)
    }

    override val urlHomePage get() = DmojApi.urls.main

    override fun isValidForUserId(char: Char): Boolean = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "_" -> true
        else -> false
    }

    override fun emptyInfo() = DmojUserInfo(status = STATUS.NOT_FOUND, handle = "")

    override suspend fun downloadInfo(data: String): DmojUserInfo {
        try {
            val res = DmojApi.getUser(handle = data)
            return DmojUserInfo(
                status = STATUS.OK,
                handle = res.username,
                rating = res.rating
            )
        } catch (e: Throwable) {
            if (e.isPageNotFound) {
                return DmojUserInfo(status = STATUS.NOT_FOUND, handle = data)
            }
            return DmojUserInfo(status = STATUS.FAILED, handle = data)
        }
    }

    override suspend fun loadSuggestions(str: String): List<UserSuggestion> {
        return DmojApi.getSuggestions(str).map {
            UserSuggestion(title = it.text, userId = it.id)
        }
    }

    override suspend fun loadRatingHistory(info: DmojUserInfo): List<RatingChange>
        = DmojApi.getRatingChanges(handle = info.handle)
            .map(DmojRatingChange::toRatingChange)

    override val ratingsUpperBounds = arrayOf(
        HandleColor.GRAY to 1000,
        HandleColor.GREEN to 1300,
        HandleColor.BLUE to 1600,
        HandleColor.VIOLET to 1900,
        HandleColor.YELLOW to 2400
    )

    override val rankedHandleColorsList = HandleColor.rankedDmoj

    override fun originalColor(handleColor: HandleColor) =
        when (handleColor) {
            HandleColor.GRAY -> Color(0xFF999999)
            HandleColor.GREEN -> Color(0xff00a900)
            HandleColor.BLUE -> Color(0xFF0000FF)
            HandleColor.VIOLET -> Color(0xFF800080)
            HandleColor.YELLOW -> Color(0xFFFFB100)
            HandleColor.RED -> Color(0xFFEE0000)
            else -> throw HandleColor.UnknownHandleColorException(handleColor, this)
        }

    @Composable
    override fun makeRatedSpan(text: String, rating: Int): AnnotatedString {
        if (rating < 3000) return super.makeRatedSpan(text, rating)
        return buildAnnotatedString {
            append(text[0].toString(), fontWeight = FontWeight.Bold)
            append(super.makeRatedSpan(text.drop(1), rating))
        }
    }

    @Composable
    override fun ExpandedContent(
        userInfo: DmojUserInfo,
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

    override fun getDataStore() = accountDataStore(context.account_dmoj_dataStore)
}
