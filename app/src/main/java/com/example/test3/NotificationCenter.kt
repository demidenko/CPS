package com.example.test3

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import androidx.appcompat.app.AppCompatActivity

class NotificationChannels {
    companion object{
        //codeforces
        const val codeforces_contest_watcher = "cf_contest_watcher"
        const val codeforces_rating_changes = "cf_rating_changes"
        //project euler
        const val project_euler_news = "pe_news"
        const val project_euler_problems = "pe_problems"
    }
}

fun createNotificationChannels(activity: MainActivity){
    val m = (activity.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager)

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
        "New Problems",
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