package com.demich.cps.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.demich.cps.R
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.contests.monitors.CodeforcesMonitorNotifier
import com.demich.cps.contests.monitors.flowOfContestData
import com.demich.cps.contests.monitors.launchIn
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.models.CodeforcesProblemVerdict
import com.demich.cps.platforms.api.codeforces.models.CodeforcesSubmission
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

class CodeforcesMonitorWorker(val context: Context, params: WorkerParameters): CoroutineWorker(context, params) {

    companion object {
        fun getWork(context: Context): CPSOneTimeWork =
            object : CPSOneTimeWork(name = "cf_monitor", context = context) {
                override val requestBuilder: OneTimeWorkRequest.Builder
                    get() = OneTimeWorkRequestBuilder<CodeforcesMonitorWorker>()
            }

        suspend fun start(contestId: Int, handle: String, context: Context) {
            val monitor = CodeforcesMonitorDataStore(context)

            val replace: Boolean
            if (contestId == monitor.contestId() && handle == monitor.handle()) {
                if (getWork(context).flowOfWorkInfo().first().isRunning)
                    replace = false
                else
                    replace = true
            } else {
                replace = true
                monitor.reset()
                monitor.handle(handle)
                monitor.contestId(contestId)
            }

            getWork(context).enqueue(replace)
        }
    }

    override suspend fun doWork(): Result {
        val monitor = CodeforcesMonitorDataStore(context)

        val contestId = monitor.contestId.flow.filterNotNull().first()
        val handle = monitor.handle()

        val notificationBuilder = createNotificationBuilder(handle)
            .also { setForeground(it) }

        coroutineScope {
            monitor.launchIn(
                scope = this,
                onRatingChange = { ratingChange ->
                    launch { CodeforcesAccountManager().applyRatingChange(ratingChange, context) }
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
            smallIcon = R.drawable.ic_monitor
            subText = handle
            time = null
            silent = true
            setStyle(NotificationCompat.DecoratedCustomViewStyle())
            //TODO intent open contest screen
        }

    private fun notify(submission: CodeforcesSubmission) =
        notificationChannels.codeforces.submission_result(submission.id).notify(context) {
            if (submission.verdict == CodeforcesProblemVerdict.OK) {
                smallIcon = R.drawable.ic_problem_ok
                colorResId = R.color.success
            } else {
                smallIcon = R.drawable.ic_problem_fail
                colorResId = R.color.fail
            }
            val problemName = "${submission.contestId}${submission.problem.index}"
            val result = submission.makeVerdict()
            contentTitle = "Problem $problemName: $result"
            subText = "Codeforces system testing result"
            time = null
            autoCancel = true
            url = CodeforcesApi.urls.submission(submission)
        }
}