package com.demich.cps.accounts.managers

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.demich.cps.accounts.SmallRatedAccountPanel
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.ui.theme.CPSColors
import com.demich.cps.ui.theme.cpsColors
import kotlinx.datetime.Instant

abstract class RatedAccountManager<U: RatedUserInfo>(type: AccountManagers):
    AccountManager<U>(type)
{
    override val userIdTitle get() = "handle"

    abstract val ratingsUpperBounds: Array<Pair<HandleColor, Int>>
    fun getHandleColor(rating: Int): HandleColor =
        ratingsUpperBounds
            .firstOrNull { rating < it.second }?.first
            ?: HandleColor.RED

    abstract fun originalColor(handleColor: HandleColor): Color

    fun colorFor(handleColor: HandleColor, cpsColors: CPSColors): Color =
        if (cpsColors.useOriginalHandleColors) originalColor(handleColor)
        else cpsColors.handleColor(handleColor)

    @Composable
    fun colorFor(handleColor: HandleColor): Color =
        colorFor(handleColor = handleColor, cpsColors = cpsColors)

    @Composable
    fun colorFor(rating: Int): Color = colorFor(handleColor = getHandleColor(rating))

    @Composable
    fun makeHandleSpan(userInfo: U): AnnotatedString = with(userInfo) {
        if (status == STATUS.OK) makeOKSpan(text = handle, rating = rating)
        else AnnotatedString(text = handle)
    }

    @Composable
    open fun makeRatedSpan(text: String, rating: Int) = AnnotatedString(
        text = text,
        spanStyle = SpanStyle(color = colorFor(rating = rating), fontWeight = FontWeight.Bold)
    )

    @Composable
    fun makeOKSpan(text: String, rating: Int?): AnnotatedString {
        return if (rating == null) AnnotatedString(text = text)
        else makeRatedSpan(text, rating)
    }

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

enum class HandleColor {
    GRAY,
    BROWN,
    GREEN,
    CYAN,
    BLUE,
    VIOLET,
    YELLOW,
    ORANGE,
    RED;

    companion object {
        //TODO atcoder::orange == cf::red
        val rankedCodeforces    = arrayOf(GRAY, GRAY, GREEN, CYAN, BLUE, VIOLET, VIOLET, ORANGE, ORANGE, RED)
        val rankedAtCoder       = arrayOf(GRAY, BROWN, GREEN, CYAN, BLUE, YELLOW, YELLOW, ORANGE, ORANGE, RED)
        val rankedTopCoder      = arrayOf(GRAY, GRAY, GREEN, GREEN, BLUE, YELLOW, YELLOW, YELLOW, YELLOW, RED)
        //TODO:
        val rankedCodeChef      = arrayOf(GRAY, GRAY, GREEN, GREEN, BLUE, VIOLET, YELLOW, YELLOW, ORANGE, RED)
        val rankedDmoj          = arrayOf(GRAY, GRAY, GREEN, GREEN, BLUE, VIOLET, VIOLET, YELLOW, YELLOW, RED)
    }

    class UnknownHandleColorException(color: HandleColor, manager: RatedAccountManager<*>):
        Throwable("Manager ${manager.type.name} does not support color ${color.name}")
}

interface RatingRevolutionsProvider {
    //list of (last time, bounds)
    val ratingUpperBoundRevolutions: List<Pair<Instant, Array<Pair<HandleColor, Int>>>>
}
