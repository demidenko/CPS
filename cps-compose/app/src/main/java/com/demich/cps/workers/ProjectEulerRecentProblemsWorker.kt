package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.R
import com.demich.cps.community.settings.CommunitySettingsDataStore
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.notifications.attachUrl
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.notifications.setBigContent
import com.demich.cps.platforms.api.ProjectEulerApi
import com.demich.cps.platforms.utils.ProjectEulerUtils
import com.demich.kotlin_stdlib_boost.minOfNotNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class ProjectEulerRecentProblemsWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSPeriodicWork(name = "pe_recent", context = context) {
            override suspend fun isEnabled() =
                context.settingsCommunity.enabledNewsFeeds().contains(CommunitySettingsDataStore.NewsFeed.project_euler_problems)

            override val requestBuilder: PeriodicWorkRequest.Builder
                get() = CPSPeriodicWorkRequestBuilder<ProjectEulerRecentProblemsWorker>(
                    repeatInterval = 6.hours
                )
        }
    }

    override suspend fun runWork(): Result {
        scanNews()
        //TODO: improve logic with hint: sync with recent news worker, wait if time is close
        enqueueByHint()
        return Result.success()
    }

    private suspend fun scanNews() {
        context.settingsCommunity.scanNewsFeed(
            newsFeed = CommunitySettingsDataStore.NewsFeed.project_euler_problems,
            posts = ProjectEulerUtils.extractRecentProblems(ProjectEulerApi.getRecentPage())
        ) {
            val problemId = it.id.toInt()
            notificationChannels.project_euler.problems(problemId).notify(context) {
                setSubText("Project Euler • New problem published!")
                setContentTitle("Problem $problemId")
                setBigContent(it.name)
                setSmallIcon(R.drawable.ic_logo_projecteuler)
                setColor(context.getColor(R.color.project_euler_main))
                setShowWhen(true)
                setAutoCancel(true)
                attachUrl(url = ProjectEulerApi.urls.problem(problemId), context = context)
            }
        }
    }

    private suspend fun enqueueByHint() {
        val rssPage = ProjectEulerApi.getRSSPage()

        val nextDate = ProjectEulerUtils.extractProblemsFromRssPage(rssPage)
            .minOfNotNull { (id, date) -> date.takeIf { it > workerStartTime } }
            ?: return

        work.enqueueAtIfEarlier(nextDate + 1.minutes)
    }
}