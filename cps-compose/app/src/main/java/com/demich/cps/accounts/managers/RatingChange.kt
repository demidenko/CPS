package com.demich.cps.accounts.managers

import com.demich.cps.platforms.api.AtCoderApi
import com.demich.cps.platforms.api.AtCoderRatingChange
import com.demich.cps.platforms.api.CodeChefRatingChange
import com.demich.cps.platforms.api.DmojRatingChange
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRatingChange
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable

@Serializable
data class RatingChange(
    val rating: Int,
    val date: Instant,
    val title: String = "",
    val oldRating: Int? = null,
    val rank: Int,
    val url: String? = null
)

internal fun CodeforcesRatingChange.toRatingChange() =
    RatingChange(
        rating = newRating,
        oldRating = oldRating,
        date = ratingUpdateTime,
        title = contestName,
        rank = rank,
        url = CodeforcesApi.urls.contestsWith(handle)
    )

internal fun AtCoderRatingChange.toRatingChange(handle: String) =
    RatingChange(
        rating = NewRating,
        oldRating = OldRating,
        rank = Place,
        date = EndTime,
        title = ContestName,
        url = AtCoderApi.urls.userContestResult(handle, getContestId())
    )

internal fun CodeChefRatingChange.toRatingChange() =
    RatingChange(
        rating = rating.toInt(),
        rank = rank.toInt(),
        title = name,
        date = LocalDateTime.Format {
            date(LocalDate.Formats.ISO)
            char(' ')
            time(LocalTime.Formats.ISO)
        }.parse(end_date).toInstant(TimeZone.of("IST"))
    )

internal fun DmojRatingChange.toRatingChange() =
    RatingChange(
        rating = rating,
        date = Instant.fromEpochMilliseconds(timestamp.toLong()),
        title = label,
        rank = ranking
    )
