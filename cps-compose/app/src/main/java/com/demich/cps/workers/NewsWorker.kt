package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.*
import com.demich.cps.news.settings.NewsSettingsDataStore
import com.demich.cps.news.settings.NewsSettingsDataStore.NewsFeed.atcoder_news
import com.demich.cps.news.settings.NewsSettingsDataStore.NewsFeed.project_euler_news
import com.demich.cps.utils.AtCoderApi
import com.demich.datastore_itemized.edit
import kotlinx.datetime.Instant
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
            override suspend fun isEnabled() = NewsSettingsDataStore(context).enabledNewsFeeds().isNotEmpty()
            override val requestBuilder: PeriodicWorkRequest.Builder
                get() = CPSPeriodicWorkRequestBuilder<NewsWorker>(
                    repeatInterval = 6.hours,
                    batteryNotLow = true
                )
        }
    }

    val settings by lazy { NewsSettingsDataStore(context) }
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
        val panels = Jsoup.parse(AtCoderApi.getMainPage())
            .select("div.panel.panel-default")

        data class Post(
            val title: String,
            val time: Instant,
            val id: String
        )

        val lastPostId = settings.newsFeedsLastIds()[atcoder_news]

        val newPosts = buildList {
            for (panel in panels) {
                val header = panel.expectFirst("div.panel-heading")
                val titleElement = header.expectFirst("h3.panel-title")
                val timeElement = header.expectFirst("span.tooltip-unix")
                val id = titleElement.expectFirst("a").attr("href").removePrefix("/posts/")

                if (id == lastPostId) break

                add(Post(
                    title = titleElement.text(),
                    time = Instant.fromEpochSeconds(timeElement.attr("title").toLong()),
                    id = id
                ))
            }
        }

        if (lastPostId != null) {
            newPosts.forEach {
                notificationBuildAndNotify(
                    context = context,
                    channel = NotificationChannels.atcoder.news,
                    notificationId = NotificationIds.makeAtCoderNewsId(it.id.toInt())
                ) {
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

        newPosts.firstOrNull()?.let {
            settings.newsFeedsLastIds.edit {
                this[atcoder_news] = it.id
            }
        }
    }

    private suspend fun projectEulerNews() {
        //TODO
    }
}