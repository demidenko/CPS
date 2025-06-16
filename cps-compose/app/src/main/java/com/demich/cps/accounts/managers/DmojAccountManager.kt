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
import com.demich.cps.accounts.to
import com.demich.cps.accounts.userinfo.DmojUserInfo
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.platforms.api.DmojApi
import com.demich.cps.platforms.api.isPageNotFound
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.theme.CPSColors
import com.demich.cps.utils.append


class DmojAccountManager :
    RatedAccountManager<DmojUserInfo>(AccountManagerType.dmoj),
    ProfileSuggestionsProvider
{
    override val urlHomePage get() = DmojApi.urls.main

    override fun isValidForUserId(char: Char): Boolean = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "_" -> true
        else -> false
    }

    override suspend fun getUserInfo(data: String): DmojUserInfo =
        DmojApi.runCatching {
            val res = getUser(handle = data)
            DmojUserInfo(
                status = STATUS.OK,
                handle = res.username,
                rating = res.rating
            )
        }.getOrElse {
            if (it.isPageNotFound) {
                return DmojUserInfo(status = STATUS.NOT_FOUND, handle = data)
            }
            return DmojUserInfo(status = STATUS.FAILED, handle = data)
        }

    override suspend fun fetchSuggestions(str: String): List<UserSuggestion> {
        return DmojApi.getSuggestions(str).map {
            UserSuggestion(title = it.text, userId = it.id)
        }
    }

    override suspend fun getRatingChanges(userId: String): List<RatingChange> =
        DmojApi.getRatingChanges(handle = userId).map { it.toRatingChange() }

    override val ratingsUpperBounds by lazy {
        listOf(
            HandleColor.GRAY to 1000,
            HandleColor.GREEN to 1300,
            HandleColor.BLUE to 1600,
            HandleColor.VIOLET to 1900,
            HandleColor.ORANGE to 2400
        )
    }

    override val rankedHandleColorsList = HandleColor.rankedDmoj

    override fun originalColor(handleColor: HandleColor) =
        when (handleColor) {
            HandleColor.GRAY -> Color(0xFF999999)
            HandleColor.GREEN -> Color(0xff00a900)
            HandleColor.BLUE -> Color(0xFF0000FF)
            HandleColor.VIOLET -> Color(0xFF800080)
            HandleColor.ORANGE -> Color(0xFFFFB100)
            HandleColor.RED -> Color(0xFFEE0000)
            else -> throw UnknownHandleColorException(handleColor, this)
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

    override fun dataStore(context: Context) = simpleAccountDataStore(context)
}
