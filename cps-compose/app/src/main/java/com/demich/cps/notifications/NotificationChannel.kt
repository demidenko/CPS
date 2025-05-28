package com.demich.cps.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationChannelSingleId internal constructor(
    val notificationId: Int,
    val channelInfo: NotificationChannelInfo
) {
    fun builder(context: Context, block: NotificationBuildScope.() -> Unit): NotificationBuilder {
        val notificationManager = NotificationManagerCompat.from(context).apply {
            createNotificationChannel(channelInfo)
        }
        return NotificationBuilder(
            builder = NotificationCompat.Builder(context, channelInfo.id),
            notificationId = notificationId,
            notificationManager = notificationManager,
            context = context
        ).apply { edit(block) }
    }

    fun notify(context: Context, block: NotificationBuildScope.() -> Unit) =
        builder(context, block).notify()
}

class NotificationChannelRangeId internal constructor(
    private val idRange: IntRange,
    val channelInfo: NotificationChannelInfo
) {
    operator fun invoke(key: Any): NotificationChannelSingleId =
        NotificationChannelSingleId(
            notificationId = key.hashCode().mod(idRange.last - idRange.first + 1) + idRange.first,
            channelInfo = channelInfo
        )
}
