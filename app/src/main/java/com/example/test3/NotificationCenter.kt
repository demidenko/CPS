package com.example.test3

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationChannels {
    //codeforces
    const val codeforces_contest_watcher = "cf_contest_watcher"
    const val codeforces_rating_changes = "cf_rating_changes"
    const val codeforces_contribution_changes = "cf_contribution_changes"

    //project euler
    const val project_euler_news = "pe_news"
    const val project_euler_problems = "pe_problems"

    //acmp
    const val acmp_news = "acmp_news"

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
            group = group_id_test
        })
        m.createNotificationChannel(NotificationChannel(
            "test2",
            "test2 channel",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            group = group_id_test
        })
        m.createNotificationChannel(NotificationChannel(
            "test3",
            "test3 channel",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            group = group_id_test
        })
        m.createNotificationChannel(NotificationChannel(
            "test4",
            "test4 channel",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            group = group_id_test
        })
        m.createNotificationChannel(NotificationChannel(
            "test5",
            "test5 channel",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            group = group_id_test
        })

        //codeforces channels
        val group_id_codeforces = "codeforces"
        m.createNotificationChannelGroup(
            NotificationChannelGroup(
                group_id_codeforces,
                "CodeForces"
            ))
        m.createNotificationChannel(NotificationChannel(
            codeforces_rating_changes,
            "Rating changes",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            group = group_id_codeforces
        })
        m.createNotificationChannel(NotificationChannel(
            codeforces_contribution_changes,
            "Contribution changes",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            group = group_id_codeforces
        })
        m.createNotificationChannel(NotificationChannel(
            codeforces_contest_watcher,
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
            project_euler_problems,
            "Recent Problems",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            group = group_id_project_euler
        })
        m.createNotificationChannel(NotificationChannel(
            project_euler_news,
            "News",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            group = group_id_project_euler
        })

        //acmp channels
        val group_id_acmp = "acmp"
        m.createNotificationChannelGroup(
            NotificationChannelGroup(
                group_id_acmp,
                "ACMP"
            )
        )
        m.createNotificationChannel(NotificationChannel(
            acmp_news,
            "News",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            group = group_id_acmp
        })
    }
}

object NotificationIDs {
    private var id = 0

    //codeforces
    val codeforces_contest_watcher = ++id
    val codeforces_contribution_changes = ++id

    //project euler
    fun makeProjectEulerRecentProblemNotificationID(problemID: Int): Int = 1_000_000 + problemID
    fun makeProjectEulerNewsNotificationID(title: String): Int {
        var res = title.hashCode() % 900_000
        if(res<0) res += 900_000
        return 1_100_000 + res
    }

    //acmp
    fun makeACMPNewsNotificationID(newsID: Int): Int = 2_000_000 + newsID

    //test
    val test = ++id
}

object NotificationColors {
    //project euler
    val project_euler_main = Color.parseColor("#6B4E3D")

    //acmp
    val acmp_main = Color.parseColor("#006600")
}

fun makeSimpleNotification(context: Context, id: Int, title: String, content: String, silent: Boolean = true){
    val n = NotificationCompat.Builder(context, "test").apply {
        setSmallIcon(R.drawable.ic_news)
        setShowWhen(true)
        setContentTitle(title)
        setContentText(content)
        if(silent) setNotificationSilent()
    }
    NotificationManagerCompat.from(context).notify(id, n.build())
}

fun makePendingIntentOpenURL(url: String, context: Context): PendingIntent {
    return PendingIntent.getActivity(context, 0, Intent(Intent.ACTION_VIEW, Uri.parse(url)), 0)
}