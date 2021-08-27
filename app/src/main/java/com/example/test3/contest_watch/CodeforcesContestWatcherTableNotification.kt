package com.example.test3.contest_watch

import android.content.Context
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.italic
import com.example.test3.*
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.utils.*
import java.util.concurrent.TimeUnit

class CodeforcesContestWatcherTableNotification(
    val context: Context,
    val handle: String,
    val notificationTable: NotificationCompat.Builder
): CodeforcesContestWatchListener {
    val notificationManager by lazy { NotificationManagerCompat.from(context) }

    var contestType = CodeforcesContestType.UNDEFINED
    var contestPhase = CodeforcesContestPhase.UNDEFINED
    var changes = false
    var contestantRank = ""
    var contestantPoints = ""
    var participationType = CodeforcesParticipationType.NOT_PARTICIPATED

    val rview_small = RemoteViews(context.packageName, R.layout.cf_watcher_notification_small)
    val rview_big = RemoteViews(context.packageName, R.layout.cf_watcher_notification_big)
    val rviews = arrayOf(rview_small, rview_big)
    val rviewsByProblem = mutableMapOf<String, RemoteViews>()

    override fun onSetContestNameAndType(contestName: String, contestType: CodeforcesContestType) {
        changes = true
        notificationTable.setSubText("$contestName • $handle")
        this.contestType = contestType
    }

    private fun doubleToString(x: Double) = x.toString().removeSuffix(".0")

    private val successColor = getColorFromResource(context, R.color.success)
    private val failColor = getColorFromResource(context, R.color.fail)
    private fun spanForProblemResult(result: CodeforcesProblemResult): SpannableStringBuilder =
        SpannableStringBuilder().apply {
            val pts = doubleToString(result.points)
            when (contestType) {
                CodeforcesContestType.CF -> {
                    when (result.type) {
                        CodeforcesProblemStatus.FINAL -> {
                            if(result.points == 0.0){
                                if(result.rejectedAttemptCount > 0) color(failColor){ append("-${result.rejectedAttemptCount}") }
                            }else{
                                bold { color(successColor){ append(pts) } }
                            }
                        }
                        CodeforcesProblemStatus.PRELIMINARY -> {
                            if(result.points == 0.0){
                                if(contestPhase == CodeforcesContestPhase.SYSTEM_TEST) italic { append("?") }
                            } else bold { append(pts) }
                        }
                    }
                }
                CodeforcesContestType.ICPC -> {
                    if(result.points == 1.0) bold {
                        if(result.type == CodeforcesProblemStatus.FINAL) color(successColor){ append("+") }
                        else append("+")
                    }else{
                        //if(result.rejectedAttemptCount > 0) append("-${result.rejectedAttemptCount}")
                    }
                }
                CodeforcesContestType.IOI -> {
                    if(result.points != 0.0 ) bold { append(pts) }
                }
            }
        }

    override fun onSetProblemNames(problemNames: Array<String>) {
        changes = true
        rview_big.removeAllViews(R.id.cf_watcher_notification_table_tasks)
        rviewsByProblem.clear()
        problemNames.forEach { problemName ->
            val r = RemoteViews(context.packageName, R.layout.cf_watcher_notification_table_column)
            r.setTextViewText(R.id.cf_watcher_notification_table_column_header, problemName)
            r.setTextViewText(R.id.cf_watcher_notification_table_column_cell, "")
            rview_big.addView(R.id.cf_watcher_notification_table_tasks, r)
            rviewsByProblem[problemName] = r
        }
    }

    override fun onSetContestPhase(phase: CodeforcesContestPhase) {
        changes = true
        contestPhase = phase
        rviews.forEach { it.setChronometer(
            R.id.cf_watcher_notification_progress,
            SystemClock.elapsedRealtime(), null, false) }
        rviews.forEach { it.setTextViewText(R.id.cf_watcher_notification_progress, "") }
        rviews.forEach { it.setTextViewText(R.id.cf_watcher_notification_phase, phase.getTitle()) }
    }

    override fun onSetRemainingTime(timeSeconds: Long) {
        changes = true
        rviews.forEach { it.setChronometer(R.id.cf_watcher_notification_progress, SystemClock.elapsedRealtime() + TimeUnit.SECONDS.toMillis(timeSeconds), null, true) }
    }

    override fun onSetSysTestProgress(percents: Int) {
        changes = true
        rviews.forEach { it.setTextViewText(R.id.cf_watcher_notification_progress, "$percents%") }
    }

    override fun onSetContestantRank(rank: Int) {
        changes = true
        contestantRank =
            if(participationType == CodeforcesParticipationType.CONTESTANT) "$rank"
            else "*$rank"
        rview_big.setTextViewText(R.id.cf_watcher_notification_rank, contestantRank)
    }

    override fun onSetContestantPoints(points: Double) {
        changes = true
        contestantPoints = doubleToString(points)
        rview_big.setTextViewText(R.id.cf_watcher_notification_points, contestantPoints)
    }

    override fun onSetParticipationType(type: CodeforcesParticipationType) {
        changes = true
        participationType = type
    }

    override fun onSetProblemResult(problemName: String, result: CodeforcesProblemResult) {
        changes = true
        rviewsByProblem[problemName]?.run{
            setTextViewText(R.id.cf_watcher_notification_table_column_cell, spanForProblemResult(result))
        }
    }

    override fun onSetProblemSystestResult(submission: CodeforcesSubmission) {
        val problemName = "${submission.contestId}${submission.problem.index}"
        val result = submission.makeVerdict()
        notificationBuildAndNotify(
            context,
            NotificationChannels.codeforces_contest_watcher,
            NotificationIDs.makeCodeforcesSystestSubmissionID(submission.id)
        ) {
            if(submission.verdict == CodeforcesProblemVerdict.OK){
                setSmallIcon(R.drawable.ic_problem_ok)
                color = successColor
            }else{
                setSmallIcon(R.drawable.ic_problem_fail)
                color = failColor
            }
            setContentTitle("Problem $problemName: $result")
            setSubText("Codeforces system testing result")
            setShowWhen(false)
            setAutoCancel(true)
            setContentIntent(makePendingIntentOpenURL(CodeforcesURLFactory.submission(submission), context))
        }
    }

    override suspend fun onRatingChange(ratingChange: CodeforcesRatingChange) {
        CodeforcesAccountManager(context).applyRatingChange(ratingChange)
    }

    override fun commit() {
        if(!changes) return
        changes = false


        notificationTable.setCustomContentView(rview_small.apply {
            setTextViewText(
                R.id.cf_watcher_notification_rank,
                if(participationType == CodeforcesParticipationType.NOT_PARTICIPATED) "not participated"
                else "rank: $contestantRank • points: $contestantPoints"
            )
        })

        notificationTable.setCustomBigContentView(
            if (participationType == CodeforcesParticipationType.NOT_PARTICIPATED) null
            else rview_big
        )


        notificationManager.notify(NotificationIDs.codeforces_contest_watcher, notificationTable.build())
    }
}