package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.*
import com.demich.cps.community.settings.CommunitySettingsDataStore
import com.demich.cps.community.settings.CommunitySettingsDataStore.NewsFeed.atcoder_news
import com.demich.cps.community.settings.CommunitySettingsDataStore.NewsFeed.project_euler_news
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.notifications.attachUrl
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.notifications.setBigContent
import com.demich.cps.notifications.setWhen
import com.demich.cps.platforms.api.AtCoderApi
import com.demich.cps.platforms.api.ProjectEulerApi
import com.demich.cps.platforms.utils.AtCoderUtils
import com.demich.cps.platforms.utils.ProjectEulerUtils
import com.demich.cps.utils.asHtmlToSpanned
import kotlin.time.Duration.Companion.hours

class NewsWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSPeriodicWork(name = "news", context = context) {
            override suspend fun isEnabled(): Boolean {
                val enabledFeeds = context.settingsCommunity.enabledNewsFeeds() - CommunitySettingsDataStore.NewsFeed.project_euler_problems
                return enabledFeeds.isNotEmpty()
            }

            override suspend fun requestBuilder() =
                CPSPeriodicWorkRequestBuilder<NewsWorker>(
                    repeatInterval = 6.hours,
                    batteryNotLow = true
                )
        }
    }

    val settings by lazy { context.settingsCommunity }
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
                setBigContent(it.title.trim()) //TODO: title + content html
                setSmallIcon(R.drawable.ic_community)
                it.time?.let { time -> setWhen(time) }
                attachUrl(url = AtCoderApi.urls.post(it.id.toInt()), context = context)
                //setColor
                setAutoCancel(true)
            }
        }
    }

    private suspend fun projectEulerNews() {
        val rssPage = ProjectEulerApi.getRSSPage()

        settings.scanNewsFeed(
            newsFeed = project_euler_news,
            posts = ProjectEulerUtils.extractNewsFromRSSPage(rssPage = rssPage)
        ) {
            notificationChannels.project_euler.news(it.id.toInt()).notify(context) {
                setSubText("Project Euler news")
                setContentTitle(it.title)
                //TODO: still <p> .. </p>
                setBigContent(it.descriptionHtml.asHtmlToSpanned())
                setSmallIcon(R.drawable.ic_community)
                setColor(context.getColor(R.color.project_euler_main))
                setShowWhen(false)
                setAutoCancel(true)
                attachUrl(url = ProjectEulerApi.urls.news, context = context)
            }
        }
    }
}