package com.demich.cps.profiles.managers

import android.content.Context
import com.demich.cps.R
import com.demich.cps.notifications.NotificationChannelSingleId
import com.demich.cps.profiles.RatingChange
import com.demich.cps.profiles.ratingDiff
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.RatedUserInfo
import com.demich.cps.profiles.userinfo.handle
import com.demich.cps.utils.jsonCPS
import com.demich.cps.utils.toSignedString
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.DataStoreWrapper
import kotlinx.serialization.Serializable
import kotlin.time.Instant

abstract class RatedProfileStorage<U: RatedUserInfo>(
    private val manager: RatedProfileManager<U>,
    private val context: Context,
    dataStoreWrapper: DataStoreWrapper
):
    ProfileUniqueStorage<U>(dataStoreWrapper)
{
    private val lastRatingChange: DataStoreItem<ShortRatingChange?> =
        jsonCPS.itemNullable(name = "last_rating_change")

    protected abstract val ratingChangeNotificationChannel: NotificationChannelSingleId
    protected abstract fun U.copyRating(rating: Int): U

    suspend fun applyRatingChange(ratingChange: RatingChange) {
        val prev = lastRatingChange()

        if (prev != null) {
            if (ratingChange.date < prev.date) return
            if (ratingChange.date == prev.date && ratingChange.rating == prev.rating) return
        }

        //save
        lastRatingChange.setValue(ratingChange.short())

        if (prev == null) return //TODO: consider cases

        //update userInfo
        val profile = profile() ?: return

        val newProfile = manager.fetchProfile(str = profile.handle)
        if (newProfile is ProfileResult.Failed) {
            if (profile is ProfileResult.Success) {
                val newUserInfo = profile.userInfo.copyRating(rating = ratingChange.rating)
                setProfile(ProfileResult(newUserInfo), reset = false)
            } else {
                // TODO ??????????
            }
        } else {
            setProfile(newProfile, reset = false)
        }

        notifyRatingChange(
            ratingChange = ratingChange,
            handle = profile.handle
        )
    }

    private fun notifyRatingChange(ratingChange: RatingChange, handle: String) {
        ratingChangeNotificationChannel.notify(context) {
            val difference = ratingChange.ratingDiff()
            smallIcon = if (difference < 0) R.drawable.ic_rating_down else R.drawable.ic_rating_up
            contentTitle = "$handle new rating: ${ratingChange.rating}"
            contentText = "${difference.toSignedString()} (rank: ${ratingChange.rank})"
            subText = "${manager.platform} rating changes"
            color = manager.originalColor(manager.getHandleColor(ratingChange.rating)) //TODO not original but cpsColors
            ratingChange.url?.let { url = it }
            time = ratingChange.date
        }
    }
}

@Serializable
private data class ShortRatingChange(
    val date: Instant,
    val rating: Int
)

private fun RatingChange.short() =
    ShortRatingChange(
        date = date,
        rating = rating
    )