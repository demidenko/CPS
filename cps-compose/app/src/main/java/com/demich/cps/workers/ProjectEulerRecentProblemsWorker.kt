package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.R
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.api.projecteuler.ProjectEulerUrls
import com.demich.cps.platforms.clients.ProjectEulerClient
import com.demich.cps.platforms.utils.ProjectEulerParser
import com.demich.cps.platforms.utils.ProjectEulerRssParser
import com.demich.cps.ui.platformLogoResId
import com.demich.cps.utils.getSystemTime
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.flowOf
import com.demich.datastore_itemized.value
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ProjectEulerRecentProblemsWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object : CPSPeriodicWorkProvider {
        override val workName get() = "pe_recent"

        override fun getWork(context: Context) = object : CPSPeriodicWork(name = workName, context = context) {
            override suspend fun isEnabled() =
                context.settingsCommunity.enabledNewsFeeds().contains(project_euler_problems)

            override suspend fun requestBuilder() =
                CPSPeriodicWorkRequestBuilder<ProjectEulerRecentProblemsWorker>(
                    repeatInterval = 6.hours
                )

            override fun flowOfInfo() =
                context.workerStorage.flowOf {
                    mapOf("next problem" to problemPublishTime.value)
                }
        }

        @IgnorableReturnValue
        suspend fun extractAndSaveHint(
            parser: ProjectEulerRssParser,
            context: Context
        ): Instant? {
            val currentTime = getSystemTime()
            return parser.parseProblems()
                .mapNotNull { (id, date) -> date.takeIf { it > currentTime } }
                .minOrNull()
                ?.also {
                    context.workerStorage.problemPublishTime.setValue(it)
                }
        }
    }

    override suspend fun runWork() {
        scanProblems()
        //TODO: wait if time is close
        enqueueByHint()
    }

    private suspend fun scanProblems() {
        ProjectEulerParser().parseRecentProblems(ProjectEulerClient().getRecentPage()).scanNewsFeed(
            newsFeed = project_euler_problems,
            storage = NewsFeedStorage(context)
        ) { post ->
            val problemId = post.id.toInt()
            notificationChannels.project_euler.problems(problemId).notify(context) {
                subText = "Project Euler • New problem published!"
                contentTitle = "Problem $problemId"
                bigContent = post.name
                smallIcon = platformLogoResId(platform = project_euler)
                colorResId = R.color.project_euler_main
                time = post.date
                autoCancel = true
                url = ProjectEulerUrls.problem(problemId)
            }
        }
    }

    private suspend fun enqueueByHint() {
        val nextTime = getPublishTimeHint() ?: return
        work.enqueueAtIfEarlier(nextTime + 1.minutes)
    }

    private suspend fun getPublishTimeHint(): Instant? {
        context.workerStorage.problemPublishTime().let {
            if (it != null && it > workerStartTime) return it
        }
        val rssParser = ProjectEulerRssParser(rssPage = ProjectEulerClient().getRSSPage())
        return extractAndSaveHint(parser = rssParser, context = context)
    }
}

private val Context.workerStorage get() = ProjectEulerRecentProblemsWorkerStorage(this)

private class ProjectEulerRecentProblemsWorkerStorage(context: Context): ItemizedDataStore(context.dataStore) {
    companion object {
        private val Context.dataStore by workerDataStoreDelegate(ProjectEulerRecentProblemsWorker)
    }

    val problemPublishTime = jsonCPS.itemNullable<Instant>(name = "problem_publish_time")
}