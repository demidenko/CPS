package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.demich.cps.*
import com.demich.cps.accounts.SmallRatedAccountPanel
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.ui.useOriginalColors
import com.demich.cps.utils.signedToString
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

abstract class RatedAccountManager<U: RatedUserInfo>(context: Context, type: AccountManagers):
    AccountManager<U>(context, type)
{
    override val userIdTitle get() = "handle"

    abstract val ratingsUpperBounds: Array<Pair<HandleColor, Int>>
    fun getHandleColor(rating: Int): HandleColor =
        ratingsUpperBounds
            .firstOrNull { rating < it.second }?.first
            ?: HandleColor.RED

    abstract fun originalColor(handleColor: HandleColor): Color

    @Composable
    fun colorFor(handleColor: HandleColor): Color =
        if (useOriginalColors) originalColor(handleColor)
        else cpsColors.handleColor(handleColor)

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
    fun makeOKSpan(text: String, rating: Int): AnnotatedString {
        return if (rating == NOT_RATED) AnnotatedString(text = text)
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
    override fun Panel(userInfo: U) = SmallRatedAccountPanel(userInfo)

    abstract val rankedHandleColorsList: Array<HandleColor>

    protected open suspend fun loadRatingHistory(info: U): List<RatingChange>? = null
    suspend fun getRatingHistory(info: U): List<RatingChange>? {
        return kotlin.runCatching {
            loadRatingHistory(info)?.sortedBy { it.date }
        }.getOrNull()
    }
}

const val NOT_RATED = Int.MIN_VALUE

abstract class RatedUserInfo: UserInfo() {
    abstract val handle: String
    abstract val rating: Int

    final override val userId: String
        get() = handle

    fun isRated() = status == STATUS.OK && rating != NOT_RATED

    fun ratingToString() = if (rating == NOT_RATED) "[not rated]" else rating.toString()
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


@Serializable
data class RatingChange(
    val rating: Int,
    val date: Instant,
    val title: String = "",
    val oldRating: Int? = null,
    val rank: Int? = null
)

fun notifyRatingChange(
    manager: RatedAccountManager<out RatedUserInfo>,
    notificationChannel: NotificationChannelLazy,
    notificationId: Int,
    handle: String, newRating: Int, oldRating: Int, rank: Int, url: String? = null, time: Instant? = null
) {
    notificationBuildAndNotify(manager.context, notificationChannel, notificationId) {
        val decreased = newRating < oldRating
        setSmallIcon(if (decreased) R.drawable.ic_rating_down else R.drawable.ic_rating_up)
        setContentTitle("$handle new rating: $newRating")
        val difference = signedToString(newRating - oldRating)
        setContentText("$difference (rank: $rank)")
        setSubText("${manager.type.name} rating changes")
        color = manager.originalColor(manager.getHandleColor(newRating))
            .toArgb() //TODO not original but cpsColors
        if (url != null) attachUrl(url, manager.context)
        if (time != null) setWhen(time)
    }
}