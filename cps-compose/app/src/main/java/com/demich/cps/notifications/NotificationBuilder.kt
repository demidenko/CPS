package com.demich.cps.notifications

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationBuilder internal constructor(
    private val builder: NotificationCompat.Builder,
    val notificationId: Int,
    private val notificationManager: NotificationManagerCompat
) {
    val builderScope = NotificationBuildScope(
        builder = builder
    )

    inline fun edit(block: NotificationBuildScope.() -> Unit) = builderScope.block()

    fun build(context: Context): Notification {
        builderScope.finalize(context)
        return builder.build()
    }

    fun buildAndPush(context: Context) {
        if (!notificationManager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                return
        }
        notificationManager.notify(notificationId, build(context = context))
    }
}