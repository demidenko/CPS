package com.demich.cps.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationBuilder(
    val builder: NotificationCompat.Builder,
    val notificationId: Int,
    private val notificationManager: NotificationManagerCompat
) {
    inline fun build(block: (Int, Notification) -> Unit) {
        block(notificationId, builder.build())
    }

    @JvmName("notifyCustom")
    fun notify() = build(notificationManager::notify)
}

class NotificationChannelSingleId(
    val notificationId: Int,
    val channelInfo: NotificationChannelInfo
) {
    fun builder(context: Context, buildBody: NotificationCompat.Builder.() -> Unit): NotificationBuilder {
        val notificationManager = NotificationManagerCompat.from(context).apply {
            createNotificationChannel(channelInfo)
        }
        return NotificationBuilder(
            builder = NotificationCompat.Builder(context, channelInfo.id).apply(buildBody),
            notificationId = notificationId,
            notificationManager = notificationManager
        )
    }

    fun notify(context: Context, buildBody: NotificationCompat.Builder.() -> Unit) =
        builder(context, buildBody).notify()
}

class NotificationChannelRangeId(
    val idRange: IntRange,
    val channelInfo: NotificationChannelInfo
) {
    operator fun invoke(key: Any): NotificationChannelSingleId =
        NotificationChannelSingleId(
            notificationId = key.hashCode().mod(idRange.last - idRange.first + 1) + idRange.first,
            channelInfo = channelInfo
        )
}
