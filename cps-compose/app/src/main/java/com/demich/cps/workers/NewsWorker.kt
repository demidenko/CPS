package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.R
import com.demich.cps.community.settings.CommunitySettingsDataStore
import com.demich.cps.community.settings.CommunitySettingsDataStore.NewsFeed.atcoder_news
import com.demich.cps.community.settings.CommunitySettingsDataStore.NewsFeed.project_euler_news
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.api.atcoder.AtCoderUrls
import com.demich.cps.platforms.api.projecteuler.ProjectEulerUrls
import com.demich.cps.platforms.clients.AtCoderClient
import com.demich.cps.platforms.clients.ProjectEulerClient
import com.demich.cps.platforms.utils.ProjectEulerUtils
import com.demich.cps.platforms.utils.atcoder.AtCoderUtils
import com.demich.cps.utils.asHtmlToSpanned
import kotlin.time.Duration.Companion.hours

class NewsWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object : CPSPeriodicWorkProvider {
        override fun getWork(context: Context) = object : CPSPeriodicWork(name = "news", context = context) {
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

    override suspend fun runWork(): Result {
        joinAllWithProgress {
            context.settingsCommunity.enabledNewsFeeds().let { enabled ->
                if (atcoder_news in enabled) add(::atcoderNews)
                if (project_euler_news in enabled) add(::projectEulerNews)
            }
        }

        return Result.success()
    }

    private suspend fun atcoderNews() {
        hintsDataStore.scanNewsFeed(
            newsFeed = atcoder_news,
            posts = AtCoderUtils.extractNews(source = AtCoderClient.getMainPage())
        ) { post ->
            notificationChannels.atcoder.news(post.id.toInt()).notify(context) {
                subText = "atcoder news"
                bigContent = post.title.trim() //TODO: title + content html
                smallIcon = R.drawable.ic_community
                post.time?.let { time = it }
                url = AtCoderUrls.post(post.id.toInt())
                //setColor
                autoCancel = true
            }
        }
    }

    private suspend fun projectEulerNews() {
        val rssPage = ProjectEulerClient.getRSSPage()

        hintsDataStore.scanNewsFeed(
            newsFeed = project_euler_news,
            posts = ProjectEulerUtils.extractNewsFromRSSPage(rssPage = rssPage)
        ) { post ->
            notificationChannels.project_euler.news(post.id.toInt()).notify(context) {
                subText = "Project Euler news"
                contentTitle = post.title
                //TODO: still <p> .. </p>
                bigContent = post.descriptionHtml.asHtmlToSpanned()
                smallIcon = R.drawable.ic_community
                colorResId = R.color.project_euler_main
                time = null
                autoCancel = true
                url = ProjectEulerUrls.news
            }
        }

        if (ProjectEulerRecentProblemsWorker.getWork(context).isEnabled()) {
            ProjectEulerRecentProblemsWorker.extractAndSaveHint(rssPage = rssPage, context = context)
        }
    }
}