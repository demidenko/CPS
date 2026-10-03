package com.demich.cps.profiles.managers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.fetchstate.fetchResultOf
import com.demich.cps.platforms.utils.toProfileResult
import com.demich.cps.profiles.RatingColor
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.RatedUserInfo
import com.demich.cps.profiles.userinfo.UserInfo
import com.demich.cps.profiles.userinfo.handle
import com.demich.cps.ui.theme.CPSColors
import com.demich.cps.ui.theme.cpsColors

fun RatedProfileManager<*>.ratingColorFor(rating: Int): RatingColor =
    ratingsUpperBounds
        .firstOrNull { rating < it.ratingUpperBound }
        ?.ratingColor ?: RED

context(manager: RatedProfileManager<*>)
fun CPSColors.colorFor(ratingColor: RatingColor): Color =
    if (useOriginalHandleColors) manager.platformColorOrThrow(ratingColor)
    else ratingColor(ratingColor)

context(manager: RatedProfileManager<*>)
fun CPSColors.colorFor(rating: Int): Color =
    colorFor(ratingColor = manager.ratingColorFor(rating))

@Composable
@ReadOnlyComposable
fun RatedProfileManager<*>.colorFor(ratingColor: RatingColor): Color =
    cpsColors.colorFor(ratingColor = ratingColor)

fun RatedProfileManager<*>.makeOKSpan(text: String, rating: Int?, cpsColors: CPSColors): AnnotatedString =
    if (rating == null) AnnotatedString(text = text)
    else makeRatedSpan(text, rating, cpsColors)

@Composable
@ReadOnlyComposable
fun <U: RatedUserInfo> RatedProfileManager<U>.makeHandleSpan(profileResult: ProfileResult<U>): AnnotatedString =
    if (profileResult is ProfileResult.Success) {
        val userInfo = profileResult.userInfo
        makeOKSpan(
            text = userInfo.handle,
            rating = userInfo.rating,
            cpsColors = cpsColors
        )
    } else {
        AnnotatedString(text = profileResult.handle)
    }

suspend fun <U: UserInfo> ProfileManager<U>.fetchProfile(userId: String): ProfileResult<U> =
    fetchResultOf { getUserInfo(userId) }.toProfileResult(userId)
