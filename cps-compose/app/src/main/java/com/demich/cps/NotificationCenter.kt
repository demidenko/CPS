package com.demich.cps

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.datetime.Instant


fun notificationBuilder(
    context: Context,
    channel: NotificationChannelLazy,
    buildBody: NotificationCompat.Builder.() -> Unit
): NotificationCompat.Builder {
    return NotificationCompat.Builder(context, channel.getId(context)).apply(buildBody)
}

fun NotificationCompat.Builder.notifyBy(
    notificationManager: NotificationManagerCompat,
    notificationId: Int,
) = notificationManager.notify(notificationId, build())

fun NotificationCompat.Builder.notifyBy(
    notificationManager: NotificationManager,
    notificationId: Int,
) = notificationManager.notify(notificationId, build())

fun notificationBuildAndNotify(
    context: Context,
    channel: NotificationChannelLazy,
    notificationId: Int,
    buildBody: NotificationCompat.Builder.() -> Unit
) = notificationBuilder(context, channel, buildBody).notifyBy(NotificationManagerCompat.from(context), notificationId)

fun NotificationCompat.Builder.setBigContent(str: CharSequence) = setContentText(str).setStyle(NotificationCompat.BigTextStyle().bigText(str))

fun NotificationCompat.Builder.setWhen(time: Instant) {
    setShowWhen(true)
    setWhen(time.toEpochMilliseconds())
}

fun NotificationCompat.Builder.setProgress(total: Int, current: Int) =
    setProgress(total, current, false)

fun NotificationCompat.Builder.attachUrl(url: String, context: Context) {
    setContentIntent(
        PendingIntent.getActivity(
            context,
            0,
            Intent(Intent.ACTION_VIEW, Uri.parse(url)),
            PendingIntent.FLAG_IMMUTABLE
        )
    )
}


object NotificationChannels {

    object codeforces: NotificationChannelGroupLazy("codeforces", "CodeForces") {
        val rating_changes = channel("cf_rating_changes", "Rating changes", Importance.HIGH)
        val contribution_changes = channel("cf_contribution_changes", "Contribution changes", Importance.MIN)
        val contest_monitor = channel("cf_contest_monitor", "Contest monitor")
        val follow_new_blog = channel("cf_follow_new_blog", "Follow: new blog entries")
        val follow_progress = channel("cf_follow_progress", "Follow: update progress", Importance.MIN)
        val upsolving_suggestion = channel("cf_upsolving_suggestion", "Upsolving suggestions")
    }

    object atcoder: NotificationChannelGroupLazy("atcoder", "AtCoder") {
        val rating_changes = channel("atcoder_rating_changes", "Rating changes", Importance.HIGH)
    }

    object project_euler: NotificationChannelGroupLazy("project_euler", "Project Euler") {
        val news = channel("pe_news", "Recent Problems")
        val problems = channel("pe_problems", "News")
    }

    object acmp: NotificationChannelGroupLazy("acmp", "ACMP") {
        val news = channel("acmp_news", "News")
    }

    object olympiads_zaoch: NotificationChannelGroupLazy("olympiads_zaoch", "olympiads.ru/zaoch") {
        val news = channel("olympiads_zaoch_news", "News")
    }

    object test: NotificationChannelGroupLazy("test", "Test group") {
        val test = channel("test", "test channel")
    }


    enum class Importance {
        MIN,
        DEFAULT,
        HIGH;

        fun convert(): Int =
            when(this) {
                DEFAULT -> NotificationManager.IMPORTANCE_DEFAULT
                MIN -> NotificationManager.IMPORTANCE_MIN
                HIGH -> NotificationManager.IMPORTANCE_HIGH
            }
    }

    private fun NotificationChannelGroupLazy.channel(
        id: String,
        name: String,
        importance: Importance = Importance.DEFAULT
    ) = NotificationChannelLazy(id, name, importance, this)
}

abstract class NotificationChannelGroupLazy(private val id: String, val name: String) {
    private var created = false
    fun getId(m: NotificationManagerCompat): String {
        if (!created) {
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
    private val groupLazy: NotificationChannelGroupLazy
) {
    private var created = false
    fun getId(context: Context): String {
        if (!created) {
            val m = NotificationManagerCompat.from(context)
            val channel = NotificationChannel(id, name, importance.convert()).apply {
                group = groupLazy.getId(m)
            }
            m.createNotificationChannel(channel)
            created = true
        }
        return id
    }
}

object NotificationIds {
    private object nextId {
        private var id = 0
        operator fun invoke() = ++id
    }

    private object nextIdInterval {
        private var start = 1_000_000
        private val step = 1_000_000
        operator fun invoke() = IntervalId(start, step).also { start += step }
    }

    data class IntervalId(val from: Int, val length: Int) {
        init {
            require(length > 0) { "illegal interval length: $length" }
        }
        operator fun invoke(int: Int) = from + int.mod(length)
        operator fun invoke(long: Long) = from + long.mod(length)
        operator fun invoke(str: String) = invoke(str.hashCode())
    }

    //codeforces
    val codeforces_contest_monitor = nextId()
    val codeforces_rating_changes = nextId()
    val codeforces_contribution_changes = nextId()
    val makeCodeforcesSystestSubmissionId = nextIdInterval()
    val makeCodeforcesFollowBlogId = nextIdInterval()
    val codeforces_follow_progress = nextId()
    val makeCodeforcesUpsolveProblemId = nextIdInterval()

    //atcoder
    val atcoder_rating_changes = nextId()

    //project euler
    val makeProjectEulerRecentProblemNotificationId = nextIdInterval()
    val makeProjectEulerNewsNotificationId = nextIdInterval()

    //acmp
    val makeACMPNewsNotificationId = nextIdInterval()

    //zaoch
    val makeZaochNewsNotificationId = nextIdInterval()

    //test
    val testId = nextId()
}
