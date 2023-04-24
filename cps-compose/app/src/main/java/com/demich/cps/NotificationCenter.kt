package com.demich.cps

import android.app.NotificationChannelGroup
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.demich.cps.notifications.Importance
import com.demich.cps.notifications.NotificationChannelInfo
import com.demich.cps.notifications.NotificationIdProvider
import com.demich.cps.notifications.createNotificationChannel
import com.demich.cps.notifications.notifyBy


fun notificationBuilder(
    context: Context,
    channel: NotificationChannelLazy,
    buildBody: NotificationCompat.Builder.() -> Unit
): NotificationCompat.Builder {
    return NotificationCompat.Builder(context, channel.getId(context)).apply(buildBody)
}

fun notificationBuildAndNotify(
    context: Context,
    channel: NotificationChannelLazy,
    notificationId: Int,
    buildBody: NotificationCompat.Builder.() -> Unit
) = notificationBuilder(context, channel, buildBody).notifyBy(NotificationManagerCompat.from(context), notificationId)


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



}

abstract class NotificationChannelGroupLazy(id: String, name: String) {
    private val group = NotificationChannelGroup(id, name)
    protected fun channel(
        id: String,
        name: String,
        importance: Importance = Importance.DEFAULT
    ) = NotificationChannelLazy(NotificationChannelInfo(id, name, importance, group))
}

class NotificationChannelLazy(
    private val channelInfo: NotificationChannelInfo
) {
    fun getId(context: Context): String {
        NotificationManagerCompat.from(context).createNotificationChannel(channelInfo)
        return channelInfo.id
    }
}

object NotificationIds: NotificationIdProvider() {
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
