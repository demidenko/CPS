package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.community.follow.followRepository
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.features.codeforces.follow.database.CodeforcesUserBlog
import com.demich.cps.utils.toSystemDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant


class CodeforcesCommunityFollowWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object : CPSPeriodicWorkProvider {
        override fun getWork(context: Context) = object : CPSPeriodicWork(name = "cf_follow", context = context) {
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
        workerStartTime - userLastOnlineTime() > 2.days

    private suspend fun needFullCheck(): Boolean {
        val lastSuccess = hintsDataStore.followLastSuccessTime() ?: return true
        if (workerStartTime.toSystemDateTime().date != lastSuccess.toSystemDateTime().date) {
            return true
        }
        return false
    }

    override suspend fun runWork(): Result {
        val repository = context.followRepository

        //TODO: consider skip this if blogs.size is small
        //update userInfo to keep fresh lastOnlineTime
        repository.updateUsers()

        val blogs = repository.blogs()

        blogs
            .let {
                if (needFullCheck()) it
                else it.filter { blog -> blog.blogEntries == null || !blog.isUserInactive() }
            }
            .sortedByDescending { it.userLastOnlineTime() }
            .forEachWithProgress {
                val handle = it.handle
                if (handle !in proceeded) {
                    repository.getAndReloadBlogEntries(handle).getOrThrow()
                    proceeded.add(handle)
                }
            }

        work.enqueueInIfEarlier(
            duration = nextEnqueueIn(blogsCount = blogs.size, proceeded = proceeded.size).coerceAtLeast(2.hours)
        )

        hintsDataStore.followLastSuccessTime.setValue(workerStartTime)

        return Result.success()
    }
}

private fun CodeforcesUserBlog.userLastOnlineTime(): Instant =
    userInfo?.lastOnlineTime ?: Instant.DISTANT_PAST

private fun nextEnqueueIn(
    blogsCount: Int,
    proceeded: Int
): Duration {
    return 30.minutes * proceeded
}