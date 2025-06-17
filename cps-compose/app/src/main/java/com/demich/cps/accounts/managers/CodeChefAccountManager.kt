package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.SmallAccountPanelTwoLines
import com.demich.cps.accounts.screens.CodeChefUserInfoExpandedContent
import com.demich.cps.accounts.until
import com.demich.cps.accounts.userinfo.CodeChefUserInfo
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.accounts.userinfo.ratingToString
import com.demich.cps.accounts.userinfo.userInfoOrNull
import com.demich.cps.platforms.api.CodeChefApi
import com.demich.cps.platforms.api.isRedirect
import com.demich.cps.platforms.utils.CodeChefUtils
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.theme.CPSColors
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.append
import com.demich.kotlin_stdlib_boost.partitionIndex


class CodeChefAccountManager :
    RatedAccountManager<CodeChefUserInfo>(AccountManagerType.codechef),
    ProfileSuggestionsProvider
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

    override suspend fun fetchProfile(data: String): ProfileResult<CodeChefUserInfo> =
        CodeChefUtils.runCatching {
            ProfileResult.Success(
                userInfo = extractUserInfo(
                    source = CodeChefApi.getUserPage(handle = data),
                    handle = data
                )
            )
        }.getOrElse { e ->
            if (e.isRedirect) ProfileResult.NotFound(data)
            else ProfileResult.Failed(data)
        }

    override suspend fun fetchSuggestions(str: String): List<UserSuggestion> =
        CodeChefApi.getSuggestions(str).list.map {
            UserSuggestion(
                userId = it.username,
                info = it.rating.toString()
            )
        }

    override suspend fun getRatingChanges(userId: String): List<RatingChange> =
        CodeChefApi.getRatingChanges(handle = userId).map { it.toRatingChange() }

    override val ratingsUpperBounds by lazy {
        listOf(
            HandleColor.GRAY until 1400,
            HandleColor.GREEN until 1600,
            HandleColor.BLUE until 1800,
            HandleColor.VIOLET until 2000,
            HandleColor.YELLOW until 2200,
            HandleColor.ORANGE until 2500
        )
    }

    override fun originalColor(handleColor: HandleColor): Color =
        when (handleColor) {
            HandleColor.GRAY -> Color(0xFF666666)
            HandleColor.GREEN -> Color(0xFF1E7D22)
            HandleColor.BLUE -> Color(0xFF3366CC)
            HandleColor.VIOLET -> Color(0xFF684273)
            HandleColor.YELLOW -> Color(255, 191, 0)
            HandleColor.ORANGE -> Color(255, 127, 0)
            HandleColor.RED -> Color(208,1,27)
            else -> throw UnknownHandleColorException(handleColor, this)
        }

    override val rankedHandleColorsList = HandleColor.rankedCodeChef

    private fun getRatingStarNumber(rating: Int): Int {
        return ratingsUpperBounds.partitionIndex { rating >= it.ratingUpperBound } + 1
    }

    override fun makeRatedSpan(text: String, rating: Int, cpsColors: CPSColors) =
        buildAnnotatedString {
            append(text = "${getRatingStarNumber(rating)}$star ", color = colorFor(rating, cpsColors))
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
    override fun PanelContent(profileResult: ProfileResult<CodeChefUserInfo>) {
        SmallAccountPanelTwoLines(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    profileResult.userInfoOrNull()?.rating?.let {
                        StarBox(
                            rating = it,
                            textColor = cpsColors.background,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text(
                        text = profileResult.userId,
                        fontSize = 30.sp
                    )
                }
            },
            additionalTitle = {
                //TODO: code copied from default (except rated color)
                if (profileResult is ProfileResult.Success) {
                    val userInfo = profileResult.userInfo
                    Text(
                        text = userInfo.ratingToString(),
                        fontSize = 25.sp,
                        color = if (userInfo.rating != null) cpsColors.content else cpsColors.contentAdditional,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
    }

    @Composable
    override fun ExpandedContent(
        profileResult: ProfileResult<CodeChefUserInfo>,
        setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
        modifier: Modifier
    ) {
        CodeChefUserInfoExpandedContent(
            profileResult = profileResult,
            setBottomBarContent = setBottomBarContent,
            modifier = modifier
        )
    }

    override fun dataStore(context: Context) = simpleProfileDataStore(context)

}