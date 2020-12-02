package com.example.test3

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationChannels {
    //codeforces
    const val codeforces_contest_watcher = "cf_contest_watcher"
    const val codeforces_rating_changes = "cf_rating_changes"
    const val codeforces_contribution_changes = "cf_contribution_changes"
    const val codeforces_follow_new_blog = "cf_follow_new_blog"

    //project euler
    const val project_euler_news = "pe_news"
    const val project_euler_problems = "pe_problems"

    //acmp
    const val acmp_news = "acmp_news"

    //zaoch
    const val olympiads_zaoch_news = "olympiads_zaoch_news"

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannels(context: Context){
        val m = NotificationManagerCompat.from(context)

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
        m.createNotificationChannel(NotificationChannel(
            codeforces_follow_new_blog,
            "Followed blogs",
            NotificationManager.IMPORTANCE_DEFAULT
        ))

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

        //zaoch channels
        val group_id_zaoch = "zaoch"
        m.createNotificationChannelGroup(
            NotificationChannelGroup(
                group_id_zaoch,
                "olympiads.ru/zaoch"
            )
        )
        m.createNotificationChannel(
            NotificationChannel(
            olympiads_zaoch_news,
            "News",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            group = group_id_zaoch
        })
    }
}

object NotificationIDs {
    object nextID {
        private var id = 0
        operator fun invoke() = ++id
    }

    object nextIntervalID {
        private var start = 1_000_000
        private val step = 1_000_000
        operator fun invoke() = IntervalID(start, step).also { start += step }
    }

    data class IntervalID(val from: Int, val length: Int){
        init {
            if(length < 1) throw IllegalArgumentException("illegal interval length: $length")
        }
        operator fun invoke(int: Int) = (int % length + length) % length + from
        operator fun invoke(long: Long) = ((long % length).toInt() + length) % length + from
        operator fun invoke(str: String) = invoke(str.hashCode())
    }

    //codeforces
    val codeforces_contest_watcher = nextID()
    val codeforces_rating_changes = nextID()
    val codeforces_contribution_changes = nextID()
    val makeCodeforcesSystestSubmissionID = nextIntervalID()
    val makeCodeforcesFollowBlogID = nextIntervalID()

    //project euler
    val makeProjectEulerRecentProblemNotificationID = nextIntervalID()
    val makeProjectEulerNewsNotificationID = nextIntervalID()

    //acmp
    val makeACMPNewsNotificationID = nextIntervalID()

    //zaoch
    val makeZaochNewsNotificationID = nextIntervalID()

    //test
    val test = nextID()
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

fun makeIntentOpenUrl(url: String) = Intent(Intent.ACTION_VIEW, Uri.parse(url))

fun makePendingIntentOpenURL(url: String, context: Context): PendingIntent {
    return PendingIntent.getActivity(context, 0, Intent(Intent.ACTION_VIEW, Uri.parse(url)), 0)
}