package com.demich.cps.accounts.managers

import androidx.compose.ui.graphics.toArgb
import com.demich.cps.*
import com.demich.cps.utils.*
import com.demich.cps.utils.codeforces.CodeforcesApi
import com.demich.cps.utils.codeforces.CodeforcesRatingChange
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class RatingChange(
    val rating: Int,
    val date: Instant,
    val title: String = "",
    val oldRating: Int? = null,
    val rank: Int? = null,
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
        date = Instant.parse(end_date.split(' ').run { "${get(0)}T${get(1)}Z" })
    )

internal fun DmojRatingChange.toRatingChange() =
    RatingChange(
        rating = rating,
        date = Instant.fromEpochMilliseconds(timestamp.toLong()),
        title = label,
        rank = ranking
    )


fun notifyRatingChange(
    manager: RatedAccountManager<out RatedUserInfo>,
    notificationChannel: NotificationChannelLazy,
    notificationId: Int,
    handle: String,
    ratingChange: RatingChange
) {
    notificationBuildAndNotify(manager.context, notificationChannel, notificationId) {
        val difference = ratingChange.rating - (ratingChange.oldRating ?: 0)
        setSmallIcon(if (difference < 0) R.drawable.ic_rating_down else R.drawable.ic_rating_up)
        setContentTitle("$handle new rating: ${ratingChange.rating}")
        setContentText("${difference.toSignedString()} (rank: ${ratingChange.rank})")
        setSubText("${manager.type.name} rating changes")
        color = manager.originalColor(manager.getHandleColor(ratingChange.rating))
            .toArgb() //TODO not original but cpsColors
        ratingChange.url?.let { attachUrl(it, manager.context) }
        setWhen(ratingChange.date)
    }
}