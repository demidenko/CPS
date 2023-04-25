package com.demich.cps.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.demich.cps.*
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.contests.monitors.CodeforcesMonitorNotifier
import com.demich.cps.contests.monitors.flowOfContestData
import com.demich.cps.contests.monitors.launchIn
import com.demich.cps.notifications.attachUrl
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.api.CodeforcesProblemVerdict
import com.demich.cps.platforms.api.CodeforcesSubmission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CodeforcesMonitorWorker(val context: Context, params: WorkerParameters): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val monitor = CodeforcesMonitorDataStore(context)

        val contestId = monitor.contestId.flow.filterNotNull().first()
        val handle = monitor.handle()

        val notificationBuilder = createNotificationBuilder(handle).apply {
            setForeground(ForegroundInfo(notificationId, build()))
        }

        withContext(Dispatchers.IO) {
            monitor.launchIn(
                scope = this,
                onRatingChange = { ratingChange ->
                    launch { CodeforcesAccountManager(context).applyRatingChange(ratingChange) }
                },
                onSubmissionFinalResult = { submission ->
                    launch { notify(submission) }
                }
            )

            val notifier = CodeforcesMonitorNotifier(
                context = context,
                notificationBuilder = notificationBuilder,
                handle = handle
            )

            monitor.flowOfContestData()
                .takeWhile { it?.contestId == contestId }
                .filterNotNull()
                .distinctUntilChanged()
                .collect {
                    notifier.apply(it)
                }
        }

        return Result.success()
    }

    private fun createNotificationBuilder(handle: String) =
        notificationChannels.codeforces.contest_monitor.builder(context) {
            setSmallIcon(R.drawable.ic_monitor)
            setSubText(handle)
            setShowWhen(false)
            setSilent(true)
            setStyle(NotificationCompat.DecoratedCustomViewStyle())
            //TODO intent open contest screen
        }

    private fun notify(submission: CodeforcesSubmission) =
        notificationChannels.codeforces.submission_result(submission.id).notify(context) {
            if (submission.verdict == CodeforcesProblemVerdict.OK) {
                setSmallIcon(R.drawable.ic_problem_ok)
                color = context.getColor(R.color.success)
            } else {
                setSmallIcon(R.drawable.ic_problem_fail)
                color = context.getColor(R.color.fail)
            }
            val problemName = "${submission.contestId}${submission.problem.index}"
            val result = submission.makeVerdict()
            setContentTitle("Problem $problemName: $result")
            setSubText("Codeforces system testing result")
            setShowWhen(false)
            setAutoCancel(true)
            attachUrl(url = CodeforcesApi.urls.submission(submission), context)
        }
}