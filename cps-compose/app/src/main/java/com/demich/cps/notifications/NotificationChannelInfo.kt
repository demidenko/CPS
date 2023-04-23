package com.demich.cps.notifications

import android.app.NotificationChannel

class NotificationChannelInfo(
    val id: String,
    val name: String,
    val importance: Importance
) {
    fun toAndroidChannel(): NotificationChannel =
        NotificationChannel(id, name, importance.toAndroidImportance())
}