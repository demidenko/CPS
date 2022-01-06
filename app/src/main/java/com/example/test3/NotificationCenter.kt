package com.example.test3

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
import com.example.test3.utils.getCurrentTime


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

    //codeforces
    private val group_codeforces by lazy { NotificationChannelGroupLazy("codeforces", "CodeForces") }
    val codeforces_contest_watcher by lazy { NotificationChannelLazy("cf_contest_watcher", "Contest watch", Importance.DEFAULT, group_codeforces) }
    val codeforces_rating_changes by lazy { NotificationChannelLazy("cf_rating_changes", "Rating changes", Importance.HIGH, group_codeforces) }
    val codeforces_contribution_changes by lazy { NotificationChannelLazy("cf_contribution_changes", "Contribution changes", Importance.MIN, group_codeforces) }
    val codeforces_follow_new_blog by lazy { NotificationChannelLazy("cf_follow_new_blog", "Follow: new blog entries", Importance.DEFAULT, group_codeforces) }
    val codeforces_follow_progress by lazy { NotificationChannelLazy("cf_follow_progress", "Follow: update progress", Importance.MIN, group_codeforces) }
    val codeforces_upsolving_suggestion by lazy { NotificationChannelLazy("cf_upsolving_suggestion", "Upsolving suggestions", Importance.DEFAULT, group_codeforces) }

    //atcoder
    private val group_atcoder by lazy { NotificationChannelGroupLazy("atcoder", "AtCoder") }
    val atcoder_rating_changes by lazy { NotificationChannelLazy("atcoder_rating_changes", "Rating changes", Importance.HIGH, group_atcoder) }

    //project euler
    private val group_project_euler by lazy { NotificationChannelGroupLazy("project_euler", "Project Euler") }
    val project_euler_news by lazy { NotificationChannelLazy("pe_news", "Recent Problems", Importance.DEFAULT, group_project_euler) }
    val project_euler_problems by lazy { NotificationChannelLazy("pe_problems", "News", Importance.DEFAULT, group_project_euler) }

    //acmp
    private val group_acmp by lazy { NotificationChannelGroupLazy("acmp", "ACMP") }
    val acmp_news by lazy { NotificationChannelLazy("acmp_news", "News", Importance.DEFAULT, group_acmp) }

    //zaoch
    private val group_zaoch by lazy { NotificationChannelGroupLazy("zaoch", "olympiads.ru/zaoch") }
    val olympiads_zaoch_news by lazy { NotificationChannelLazy("olympiads_zaoch_news", "News", Importance.DEFAULT, group_zaoch) }

    //test
    private val group_test by lazy { NotificationChannelGroupLazy("test", "Test group") }
    val test by lazy { NotificationChannelLazy("test", "test channel", Importance.DEFAULT, group_test) }


    enum class Importance {
        MIN,
        DEFAULT,
        HIGH;

        @RequiresApi(Build.VERSION_CODES.N)
        fun convert(): Int =
            when(this){
                DEFAULT -> NotificationManager.IMPORTANCE_DEFAULT
                MIN -> NotificationManager.IMPORTANCE_MIN
                HIGH -> NotificationManager.IMPORTANCE_HIGH
            }
    }
}

class NotificationChannelGroupLazy(private val id: String, val name: String){
    private var created = false
    @RequiresApi(Build.VERSION_CODES.O)
    fun getID(m: NotificationManagerCompat): String {
        if(!created){
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
){
    private var created = false
    fun getID(context: Context): String {
        if(!created){
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
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
            require(length > 0) { "illegal interval length: $length" }
        }
        operator fun invoke(int: Int) = from + int.mod(length)
        operator fun invoke(long: Long) = from + long.mod(length)
        operator fun invoke(str: String) = invoke(str.hashCode())
    }

    //codeforces
    val codeforces_contest_watcher = nextID()
    val codeforces_rating_changes = nextID()
    val codeforces_contribution_changes = nextID()
    val makeCodeforcesSystestSubmissionID = nextIntervalID()
    val makeCodeforcesFollowBlogID = nextIntervalID()
    val codeforces_follow_progress = nextID()
    val makeCodeforcesUpsolveProblemID = nextIntervalID()

    //atcoder
    val atcoder_rating_changes = nextID()

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

fun makeSimpleNotification(context: Context, id: Int, title: String, content: String, silent: Boolean = true) {
    notificationBuildAndNotify(context, NotificationChannels.test, id) {
        setSmallIcon(R.drawable.ic_news)
        setContentTitle(title)
        setContentText(content)
        setSilent(silent)
        setShowWhen(true)
        setWhen(getCurrentTime().toEpochMilliseconds())
    }
}

fun makeIntentOpenUrl(url: String) = Intent(Intent.ACTION_VIEW, Uri.parse(url))

fun makePendingIntentOpenURL(url: String, context: Context): PendingIntent {
    return PendingIntent.getActivity(context, 0, Intent(Intent.ACTION_VIEW, Uri.parse(url)), PendingIntent.FLAG_IMMUTABLE)
}