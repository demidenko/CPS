package com.demich.cps.accounts.managers

import android.content.Context
import com.demich.cps.R
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.notifications.NotificationChannelSingleId
import com.demich.cps.utils.jsonCPS
import com.demich.cps.utils.toSignedString
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.DataStoreWrapper

abstract class RatedAccountDataStore<U: RatedUserInfo>(
    private val manager: RatedAccountManager<U>,
    private val context: Context,
    dataStoreWrapper: DataStoreWrapper
):
    AccountUniqueDataStore<U>(dataStoreWrapper)
{
    private val lastRatingChange: DataStoreItem<RatingChange?> =
        jsonCPS.item(name = "last_rating_change", defaultValue = null)

    protected abstract val ratingChangeNotificationChannel: NotificationChannelSingleId
    protected abstract fun U.withNewRating(rating: Int): U

    suspend fun applyRatingChange(ratingChange: RatingChange) {
        val prev = lastRatingChange()

        if (prev != null) {
            if (ratingChange.date < prev.date) return
            if (ratingChange.date == prev.date && ratingChange.rating == prev.rating) return
        }

        //save
        lastRatingChange(newValue = ratingChange.copy(title = "", url = null))

        if (prev == null) return //TODO: consider cases

        //update userInfo
        val profile = getProfile() ?: return

        val newProfile = manager.fetchProfile(data = profile.userId)
        if (newProfile is ProfileResult.Failed) {
            if (profile is ProfileResult.Success) {
                val newUserInfo = profile.userInfo.withNewRating(rating = ratingChange.rating)
                userInfo(newValue = newUserInfo)
            } else {
                // TODO ??????????
            }
        } else {
            userInfo(newValue = newProfile.convert())
        }

        //notify
        notifyRatingChange(
            channel = ratingChangeNotificationChannel,
            ratingChange = ratingChange,
            handle = profile.userId,
            manager = manager,
            context = context
        )
    }
}

private fun notifyRatingChange(
    channel: NotificationChannelSingleId,
    ratingChange: RatingChange,
    handle: String,
    manager: RatedAccountManager<out RatedUserInfo>,
    context: Context
) {
    channel.notify(context) {
        val difference = ratingChange.rating - (ratingChange.oldRating ?: 0)
        smallIcon = if (difference < 0) R.drawable.ic_rating_down else R.drawable.ic_rating_up
        contentTitle = "$handle new rating: ${ratingChange.rating}"
        contentText = "${difference.toSignedString()} (rank: ${ratingChange.rank})"
        subText = "${manager.type.name} rating changes"
        color = manager.originalColor(manager.getHandleColor(ratingChange.rating)) //TODO not original but cpsColors
        ratingChange.url?.let { url = it }
        time = ratingChange.date
    }
}