package com.demich.cps.notifications

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.demich.cps.utils.makePendingIntentOpenUrl
import com.demich.cps.utils.writeOnlyProperty
import kotlin.time.Instant

class NotificationBuildScope(
    private val builder: NotificationCompat.Builder,
    private val context: Context
) {

    var contentTitle: String by writeOnlyProperty {
        builder.setContentTitle(it)
    }

    var contentText: String by writeOnlyProperty {
        builder.setContentText(it)
    }

    var subText: String by writeOnlyProperty {
        builder.setSubText(it)
    }

    var smallIcon: Int by writeOnlyProperty {
        builder.setSmallIcon(it)
    }

    var color: Color by writeOnlyProperty {
        builder.setColor(it.toArgb())
    }

    var colorResId: Int by writeOnlyProperty {
        builder.setColor(context.getColor(it))
    }

    var time: Instant? by writeOnlyProperty {
        builder.setShowWhen(it != null)
        if (it != null) builder.setWhen(it.toEpochMilliseconds())
    }

    var bigContent: CharSequence by writeOnlyProperty {
        builder.setContentText(it).setStyle(NotificationCompat.BigTextStyle().bigText(it))
    }

    var url: String by writeOnlyProperty {
        builder.setContentIntent(makePendingIntentOpenUrl(url = it, context = context))
    }

    var autoCancel: Boolean by writeOnlyProperty {
        builder.setAutoCancel(it)
    }

    var silent: Boolean by writeOnlyProperty {
        builder.setSilent(it)
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