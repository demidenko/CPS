package com.demich.cps.notifications

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import androidx.core.app.NotificationManagerCompat

class NotificationChannelInfo(
    val id: String,
    val name: String,
    val importance: Importance,
    val group: NotificationChannelGroup
) {
    internal fun toAndroidChannel(): NotificationChannel =
        NotificationChannel(id, name, importance.toAndroidImportance()).also {
            it.group = group.id
        }
}

fun NotificationManagerCompat.createNotificationChannel(channelInfo: NotificationChannelInfo) {
    createNotificationChannelGroup(channelInfo.group)
    createNotificationChannel(channelInfo.toAndroidChannel())
}