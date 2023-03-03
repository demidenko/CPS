package com.demich.cps.contests.monitors

import android.content.Context
import androidx.core.app.NotificationCompat

class CodeforcesMonitorNotifier(
    val context: Context,
    val notificationBuilder: NotificationCompat.Builder,
    val handle: String
) {
    fun apply(contestData: CodeforcesMonitorData) {

    }
}