package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.screens.DmojUserInfoExpandedContent
import com.demich.cps.accounts.until
import com.demich.cps.accounts.userinfo.DmojUserInfo
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.platforms.api.dmoj.DmojUrls
import com.demich.cps.platforms.clients.DmojClient
import com.demich.cps.platforms.clients.isPageNotFound
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.theme.CPSColors
import com.demich.cps.utils.append


class DmojAccountManager :
    RatedAccountManager<DmojUserInfo>(),
    ProfileSuggestionsProvider
{
    override val type get() = AccountManagerType.dmoj
    override val urlHomePage get() = DmojUrls.main

    override fun isValidForUserId(char: Char): Boolean = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "_" -> true
        else -> false
    }

    override suspend fun fetchProfile(data: String): ProfileResult<DmojUserInfo> =
        DmojClient.runCatching {
            val res = getUser(handle = data)
            ProfileResult(DmojUserInfo(handle = res.username, rating = res.rating))
        }.getOrElse {
            if (it.isPageNotFound) ProfileResult.NotFound(data)
            else ProfileResult.Failed(data)
        }

    override suspend fun fetchSuggestions(str: String): List<UserSuggestion> {
        return DmojClient.getSuggestions(str).map {
            UserSuggestion(title = it.text, userId = it.id)
        }
    }

    override suspend fun getRatingChanges(userId: String): List<RatingChange> =
        DmojClient.getRatingChanges(handle = userId).map { it.toRatingChange() }

    override val ratingsUpperBounds by lazy {
        listOf(
            HandleColor.GRAY until 1000,
            HandleColor.GREEN until 1300,
            HandleColor.BLUE until 1600,
            HandleColor.VIOLET until 1900,
            HandleColor.ORANGE until 2400
        )
    }

    override val rankedHandleColors = HandleColor.rankedDmoj

    override fun originalColor(handleColor: HandleColor) =
        when (handleColor) {
            HandleColor.GRAY -> Color(0xFF999999)
            HandleColor.GREEN -> Color(0xff00a900)
            HandleColor.BLUE -> Color(0xFF0000FF)
            HandleColor.VIOLET -> Color(0xFF800080)
            HandleColor.ORANGE -> Color(0xFFFFB100)
            HandleColor.RED -> Color(0xFFEE0000)
            else -> illegalHandleColorError(handleColor)
        }

    override fun makeRatedSpan(text: String, rating: Int, cpsColors: CPSColors): AnnotatedString {
        if (rating < 3000) return super.makeRatedSpan(text, rating, cpsColors)
        return buildAnnotatedString {
            append(text[0].toString(), fontWeight = FontWeight.Bold)
            append(super.makeRatedSpan(text.drop(1), rating, cpsColors))
        }
    }

    @Composable
    override fun ExpandedContent(
        profileResult: ProfileResult<DmojUserInfo>,
        setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
        modifier: Modifier
    ) {
        DmojUserInfoExpandedContent(
            profileResult = profileResult,
            setBottomBarContent = setBottomBarContent,
            modifier = modifier
        )
    }

    override fun dataStore(context: Context) = simpleProfileDataStore(context)

}
