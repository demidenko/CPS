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
    fun edit(buildBody: NotificationCompat.Builder.() -> Unit) = builder.buildBody()

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

class NotificationChannelSingleId internal constructor(
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
            notificationManager = notificationManager,
            context = context
        )
    }

    fun notify(context: Context, buildBody: NotificationCompat.Builder.() -> Unit) =
        builder(context, buildBody).notify()
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
