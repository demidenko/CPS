package com.demich.cps.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.demich.cps.*
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.contests.monitors.flowOfContestData
import com.demich.cps.contests.monitors.launchIn
import com.demich.cps.utils.codeforces.CodeforcesApi
import com.demich.cps.utils.codeforces.CodeforcesProblemVerdict
import com.demich.cps.utils.codeforces.CodeforcesSubmission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CodeforcesMonitorWorker(val context: Context, params: WorkerParameters): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val monitor = CodeforcesMonitorDataStore(context)

        val contestId = monitor.contestId.flow.filterNotNull().first()

        val notificationBuilder = createNotificationBuilder(handle = monitor.handle())
        setForeground(ForegroundInfo(NotificationIds.codeforces_contest_monitor, notificationBuilder.build()))

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
            monitor.flowOfContestData()
                .transformWhile {
                    if (it != null && it.contestId == contestId) {
                        emit(it)
                        true
                    } else {
                        false
                    }
                }.distinctUntilChanged().collect {
                    //TODO send to notification
                }
        }

        return Result.success()
    }

    private fun createNotificationBuilder(handle: String) =
        notificationBuilder(context, NotificationChannels.codeforces.contest_monitor) {
            setSmallIcon(R.drawable.ic_contest)
            setSubText(handle)
            setShowWhen(false)
            setSilent(true)
            //setStyle(NotificationCompat.DecoratedCustomViewStyle())
            //TODO intent open contest screen
        }

    private fun notify(submission: CodeforcesSubmission) =
        notificationBuildAndNotify(
            context = context,
            channel = NotificationChannels.codeforces.contest_monitor,
            notificationId = NotificationIds.makeCodeforcesSystestSubmissionId(submission.id)
        ) {
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