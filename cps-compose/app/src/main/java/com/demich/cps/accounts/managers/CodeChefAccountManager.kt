package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.accounts.SmallRatedAccountPanel
import com.demich.cps.accounts.rating_graph.RatingGraph
import com.demich.cps.accounts.rating_graph.RatingLoadButton
import com.demich.cps.accounts.rating_graph.rememberRatingGraphUIStates
import com.demich.cps.accounts.userinfo.CodeChefUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.platforms.api.CodeChefApi
import com.demich.cps.platforms.api.CodeChefRatingChange
import com.demich.cps.platforms.api.isRedirect
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.platforms.utils.CodeChefUtils
import com.demich.cps.utils.append
import kotlin.text.contains


class CodeChefAccountManager(context: Context):
    RatedAccountManager<CodeChefUserInfo>(context, AccountManagers.codechef),
    UserSuggestionsProvider
{
    companion object {
        private const val star = "★"
    }

    override val urlHomePage get() = CodeChefApi.urls.main

    override fun isValidForSearch(char: Char) = isValidForUserId(char)
    override fun isValidForUserId(char: Char) = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "._" -> true
        else -> false
    }

    override suspend fun downloadInfo(data: String): CodeChefUserInfo =
        CodeChefUtils.runCatching {
            extractUserInfo(
                source = CodeChefApi.getUserPage(handle = data),
                handle = data
            )
        }.getOrElse { e ->
            if (e.isRedirect) CodeChefUserInfo(status = STATUS.NOT_FOUND, handle = data)
            else CodeChefUserInfo(status = STATUS.FAILED, handle = data)
        }

    override suspend fun loadSuggestions(str: String): List<UserSuggestion> =
        CodeChefApi.getSuggestions(str).list.map {
            UserSuggestion(
                userId = it.username,
                info = it.rating.toString()
            )
        }

    override suspend fun loadRatingHistory(userId: String): List<RatingChange> =
        CodeChefApi.getRatingChanges(handle = userId)
            .map(CodeChefRatingChange::toRatingChange)

    override val ratingsUpperBounds = arrayOf(
        HandleColor.GRAY to 1400,
        HandleColor.GREEN to 1600,
        HandleColor.BLUE to 1800,
        HandleColor.VIOLET to 2000,
        HandleColor.YELLOW to 2200,
        HandleColor.ORANGE to 2500
    )

    override fun originalColor(handleColor: HandleColor): Color =
        when (handleColor) {
            HandleColor.GRAY -> Color(0xFF666666)
            HandleColor.GREEN -> Color(0xFF1E7D22)
            HandleColor.BLUE -> Color(0xFF3366CC)
            HandleColor.VIOLET -> Color(0xFF684273)
            HandleColor.YELLOW -> Color(255, 191, 0)
            HandleColor.ORANGE -> Color(255, 127, 0)
            HandleColor.RED -> Color(208,1,27)
            else -> throw HandleColor.UnknownHandleColorException(handleColor, this)
        }

    override val rankedHandleColorsList = HandleColor.rankedCodeChef

    private fun getRatingStarNumber(rating: Int): Int {
        val index = ratingsUpperBounds.indexOfFirst { rating < it.second }
        return if (index == -1) ratingsUpperBounds.size + 1 else index + 1
    }

    @Composable
    override fun makeRatedSpan(text: String, rating: Int) = buildAnnotatedString {
        append(text = "${getRatingStarNumber(rating)}$star ", color = colorFor(rating = rating))
        append(text)
    }

    @Composable
    private fun StarBox(
        modifier: Modifier = Modifier,
        rating: Int,
        textColor: Color,
        fontSize: TextUnit
    ) {
        Text(
            modifier = modifier
                .background(color = colorFor(rating = rating))
                .padding(horizontal = 4.dp, vertical = 1.dp),
            text = buildAnnotatedString {
                append(getRatingStarNumber(rating).toString())
                appendInlineContent(star)
            },
            color = textColor,
            fontSize = fontSize,
            inlineContent = mapOf(
                star to InlineTextContent(
                    Placeholder(
                        width = fontSize,
                        height = fontSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    Icon(
                        imageVector = CPSIcons.Star,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        )
    }

    @Composable
    override fun PanelContent(userInfo: CodeChefUserInfo) {
        SmallRatedAccountPanel(
            userInfo = userInfo,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    userInfo.rating?.let {
                        StarBox(
                            rating = it,
                            textColor = cpsColors.background,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text(
                        text = userInfo.handle,
                        fontSize = 30.sp
                    )
                }
            },
            additionalTitle = {
                //TODO: code copied from default (except rated color)
                if (userInfo.status == STATUS.OK) {
                    Text(
                        text = userInfo.ratingToString(),
                        fontSize = 25.sp,
                        color = if (userInfo.hasRating()) cpsColors.content else cpsColors.contentAdditional,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
    }

    @Composable
    override fun ExpandedContent(
        userInfo: CodeChefUserInfo,
        setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
        modifier: Modifier
    ) {
        val ratingGraphUIStates = rememberRatingGraphUIStates()
        Box(modifier = modifier) {
            PanelContent(userInfo)
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

    override fun getDataStore() = simpleAccountDataStore(context)
}