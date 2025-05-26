package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.community.follow.followListDao
import com.demich.cps.community.settings.settingsCommunity
import kotlin.time.Duration.Companion.hours


class CodeforcesCommunityFollowWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSPeriodicWork(name = "cf_follow", context = context) {
            override suspend fun isEnabled() = context.settingsCommunity.codeforcesFollowEnabled()
            override suspend fun requestBuilder() =
                CPSPeriodicWorkRequestBuilder<CodeforcesCommunityFollowWorker>(
                    repeatInterval = 6.hours,
                    batteryNotLow = true
                )

        }
    }

    //save handles between run after fast retry
    private val proceeded = mutableSetOf<String>()

    override suspend fun runWork(): Result {
        val dao = context.followListDao
        val savedHandles = dao.getHandles().shuffled()

        savedHandles.forEachWithProgress { handle ->
            if (handle !in proceeded) {
                dao.getAndReloadBlogEntries(handle).getOrThrow()
                proceeded.add(handle)
            }
        }

        return Result.success()
    }
}
