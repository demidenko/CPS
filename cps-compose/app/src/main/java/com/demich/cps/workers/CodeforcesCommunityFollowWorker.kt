package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.community.follow.followListDao
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.features.codeforces.follow.database.CodeforcesUserBlog
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


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

    //note that cf can have different lastOnlineTime from api and web sources
    private fun CodeforcesUserBlog.isUserInactive() =
        workerStartTime - userInfo.lastOnlineTime > 7.days

    override suspend fun runWork(): Result {
        val dao = context.followListDao
        val blogs = dao.blogs()

        //TODO: consider skip this if blogs.size is small
        //update userInfo to keep fresh lastOnlineTime
        dao.updateUsers()

        blogs.filter {
            it.blogEntries == null || !it.isUserInactive()
        }.forEachWithProgress {
            val handle = it.handle
            if (handle !in proceeded) {
                dao.getAndReloadBlogEntries(handle).getOrThrow()
                proceeded.add(handle)
            }
        }

        work.enqueueInIfEarlier(
            duration = nextEnqueueIn(blogsCount = blogs.size, proceeded = proceeded.size).coerceAtLeast(2.hours)
        )

        return Result.success()
    }
}


private fun nextEnqueueIn(
    blogsCount: Int,
    proceeded: Int
): Duration {
    return 30.minutes * proceeded
}