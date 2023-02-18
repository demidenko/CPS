package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.news.settings.NewsSettingsDataStore
import com.demich.cps.news.settings.NewsSettingsDataStore.NewsFeed.*
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

    override suspend fun runWork(): Result {
        val jobs = buildList {
            NewsSettingsDataStore(context).enabledNewsFeeds().let { enabled ->
                if (ATCODER in enabled) add(::atcoderNews)
                if (PROJECTEULER in enabled) add(::projectEulerNews)
            }
        }

        jobs.joinAllWithProgress()

        return Result.success()
    }

    private suspend fun atcoderNews() {
        //TODO
    }

    private suspend fun projectEulerNews() {
        //TODO
    }
}