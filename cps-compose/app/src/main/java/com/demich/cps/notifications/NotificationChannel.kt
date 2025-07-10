package com.demich.cps.notifications

import android.app.NotificationChannelGroup
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationChannelInfo internal constructor(
    val id: String,
    val name: String,
    val importance: Importance,
    val group: NotificationChannelGroup
)

class NotificationChannelSingleId internal constructor(
    val notificationId: Int,
    val channelInfo: NotificationChannelInfo
) {
    fun builder(context: Context): NotificationBuilder {
        val notificationManager = NotificationManagerCompat.from(context).apply {
            createNotificationChannel(channelInfo)
        }
        return NotificationBuilder(
            builder = NotificationCompat.Builder(context, channelInfo.id),
            notificationId = notificationId,
            notificationManager = notificationManager,
            context = context
        )
    }

    inline fun builder(context: Context, block: NotificationBuildScope.() -> Unit) =
        builder(context).apply { edit(block) }

    inline fun notify(context: Context, block: NotificationBuildScope.() -> Unit) =
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

private fun NotificationManagerCompat.createNotificationChannel(channelInfo: NotificationChannelInfo) {
    with(channelInfo) {
        createNotificationChannelGroup(group)
        val androidChannel = android.app.NotificationChannel(id, name, importance.toAndroidImportance()).also {
            it.group = group.id
        }
        createNotificationChannel(androidChannel)
    }
}