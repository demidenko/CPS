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
    private val notificationManager: NotificationManagerCompat,
    private val context: Context
) {
    val builderScope = NotificationBuildScope(
        builder = builder,
        context = context
    )

    inline fun edit(block: NotificationBuildScope.() -> Unit) = builderScope.block()

    fun build(): Notification = builder.build()

    @JvmName("notifyCustom")
    fun notify() {
        if (!notificationManager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                return
        }
        notificationManager.notify(notificationId, build())
    }
}