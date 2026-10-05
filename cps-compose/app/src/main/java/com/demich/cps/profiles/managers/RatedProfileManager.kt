package com.demich.cps.profiles.managers

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.demich.cps.profiles.RatingBounds
import com.demich.cps.profiles.RatingChange
import com.demich.cps.profiles.RatingColor
import com.demich.cps.profiles.RatingColorBound
import com.demich.cps.profiles.SmallRatedProfilePanel
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.RatedUserInfo
import com.demich.cps.profiles.userinfo.ratingToString
import com.demich.cps.ui.theme.CPSColors
import kotlin.time.Instant

abstract class RatedProfileManager<U: RatedUserInfo>: ProfileManager<U>() {
    override val userIdTitle get() = "handle"

    abstract val ratingsUpperBounds: RatingBounds

    abstract fun platformColor(ratingColor: RatingColor): Color?

    open fun makeRatedSpan(text: String, rating: Int, cpsColors: CPSColors): AnnotatedString =
        AnnotatedString(
            text = text,
            spanStyle = SpanStyle(
                color = cpsColors.colorFor(rating = rating),
                fontWeight = FontWeight.Bold
            )
        )

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
        SmallRatedProfilePanel(profileResult)

    abstract val rankedRatingColors: Array<RatingColor>

    protected abstract suspend fun getRatingChanges(userId: String): List<RatingChange>
    suspend fun getRatingChangeHistory(userId: String): List<RatingChange> =
        getRatingChanges(userId).sortedBy { it.date }

}

interface RatingRevolutionsProvider {
    //list of (last time, bounds)
    fun ratingUpperBoundRevolutions(): List<Pair<Instant, List<RatingColorBound>>>
}

infix fun List<RatingColorBound>.until(endTime: Instant) =
    Pair(endTime, this)

fun RatedProfileManager<*>.platformColorOrThrow(ratingColor: RatingColor): Color =
    platformColor(ratingColor = ratingColor) ?: throw IllegalArgumentException("platform $platform does not support rating color $ratingColor")

fun RatedProfileManager<*>.availableRatingColors(): List<RatingColor> =
    RatingColor.entries.filter { platformColor(it) != null }
