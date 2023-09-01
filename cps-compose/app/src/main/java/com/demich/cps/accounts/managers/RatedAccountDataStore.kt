package com.demich.cps.accounts.managers

import android.content.Context
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.notifications.NotificationChannelSingleId
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.DataStoreWrapper

abstract class RatedAccountDataStore<U: RatedUserInfo>(
    private val context: Context,
    dataStoreWrapper: DataStoreWrapper
):
    AccountUniqueDataStore<U>(dataStoreWrapper)
{
    protected val lastRatingChange: DataStoreItem<RatingChange?> =
        jsonCPS.item(name = "last_rating_change", defaultValue = null)

    protected abstract val ratingChangeNotificationChannel: NotificationChannelSingleId
    protected abstract fun U.withNewRating(rating: Int): U

    suspend fun applyRatingChange(ratingChange: RatingChange, manager: RatedAccountManager<U>) {
        val info = userInfo() ?: return
        val prev = lastRatingChange()

        if (prev != null) {
            if (ratingChange.date < prev.date) return
            if (ratingChange.date == prev.date && ratingChange.rating == prev.rating) return
        }

        //save
        lastRatingChange(newValue = ratingChange.copy(title = "", url = null))

        if (prev == null) return //TODO: consider cases

        //update userInfo

        val newUserInfo = manager.getUserInfo(data = info.handle)
        if (newUserInfo.status != STATUS.FAILED) {
            userInfo(newValue = newUserInfo)
        } else {
            userInfo(newValue = info.withNewRating(rating = ratingChange.rating))
        }

        //notify
        notifyRatingChange(
            channel = ratingChangeNotificationChannel,
            ratingChange = ratingChange,
            handle = info.handle,
            manager = manager,
            context = context
        )
    }
}