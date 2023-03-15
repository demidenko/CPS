package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.*
import com.demich.cps.news.settings.NewsSettingsDataStore
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.platforms.api.ProjectEulerApi
import com.demich.cps.utils.NewsPostEntry
import com.demich.cps.utils.scanNewsPostEntries
import com.demich.datastore_itemized.edit
import org.jsoup.Jsoup
import kotlin.time.Duration.Companion.hours

class ProjectEulerRecentProblemsWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSWork(name = "pe_recent", context = context) {
            override suspend fun isEnabled() =
                context.settingsNews.enabledNewsFeeds().contains(NewsSettingsDataStore.NewsFeed.project_euler_problems)

            override val requestBuilder: PeriodicWorkRequest.Builder
                get() = CPSPeriodicWorkRequestBuilder<ProjectEulerRecentProblemsWorker>(
                    repeatInterval = 1.hours
                )
        }
    }

    override suspend fun runWork(): Result {
        class RecentProblem(
            val name: String,
            override val id: String
        ): NewsPostEntry

        val settings = context.settingsNews
        scanNewsPostEntries(
            elements = Jsoup.parse(ProjectEulerApi.getRecentPage()).expectFirst("#problems_table").select("td.id_column"),
            getLastId = {
                settings.newsFeedsLastIds()[NewsSettingsDataStore.NewsFeed.project_euler_problems]
            },
            setLastId = {
                settings.newsFeedsLastIds.edit {
                    this[NewsSettingsDataStore.NewsFeed.project_euler_problems] = it
                }
            },
            extractPost = { idCell ->
                idCell.nextElementSibling()?.let { nameCell ->
                    RecentProblem(
                        name = nameCell.text(),
                        id = idCell.text()
                    )
                }
            }
        ) {
            val problemId = it.id.toInt()
            notificationBuildAndNotify(
                context = context,
                channel = NotificationChannels.project_euler.problems,
                notificationId = NotificationIds.makeProjectEulerRecentProblemId(problemId)
            ) {
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

        return Result.success()
    }
}