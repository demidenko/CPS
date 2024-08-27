package com.demich.cps.accounts.managers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.ui.theme.cpsColors


@Composable
@ReadOnlyComposable
fun RatedAccountManager<*>.colorFor(handleColor: HandleColor): Color =
    colorFor(handleColor = handleColor, cpsColors = cpsColors)

@Composable
@ReadOnlyComposable
fun RatedAccountManager<*>.colorFor(rating: Int): Color =
    colorFor(rating = rating, cpsColors = cpsColors)

@Composable
@ReadOnlyComposable
fun<U: RatedUserInfo> RatedAccountManager<U>.makeHandleSpan(userInfo: U): AnnotatedString =
    with(userInfo) {
        if (status == STATUS.OK) makeOKSpan(text = handle, rating = rating, cpsColors = cpsColors)
        else AnnotatedString(text = handle)
    }