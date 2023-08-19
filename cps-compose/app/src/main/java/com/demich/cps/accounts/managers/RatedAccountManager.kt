package com.demich.cps.accounts.managers

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.HandleColorBound
import com.demich.cps.accounts.SmallRatedAccountPanel
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.ui.theme.CPSColors
import kotlinx.datetime.Instant

abstract class RatedAccountManager<U: RatedUserInfo>(type: AccountManagerType):
    AccountManager<U>(type)
{
    override val userIdTitle get() = "handle"

    abstract val ratingsUpperBounds: Array<HandleColorBound>
    fun getHandleColor(rating: Int): HandleColor =
        ratingsUpperBounds
            .firstOrNull { rating < it.ratingUpperBound }?.handleColor
            ?: HandleColor.RED

    abstract fun originalColor(handleColor: HandleColor): Color

    fun colorFor(handleColor: HandleColor, cpsColors: CPSColors): Color =
        if (cpsColors.useOriginalHandleColors) originalColor(handleColor)
        else cpsColors.handleColor(handleColor)

    @Composable
    open fun makeRatedSpan(text: String, rating: Int) = AnnotatedString(
        text = text,
        spanStyle = SpanStyle(color = colorFor(rating = rating), fontWeight = FontWeight.Bold)
    )

    @Composable
    final override fun makeOKInfoSpan(userInfo: U) = with(userInfo) {
        require(status == STATUS.OK)
        makeOKSpan(
            text = handle + " " + ratingToString(),
            rating = rating
        )
    }

    @Composable
    override fun PanelContent(userInfo: U) = SmallRatedAccountPanel(userInfo)

    abstract val rankedHandleColorsList: Array<HandleColor>

    protected abstract suspend fun loadRatingHistory(userId: String): List<RatingChange>
    suspend fun getRatingHistory(userId: String): List<RatingChange> =
        loadRatingHistory(userId).sortedBy { it.date }

}


class UnknownHandleColorException(color: HandleColor, manager: RatedAccountManager<*>):
    IllegalArgumentException("Manager ${manager.type.name} does not support color ${color.name}")


interface RatingRevolutionsProvider {
    //list of (last time, bounds)
    val ratingUpperBoundRevolutions: List<Pair<Instant, Array<HandleColorBound>>>
}
