package com.demich.cps.accounts.managers

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.HandleColorBound
import com.demich.cps.accounts.SmallRatedAccountPanel
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.accounts.userinfo.ratingToString
import com.demich.cps.ui.theme.CPSColors
import kotlinx.datetime.Instant

abstract class RatedAccountManager<U: RatedUserInfo>(): AccountManager<U>() {
    override val userIdTitle get() = "handle"

    abstract val ratingsUpperBounds: List<HandleColorBound>
    fun getHandleColor(rating: Int): HandleColor =
        ratingsUpperBounds
            .firstOrNull { rating < it.ratingUpperBound }?.handleColor
            ?: HandleColor.RED

    abstract fun originalColor(handleColor: HandleColor): Color

    fun colorFor(handleColor: HandleColor, cpsColors: CPSColors): Color =
        if (cpsColors.useOriginalHandleColors) originalColor(handleColor)
        else cpsColors.handleColor(handleColor)

    fun colorFor(rating: Int, cpsColors: CPSColors): Color =
        colorFor(handleColor = getHandleColor(rating), cpsColors = cpsColors)

    open fun makeRatedSpan(text: String, rating: Int, cpsColors: CPSColors): AnnotatedString =
        AnnotatedString(
            text = text,
            spanStyle = SpanStyle(
                color = colorFor(rating = rating, cpsColors = cpsColors),
                fontWeight = FontWeight.Bold
            )
        )

    fun makeOKSpan(text: String, rating: Int?, cpsColors: CPSColors): AnnotatedString =
        if (rating == null) AnnotatedString(text = text)
        else makeRatedSpan(text, rating, cpsColors)

    final override fun makeUserInfoSpan(userInfo: U, cpsColors: CPSColors): AnnotatedString =
        with(userInfo) {
            makeOKSpan(
                text = handle + " " + ratingToString(),
                rating = rating,
                cpsColors = cpsColors
            )
        }

    @Composable
    override fun PanelContent(profileResult: ProfileResult<U>) =
        SmallRatedAccountPanel(profileResult)

    abstract val rankedHandleColorsList: Array<HandleColor>

    protected abstract suspend fun getRatingChanges(userId: String): List<RatingChange>
    suspend fun getRatingChangeHistory(userId: String): List<RatingChange> =
        getRatingChanges(userId).sortedBy { it.date }

}


class UnknownHandleColorException(color: HandleColor, manager: RatedAccountManager<*>):
    IllegalArgumentException("Manager ${manager.type.name} does not support color ${color.name}")


interface RatingRevolutionsProvider {
    //list of (last time, bounds)
    val ratingUpperBoundRevolutions: List<Pair<Instant, List<HandleColorBound>>>
}
