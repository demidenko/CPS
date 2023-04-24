package com.demich.cps.notifications

import android.app.NotificationChannelGroup

abstract class NotificationChannelGroupInfo(id: String, name: String) {
    private val group = NotificationChannelGroup(id, name)

    fun channel(notificationId: Int, id: String, name: String, importance: Importance = Importance.DEFAULT) =
        NotificationChannelSingleId(
            notificationId,
            NotificationChannelInfo("${group.id}_$id", name, importance, group)
        )

    fun channel(notificationIdRange: IntRange, id: String, name: String, importance: Importance = Importance.DEFAULT) =
        NotificationChannelRangeId(
            notificationIdRange,
            NotificationChannelInfo("${group.id}_$id", name, importance, group)
        )
}