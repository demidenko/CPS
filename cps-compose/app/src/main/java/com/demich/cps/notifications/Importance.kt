package com.demich.cps.notifications

import android.app.NotificationManager

enum class Importance {
    MIN,
    DEFAULT,
    HIGH;

    internal fun toAndroidImportance(): Int =
        when (this) {
            DEFAULT -> NotificationManager.IMPORTANCE_DEFAULT
            MIN -> NotificationManager.IMPORTANCE_MIN
            HIGH -> NotificationManager.IMPORTANCE_HIGH
        }
}