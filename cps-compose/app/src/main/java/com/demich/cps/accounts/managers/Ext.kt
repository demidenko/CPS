package com.demich.cps.accounts.managers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.ui.theme.CPSColors
import com.demich.cps.ui.theme.cpsColors


context(manager: RatedAccountManager<U>)
fun <U: RatedUserInfo> CPSColors.colorFor(handleColor: HandleColor): Color =
    if (useOriginalHandleColors) manager.originalColor(handleColor)
    else handleColor(handleColor)

context(manager: RatedAccountManager<U>)
fun <U: RatedUserInfo> CPSColors.colorFor(rating: Int): Color =
    colorFor(handleColor = manager.getHandleColor(rating))

@Composable
@ReadOnlyComposable
fun RatedAccountManager<*>.colorFor(handleColor: HandleColor): Color =
    cpsColors.colorFor(handleColor = handleColor)

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
        AnnotatedString(text = profileResult.userId)
    }