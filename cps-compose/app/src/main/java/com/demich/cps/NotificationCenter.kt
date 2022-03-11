package com.demich.cps

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


fun notificationBuilder(
    context: Context,
    channel: NotificationChannelLazy,
    buildBody: NotificationCompat.Builder.() -> Unit
): NotificationCompat.Builder {
    return NotificationCompat.Builder(context, channel.getID(context)).apply(buildBody)
}

fun NotificationCompat.Builder.notifyBy(
    m: NotificationManagerCompat,
    notificationId: Int,
) = m.notify(notificationId, build())

fun NotificationCompat.Builder.notifyBy(
    m: NotificationManager,
    notificationId: Int,
) = m.notify(notificationId, build())

fun notificationBuildAndNotify(
    context: Context,
    channel: NotificationChannelLazy,
    notificationId: Int,
    buildBody: NotificationCompat.Builder.() -> Unit
) = notificationBuilder(context, channel, buildBody).notifyBy(NotificationManagerCompat.from(context), notificationId)

fun NotificationCompat.Builder.setBigContent(str: CharSequence) = setContentText(str).setStyle(NotificationCompat.BigTextStyle().bigText(str))


object NotificationChannels {

    private val codeforces by lazyNotificationChannelGroup("codeforces", "CodeForces")
    val codeforces_contest_watcher by codeforces.lazyChannel("cf_contest_watcher", "Contest watch")
    val codeforces_rating_changes by codeforces.lazyChannel("cf_rating_changes", "Rating changes", Importance.HIGH)
    val codeforces_contribution_changes by codeforces.lazyChannel("cf_contribution_changes", "Contribution changes", Importance.MIN)
    val codeforces_follow_new_blog by codeforces.lazyChannel("cf_follow_new_blog", "Follow: new blog entries")
    val codeforces_follow_progress by codeforces.lazyChannel("cf_follow_progress", "Follow: update progress", Importance.MIN)
    val codeforces_upsolving_suggestion by codeforces.lazyChannel("cf_upsolving_suggestion", "Upsolving suggestions")

    private val atcoder by lazyNotificationChannelGroup("atcoder", "AtCoder")
    val atcoder_rating_changes by atcoder.lazyChannel("atcoder_rating_changes", "Rating changes", Importance.HIGH)

    private val project_euler by lazyNotificationChannelGroup("project_euler", "Project Euler")
    val project_euler_news by project_euler.lazyChannel("pe_news", "Recent Problems")
    val project_euler_problems by project_euler.lazyChannel("pe_problems", "News")

    private val acmp by lazyNotificationChannelGroup("acmp", "ACMP")
    val acmp_news by acmp.lazyChannel("acmp_news", "News")

    private val zaoch by lazyNotificationChannelGroup("zaoch", "olympiads.ru/zaoch")
    val olympiads_zaoch_news by zaoch.lazyChannel("olympiads_zaoch_news", "News")

    private val testGroup by lazyNotificationChannelGroup("test", "Test group")
    val test by testGroup.lazyChannel("test", "test channel")


    enum class Importance {
        MIN,
        DEFAULT,
        HIGH;

        @RequiresApi(Build.VERSION_CODES.N)
        fun convert(): Int =
            when(this) {
                DEFAULT -> NotificationManager.IMPORTANCE_DEFAULT
                MIN -> NotificationManager.IMPORTANCE_MIN
                HIGH -> NotificationManager.IMPORTANCE_HIGH
            }
    }

    private fun lazyNotificationChannelGroup(id: String, name: String) = lazy { NotificationChannelGroupLazy(id, name) }
    private fun NotificationChannelGroupLazy.lazyChannel(id: String, name: String, importance: Importance = Importance.DEFAULT) =
        lazy { NotificationChannelLazy(id, name, importance, this) }
}

class NotificationChannelGroupLazy(private val id: String, val name: String) {
    private var created = false
    @RequiresApi(Build.VERSION_CODES.O)
    fun getID(m: NotificationManagerCompat): String {
        if(!created) {
            m.createNotificationChannelGroup(NotificationChannelGroup(id, name))
            created = true
        }
        return id
    }
}

class NotificationChannelLazy(
    private val id: String,
    val name: String,
    private val importance: NotificationChannels.Importance,
    private val groupCreator: NotificationChannelGroupLazy
) {
    private var created = false
    fun getID(context: Context): String {
        if(!created) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val m = NotificationManagerCompat.from(context)
                val channel = NotificationChannel(id, name, importance.convert()).apply {
                    group = groupCreator.getID(m)
                }
                m.createNotificationChannel(channel)
            }
            created = true
        }
        return id
    }
}

object NotificationIDs {
    private object nextId {
        private var id = 0
        operator fun invoke() = ++id
    }

    private object nextIdInterval {
        private var start = 1_000_000
        private val step = 1_000_000
        operator fun invoke() = IntervalID(start, step).also { start += step }
    }

    data class IntervalID(val from: Int, val length: Int) {
        init {
            require(length > 0) { "illegal interval length: $length" }
        }
        operator fun invoke(int: Int) = from + int.mod(length)
        operator fun invoke(long: Long) = from + long.mod(length)
        operator fun invoke(str: String) = invoke(str.hashCode())
    }

    //codeforces
    val codeforces_contest_watcher = nextId()
    val codeforces_rating_changes = nextId()
    val codeforces_contribution_changes = nextId()
    val makeCodeforcesSystestSubmissionID = nextIdInterval()
    val makeCodeforcesFollowBlogID = nextIdInterval()
    val codeforces_follow_progress = nextId()
    val makeCodeforcesUpsolveProblemID = nextIdInterval()

    //atcoder
    val atcoder_rating_changes = nextId()

    //project euler
    val makeProjectEulerRecentProblemNotificationID = nextIdInterval()
    val makeProjectEulerNewsNotificationID = nextIdInterval()

    //acmp
    val makeACMPNewsNotificationID = nextIdInterval()

    //zaoch
    val makeZaochNewsNotificationID = nextIdInterval()

    //test
    val testID = nextId()
}


fun makeIntentOpenUrl(url: String) = Intent(Intent.ACTION_VIEW, Uri.parse(url))

fun makePendingIntentOpenURL(url: String, context: Context): PendingIntent {
    return PendingIntent.getActivity(context, 0, Intent(Intent.ACTION_VIEW, Uri.parse(url)), PendingIntent.FLAG_IMMUTABLE)
}