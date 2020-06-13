package com.example.test3

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat

class NotificationChannels {
    companion object {
        //codeforces
        const val codeforces_contest_watcher = "cf_contest_watcher"
        const val codeforces_rating_changes = "cf_rating_changes"
        //project euler
        const val project_euler_news = "pe_news"
        const val project_euler_problems = "pe_problems"
    }
}

class NotificationIDs {
    companion object {
        private var id = 0
        //codeforces
        val codeforces_contest_watcher = ++id
        //test
        val test = ++id
    }
}

fun createNotificationChannels(context: Context){
    val m = (context.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager)

    //test
    val group_id_test = "test"
    m.createNotificationChannelGroup(
        NotificationChannelGroup(
            group_id_test,
            "Test Group"
        )
    )
    m.createNotificationChannel(NotificationChannel(
        "test",
        "test channel",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {

    })
    m.createNotificationChannel(NotificationChannel(
        "test2",
        "test2 channel",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {

    })

    //codeforces channels
    val group_id_codeforces = "codeforces"
    m.createNotificationChannelGroup(
        NotificationChannelGroup(
            group_id_codeforces,
            "CodeForces"
    ))
    m.createNotificationChannel(NotificationChannel(
        NotificationChannels.codeforces_rating_changes,
        "Rating changes",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        group = group_id_codeforces
    })
    m.createNotificationChannel(NotificationChannel(
        NotificationChannels.codeforces_contest_watcher,
        "Contest watch",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        group = group_id_codeforces
    })

    //project euler channels
    val group_id_project_euler = "project_euler"
    m.createNotificationChannelGroup(
        NotificationChannelGroup(
            group_id_project_euler,
            "Project Euler"
    ))
    m.createNotificationChannel(NotificationChannel(
        NotificationChannels.project_euler_problems,
        "Recent Problems",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        group = group_id_project_euler
    })
    m.createNotificationChannel(NotificationChannel(
        NotificationChannels.project_euler_news,
        "News",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        group = group_id_project_euler
    })
}

fun makeSimpleNotification(context: Context, id: Int, title: String, content: String, silent: Boolean = true){
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val n = NotificationCompat.Builder(context, "test").apply {
        setSmallIcon(R.drawable.ic_news)
        setShowWhen(true)
        setContentTitle(title)
        setContentText(content)
        if(silent) setNotificationSilent()
    }
    notificationManager.notify(id, n.build())
}