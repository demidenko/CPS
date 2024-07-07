package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.room.followListDao
import kotlin.time.Duration.Companion.hours


class CodeforcesCommunityFollowWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSWork(name = "cf_follow", context = context) {
            override suspend fun isEnabled() = context.settingsCommunity.codeforcesFollowEnabled()
            override val requestBuilder get() =
                CPSPeriodicWorkRequestBuilder<CodeforcesCommunityFollowWorker>(
                    repeatInterval = 6.hours,
                    batteryNotLow = true
                )

        }
    }

    override suspend fun runWork(): Result {
        val dao = context.followListDao
        val savedHandles = dao.getHandles().shuffled()

        savedHandles.forEachWithProgress { handle ->
            if (dao.getAndReloadBlogEntries(handle).isFailure) return Result.retry()
        }

        return Result.success()
    }
}
