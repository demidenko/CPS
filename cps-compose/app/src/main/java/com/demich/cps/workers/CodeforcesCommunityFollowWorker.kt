package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.community.follow.followRepository
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.features.codeforces.follow.database.CodeforcesUserBlog
import com.demich.cps.features.codeforces.follow.database.handle
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.userInfoOrNull
import com.demich.cps.utils.toSystemLocalDate
import com.demich.datastore_itemized.edit
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

    override suspend fun runWork(): Result {
        val repository = context.followRepository

        //TODO: consider skip this if blogs.size is small
        //update userInfo to keep fresh lastOnlineTime
        val profiles = repository.updateUsers()

        val lastSuccessItem = hintsDataStore.followLastSuccessTime
        val lastOnlineItem = hintsDataStore.followLastUserOnlineTime
        val blogs = repository.blogs()


        val blogsToUpdate = lastOnlineItem().let { last ->
            blogs.filter {
                val canSkip = profiles[it.handle]?.let { profile ->
                    profile is ProfileResult.Success && profile.userInfo.lastOnlineTime != last[it.id]
                } ?: false

                it.blogSize == null || !canSkip
            }
        }

        blogsToUpdate
            .sortedByDescending { it.userLastOnlineTime() }
            .forEachWithProgress { blog ->
                val handle = blog.handle
                if (handle !in proceeded) {
                    repository.getAndReloadBlogEntries(handle).getOrThrow()
                    lastOnlineItem.edit { put(blog.id, blog.userLastOnlineTime()) }
                    proceeded.add(handle)
                }
            }

        work.enqueueInIfEarlier(
            duration = nextEnqueueIn(blogsCount = blogs.size, proceeded = proceeded.size).coerceAtLeast(2.hours)
        )

        lastSuccessItem.setValue(workerStartTime)

        return Result.success()
    }
}

private fun List<CodeforcesUserBlog>.necessaryToUpdate(lastExecTime: Instant?, currentTime: Instant) =
    if (lastExecTime == null || !isSameDay(currentTime, lastExecTime)) this
    else filter {
        it.blogSize == null || !it.isUserInactive(currentTime)
    }

//note that cf can have different lastOnlineTime from api and web sources
private fun CodeforcesUserBlog.isUserInactive(currentTime: Instant) =
    currentTime - userLastOnlineTime() > 2.days

private fun CodeforcesUserBlog.userLastOnlineTime(): Instant =
    userProfile.userInfoOrNull()?.lastOnlineTime ?: Instant.DISTANT_PAST

private fun isSameDay(a: Instant, b: Instant): Boolean =
    a.toSystemLocalDate() == b.toSystemLocalDate()

private fun nextEnqueueIn(
    blogsCount: Int,
    proceeded: Int
): Duration {
    return 30.minutes * proceeded
}