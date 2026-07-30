package com.demich.cps.profiles

import com.demich.cps.platforms.api.atcoder.AtCoderRatingChange
import com.demich.cps.platforms.api.codechef.CodeChefRatingChange
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRatingChange
import com.demich.cps.platforms.api.dmoj.DmojRatingChange
import com.demich.cps.platforms.utils.atcoder.url
import com.demich.cps.platforms.utils.date
import kotlin.time.Instant

data class RatingChange(
    val rating: Int,
    val contestTitle: String,
    val date: Instant,
    val rank: Int,
    val oldRating: Int? = null,
    val url: String? = null
)

fun RatingChange.ratingDiff(): Int =
    oldRating?.let { rating - it } ?: rating

internal fun CodeforcesRatingChange.toRatingChange() =
    RatingChange(
        rating = newRating,
        oldRating = oldRating,
        date = ratingUpdateTime,
        contestTitle = contestName,
        rank = rank,
        url = CodeforcesUrls.contestsWith(handle)
    )

internal fun AtCoderRatingChange.toRatingChange(handle: String) =
    RatingChange(
        rating = NewRating,
        oldRating = OldRating,
        rank = Place,
        date = EndTime,
        contestTitle = ContestName,
        url = url(handle)
    )

internal fun CodeChefRatingChange.toRatingChange() =
    RatingChange(
        rating = rating.toInt(),
        rank = rank.toInt(),
        contestTitle = name,
        date = date()
    )

internal fun DmojRatingChange.toRatingChange() =
    RatingChange(
        rating = rating,
        date = Instant.fromEpochMilliseconds(timestamp.toLong()),
        contestTitle = label,
        rank = ranking
    )
