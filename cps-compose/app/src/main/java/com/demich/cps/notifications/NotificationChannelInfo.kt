package com.demich.cps.notifications

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import androidx.core.app.NotificationManagerCompat

class NotificationChannelInfo internal constructor(
    val id: String,
    val name: String,
    val importance: Importance,
    val group: NotificationChannelGroup
)

internal fun NotificationManagerCompat.createNotificationChannel(channelInfo: NotificationChannelInfo) {
    with(channelInfo) {
        createNotificationChannelGroup(group)
        createNotificationChannel(NotificationChannel(id, name, importance.toAndroidImportance()).also {
            it.group = group.id
        })
    }
}