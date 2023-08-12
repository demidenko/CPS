package com.demich.cps.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationBuilder(
    private val builder: NotificationCompat.Builder,
    val notificationId: Int,
    private val notificationManager: NotificationManagerCompat
) {
    fun edit(buildBody: NotificationCompat.Builder.() -> Unit) = builder.buildBody()

    fun build(): Notification = builder.build()

    @JvmName("notifyCustom")
    //TODO: android 14 permissions
    fun notify() = notificationManager.notify(notificationId, build())
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
    private val idRange: IntRange,
    val channelInfo: NotificationChannelInfo
) {
    operator fun invoke(key: Any): NotificationChannelSingleId =
        NotificationChannelSingleId(
            notificationId = key.hashCode().mod(idRange.last - idRange.first + 1) + idRange.first,
            channelInfo = channelInfo
        )
}
