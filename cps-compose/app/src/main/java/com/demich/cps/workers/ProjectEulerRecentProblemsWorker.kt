package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.R
import com.demich.cps.community.settings.CommunitySettingsDataStore
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.api.projecteuler.ProjectEulerUrls
import com.demich.cps.platforms.clients.ProjectEulerClient
import com.demich.cps.platforms.utils.ProjectEulerUtils
import com.demich.cps.utils.getCurrentTime
import com.demich.kotlin_stdlib_boost.minOfNotNull
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
        override fun getWork(context: Context) = object : CPSPeriodicWork(name = "pe_recent", context = context) {
            override suspend fun isEnabled() =
                context.settingsCommunity.enabledNewsFeeds().contains(CommunitySettingsDataStore.NewsFeed.project_euler_problems)

            override suspend fun requestBuilder() =
                CPSPeriodicWorkRequestBuilder<ProjectEulerRecentProblemsWorker>(
                    repeatInterval = 6.hours
                )
        }

        suspend fun extractAndSaveHint(
            rssPage: String,
            context: Context
        ): Instant? {
            val item = WorkersHintsDataStore(context).projectEulerProblemPublishTime
            val currentTime = getCurrentTime()
            return ProjectEulerUtils.extractProblemsFromRssPage(rssPage)
                .minOfNotNull { (id, date) -> date.takeIf { it > currentTime } }
                .also { item.setValue(it) }
        }
    }

    override suspend fun runWork(): Result {
        scanProblems()
        //TODO: wait if time is close
        enqueueByHint()
        return Result.success()
    }

    private suspend fun scanProblems() {
        hintsDataStore.scanNewsFeed(
            newsFeed = CommunitySettingsDataStore.NewsFeed.project_euler_problems,
            posts = ProjectEulerUtils.extractRecentProblems(ProjectEulerClient.getRecentPage())
        ) { post ->
            val problemId = post.id.toInt()
            notificationChannels.project_euler.problems(problemId).notify(context) {
                subText = "Project Euler • New problem published!"
                contentTitle = "Problem $problemId"
                bigContent = post.name
                smallIcon = R.drawable.ic_logo_projecteuler
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
        hintsDataStore.projectEulerProblemPublishTime().let {
            if (it != null && it > workerStartTime) return it
        }
        return extractAndSaveHint(rssPage = ProjectEulerClient.getRSSPage(), context = context)
    }
}