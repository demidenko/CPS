package com.demich.cps.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.demich.cps.utils.makePendingIntentOpenUrl
import kotlinx.datetime.Instant


fun NotificationCompat.Builder.setBigContent(str: CharSequence) =
    setContentText(str).setStyle(NotificationCompat.BigTextStyle().bigText(str))

fun NotificationCompat.Builder.setWhen(time: Instant) {
    setShowWhen(true)
    setWhen(time.toEpochMilliseconds())
}

fun NotificationCompat.Builder.setProgress(total: Int, current: Int) =
    setProgress(total, current, false)


fun NotificationCompat.Builder.attachUrl(url: String, context: Context) {
    setContentIntent(makePendingIntentOpenUrl(url, context))
}