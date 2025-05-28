package com.demich.cps.notifications

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.demich.cps.utils.makePendingIntentOpenUrl
import kotlinx.datetime.Instant

//default values and getters don't matters, setter just send value to builder
class NotificationBuildScope(
    private val builder: NotificationCompat.Builder,
    private val context: Context
) {

    var contentTitle: String = ""
        set(value) {
            builder.setContentTitle(value)
        }

    var contentText: String = ""
        set(value) {
            builder.setContentText(value)
        }

    var subText: String = ""
        set(value) {
            builder.setSubText(value)
        }

    var smallIcon: Int = 0
        set(value) {
            builder.setSmallIcon(value)
        }

    var color: Color = Color.Black
        set(value) {
            builder.setColor(value.toArgb())
        }

    var colorResId: Int = 0
        set(value) {
            builder.setColor(context.getColor(value))
        }

    var time: Instant? = null
        set(value) {
            builder.setShowWhen(value != null)
            if (value != null) builder.setWhen(value.toEpochMilliseconds())
        }

    var bigContent: CharSequence = ""
        set(value) {
            builder.setContentText(value).setStyle(NotificationCompat.BigTextStyle().bigText(value))
        }

    var url: String = ""
        set(value) {
            builder.setContentIntent(makePendingIntentOpenUrl(url = value, context = context))
        }

    var autoCancel: Boolean = false
        set(value) {
            builder.setAutoCancel(value)
        }

    var silent: Boolean = false
        set(value) {
            builder.setSilent(value)
        }

    fun setStyle(style: NotificationCompat.Style?) {
        builder.setStyle(style)
    }

    fun setCustomContentView(view: RemoteViews?) {
        builder.setCustomContentView(view)
    }

    fun setCustomBigContentView(view: RemoteViews?) {
        builder.setCustomBigContentView(view)
    }

    fun addExtras(extras: android.os.Bundle) {
        builder.addExtras(extras)
    }
}