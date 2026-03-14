package com.demich.cps.profiles.managers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.profiles.HandleColor
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.RatedUserInfo
import com.demich.cps.profiles.userinfo.handle
import com.demich.cps.ui.theme.CPSColors
import com.demich.cps.ui.theme.cpsColors

fun RatedAccountManager<*>.getHandleColor(rating: Int): HandleColor =
    ratingsUpperBounds
        .firstOrNull { rating < it.ratingUpperBound }
        ?.handleColor ?: RED

context(manager: RatedAccountManager<*>)
fun CPSColors.colorFor(handleColor: HandleColor): Color =
    if (useOriginalHandleColors) manager.originalColor(handleColor)
    else handleColor(handleColor)

context(manager: RatedAccountManager<*>)
fun CPSColors.colorFor(rating: Int): Color =
    colorFor(handleColor = manager.getHandleColor(rating))

@Composable
@ReadOnlyComposable
fun RatedAccountManager<*>.colorFor(handleColor: HandleColor): Color =
    cpsColors.colorFor(handleColor = handleColor)

fun RatedAccountManager<*>.makeOKSpan(text: String, rating: Int?, cpsColors: CPSColors): AnnotatedString =
    if (rating == null) AnnotatedString(text = text)
    else makeRatedSpan(text, rating, cpsColors)

@Composable
@ReadOnlyComposable
fun <U: RatedUserInfo> RatedAccountManager<U>.makeHandleSpan(profileResult: ProfileResult<U>): AnnotatedString =
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