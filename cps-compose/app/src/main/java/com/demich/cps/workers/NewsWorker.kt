package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
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
            override suspend fun isEnabled() = true
            override val requestBuilder: PeriodicWorkRequest.Builder
                get() = CPSPeriodicWorkRequestBuilder<NewsWorker>(
                    repeatInterval = 6.hours,
                    batteryNotLow = true
                )
        }
    }

    override suspend fun runWork(): Result {
        val jobs = buildList {
            add(::projectEulerNews)
            add(::atcoderNews)
        }

        jobs.joinAllWithProgress()

        return Result.success()
    }

    private suspend fun atcoderNews() {

    }

    private suspend fun projectEulerNews() {

    }
}