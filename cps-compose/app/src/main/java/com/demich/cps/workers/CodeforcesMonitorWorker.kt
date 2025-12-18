package com.demich.cps.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.demich.cps.R
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.contests.monitors.CodeforcesMonitorArgs
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.contests.monitors.CodeforcesMonitorNotifier
import com.demich.cps.contests.monitors.flowOfContestData
import com.demich.cps.contests.monitors.launchIn
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import com.demich.cps.platforms.api.codeforces.models.CodeforcesProblemVerdict
import com.demich.cps.platforms.api.codeforces.models.CodeforcesSubmission
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.datastore_itemized.DataStoreEditScope
import com.demich.datastore_itemized.edit
import com.demich.datastore_itemized.value
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
            val startArgs = CodeforcesMonitorArgs(contestId, handle)

            val replace: Boolean
            if (monitor.args() == startArgs) {
                if (getWork(context).flowOfWorkInfo().first().isRunning)
                    replace = false
                else
                    replace = true
            } else {
                replace = true
                monitor.edit {
                    contextOf<DataStoreEditScope>().clear()
                    args.value = startArgs
                }
            }

            getWork(context).enqueue(replace)
        }
    }

    override suspend fun doWork(): Result {
        val monitor = CodeforcesMonitorDataStore(context)

        val args = monitor.args() ?: return Result.failure()

        val notificationBuilder = createNotificationBuilder(handle = args.handle, context = context)
            .also { setForeground(it) }

        val notifier = CodeforcesMonitorNotifier(
            context = context,
            notificationBuilder = notificationBuilder,
            handle = args.handle
        )

        coroutineScope {
            val job = monitor.flowOfContestData()
                .filterNotNull()
                .conflate()
                .onEach { notifier.apply(it) }
                .launchIn(this)

            monitor.launchIn(
                scope = this,
                api = CodeforcesClient,
                pageContentProvider = CodeforcesClient,
                onRatingChange = { ratingChange ->
                    launch { CodeforcesAccountManager().applyRatingChange(ratingChange, context) }
                },
                onSubmissionFinalResult = { submission ->
                    launch { notify(submission) }
                },
                onCompletion = {
                    job.cancel()
                }
            )
        }

        return Result.success()
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
            val result = submission.verdictString()
            contentTitle = "Problem $problemName: $result"
            subText = "Codeforces system testing result"
            time = null
            autoCancel = true
            url = CodeforcesUrls.submission(submission)
        }
}

private fun createNotificationBuilder(handle: String, context: Context) =
    notificationChannels.codeforces.contest_monitor.builder(context) {
        smallIcon = R.drawable.ic_monitor
        subText = handle
        time = null
        silent = true
        setStyle(NotificationCompat.DecoratedCustomViewStyle())
        //TODO intent open contest screen
    }

private fun CodeforcesSubmission.verdictString() =
    if (verdict == CodeforcesProblemVerdict.OK) "OK"
    else "${verdict.name} #${passedTestCount+1}"