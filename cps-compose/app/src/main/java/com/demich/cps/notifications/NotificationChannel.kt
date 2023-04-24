package com.demich.cps.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationBuilder(
    val builder: NotificationCompat.Builder,
    val notificationId: Int
) {
    fun build(block: (Notification, Int) -> Unit) {
        block(builder.build(), notificationId)
    }

    fun notifyBy(notificationManager: NotificationManagerCompat) =
        builder.notifyBy(notificationManager, notificationId)
}

class NotificationChannelSingleId(
    val id: Int,
    val channelInfo: NotificationChannelInfo
) {
    fun builder(context: Context, buildBody: NotificationCompat.Builder.() -> Unit): NotificationBuilder {
        NotificationManagerCompat.from(context).createNotificationChannel(channelInfo)
        return NotificationBuilder(NotificationCompat.Builder(context, channelInfo.id).apply(buildBody), id)
    }

    fun notify(context: Context, buildBody: NotificationCompat.Builder.() -> Unit) {
        builder(context, buildBody).notifyBy(NotificationManagerCompat.from(context))
    }
}

class NotificationChannelRangeId(
    val idRange: IntRange,
    val channelInfo: NotificationChannelInfo
) {
    fun notify(context: Context, key: Any, build: NotificationCompat.Builder.() -> Unit) {
        // = NotificationChannelSingleId(...).notify
    }
}
