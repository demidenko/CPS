package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.*
import com.demich.cps.news.settings.NewsSettingsDataStore
import com.demich.cps.news.settings.NewsSettingsDataStore.NewsFeed.atcoder_news
import com.demich.cps.news.settings.NewsSettingsDataStore.NewsFeed.project_euler_news
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.notifications.attachUrl
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.notifications.setBigContent
import com.demich.cps.notifications.setWhen
import com.demich.cps.platforms.api.AtCoderApi
import com.demich.cps.platforms.api.ProjectEulerApi
import com.demich.cps.platforms.utils.AtCoderUtils
import com.demich.cps.platforms.utils.ProjectEulerUtils
import org.jsoup.Jsoup
import kotlin.time.Duration.Companion.hours

class NewsWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSWork(name = "news", context = context) {
            override suspend fun isEnabled(): Boolean {
                val enabledFeeds = context.settingsNews.enabledNewsFeeds() - NewsSettingsDataStore.NewsFeed.project_euler_problems
                return enabledFeeds.isNotEmpty()
            }

            override val requestBuilder: PeriodicWorkRequest.Builder
                get() = CPSPeriodicWorkRequestBuilder<NewsWorker>(
                    repeatInterval = 6.hours,
                    batteryNotLow = true
                )
        }
    }

    val settings by lazy { context.settingsNews }
    override suspend fun runWork(): Result {
        val jobs = buildList {
            settings.enabledNewsFeeds().let { enabled ->
                if (atcoder_news in enabled) add(::atcoderNews)
                if (project_euler_news in enabled) add(::projectEulerNews)
            }
        }

        jobs.joinAllWithProgress()

        return Result.success()
    }

    private suspend fun atcoderNews() {
        settings.scanNewsFeed(
            newsFeed = atcoder_news,
            posts = AtCoderUtils.extractNews(source = AtCoderApi.getMainPage())
        ) {
            notificationChannels.atcoder.news(it.id.toInt()).notify(context) {
                setSubText("atcoder news")
                setBigContent(it.title.trim())
                setSmallIcon(R.drawable.ic_news)
                setWhen(it.time)
                attachUrl(url = AtCoderApi.urls.post(it.id.toInt()), context = context)
                //setColor
                setAutoCancel(true)
            }
        }
    }

    private suspend fun projectEulerNews() {
        settings.scanNewsFeed(
            newsFeed = project_euler_news,
            posts = ProjectEulerUtils.extractNews(source = ProjectEulerApi.getRSSPage())
        ) {
            notificationBuildAndNotify(
                context = context,
                channel = NotificationChannels.project_euler.news,
                notificationId = NotificationIds.makeProjectEulerNewsId(it.id.toInt())
            ) {
                setSubText("Project Euler news")
                setContentTitle(it.title)
                setBigContent(
                    Jsoup.parse(it.descriptionHtml).text()
                        .replace("\n", "")
                        .replace("<p>", "")
                        .replace("</p>", "\n\n")
                )
                setSmallIcon(R.drawable.ic_news)
                setColor(context.getColor(R.color.project_euler_main))
                setShowWhen(false)
                setAutoCancel(true)
                attachUrl(url = ProjectEulerApi.urls.news, context = context)
            }
        }
    }
}