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

fun makePendingIntentOpenUrl(url: String, context: Context): PendingIntent {
    return PendingIntent.getActivity(
        context,
        0,
        Intent(Intent.ACTION_VIEW, Uri.parse(url)),
        PendingIntent.FLAG_IMMUTABLE
    )
}

fun NotificationCompat.Builder.attachUrl(url: String, context: Context) {
    setContentIntent(makePendingIntentOpenUrl(url, context))
}


object NotificationChannels {

    object codeforces: NotificationChannelGroupLazy("codeforces", "CodeForces") {
        val rating_changes get() = channel("cf_rating_changes", "Rating changes", Importance.HIGH)
        val contribution_changes get() = channel("cf_contribution_changes", "Contribution changes", Importance.MIN)
        val contest_monitor get() = channel("cf_contest_monitor", "Contest monitor")
        val submission_result get() = channel("cf_submission_result", "Submissions results")
        val new_blog_entry get() = channel("cf_new_blog_entry", "New blog entries")
        val follow_progress get() = channel("cf_follow_progress", "Follow: update progress", Importance.MIN)
        val upsolving_suggestion get() = channel("cf_upsolving_suggestion", "Upsolving suggestions")
    }

    object atcoder: NotificationChannelGroupLazy("atcoder", "AtCoder") {
        val rating_changes get() = channel("atcoder_rating_changes", "Rating changes", Importance.HIGH)
        val news get() = channel("atcoder_news", "News")
    }

    object project_euler: NotificationChannelGroupLazy("project_euler", "Project Euler") {
        val news get() = channel("pe_news", "News")
        val problems get() = channel("pe_problems", "Recent problems")
    }


    enum class Importance {
        MIN,
        DEFAULT,
        HIGH;

        fun toAndroidImportance(): Int =
            when (this) {
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
    fun getId(m: NotificationManagerCompat): String {
        m.createNotificationChannelGroup(NotificationChannelGroup(id, name))
        return id
    }
}

class NotificationChannelLazy(
    private val id: String,
    val name: String,
    private val importance: NotificationChannels.Importance,
    private val groupLazy: NotificationChannelGroupLazy
) {
    fun getId(context: Context): String {
        val m = NotificationManagerCompat.from(context)
        val channel = NotificationChannel(id, name, importance.toAndroidImportance()).apply {
            group = groupLazy.getId(m)
        }
        m.createNotificationChannel(channel)
        return id
    }
}

object NotificationIds {
    private var firstUnused = 0
    private const val intervalLength = 1 shl 20

    private fun nextId() = ++firstUnused
    private fun nextIdInterval() = IntervalId(firstUnused).also { firstUnused += intervalLength }

    @JvmInline
    value class IntervalId(private val start: Int) {
        operator fun invoke(int: Int) = start + int.mod(intervalLength)
        operator fun invoke(long: Long) = start + long.mod(intervalLength)
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
    val makeAtCoderNewsId = nextIdInterval()

    //project euler
    val makeProjectEulerRecentProblemId = nextIdInterval()
    val makeProjectEulerNewsId = nextIdInterval()

}
