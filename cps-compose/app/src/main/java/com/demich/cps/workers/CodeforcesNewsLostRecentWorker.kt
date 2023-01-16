package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.room.CodeforcesLostBlogEntry
import com.demich.cps.room.lostBlogEntriesDao
import com.demich.cps.utils.codeforces.CodeforcesApi
import com.demich.cps.utils.codeforces.CodeforcesBlogEntry
import com.demich.cps.utils.codeforces.CodeforcesUtils
import com.demich.cps.utils.mapToSet
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours


class CodeforcesNewsLostRecentWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSWork(name = "cf_lost", context = context) {
            override suspend fun isEnabled() = context.settingsNews.codeforcesLostEnabled()
            override val requestBuilder: PeriodicWorkRequest.Builder
                get() = CPSPeriodicWorkRequestBuilder<CodeforcesNewsLostRecentWorker>(
                    repeatInterval = 1.hours
                )
        }

        /*suspend fun updateInfo(context: Context, progress: MutableStateFlow<Pair<Int,Int>?>) {
            val blogsDao = getLostBlogsDao(context)

            val blogEntries = blogsDao.getLost().sortedByDescending { it.id }

            progress.value = 0 to blogEntries.size
            var done = 0

            val users = CodeforcesUtils.getUsersInfo(blogEntries.map { it.authorHandle })
            val locale = NewsFragment.getCodeforcesContentLanguage(context)

            blogEntries.forEach { originalBlogEntry ->
                var blogEntry = originalBlogEntry.copy()

                //updates author's handle color
                users[blogEntry.authorHandle]?.takeIf { it.status==STATUS.OK }?.let { user ->
                    blogEntry = blogEntry.copy(
                        authorColorTag = CodeforcesUtils.getTagByRating(user.rating)
                    )
                }

                CodeforcesAPI.getBlogEntry(blogEntry.id,locale)?.let { response ->
                    if(response.status == CodeforcesAPIStatus.FAILED && response.isBlogNotFound(blogEntry.id)){
                        //remove deleted
                        blogsDao.remove(blogEntry)
                    } else {
                        //update title and author's handle
                        if(response.status == CodeforcesAPIStatus.OK) response.result?.let { freshBlogEntry ->
                            val title = freshBlogEntry.title.removeSurrounding("<p>", "</p>")
                            blogEntry = blogEntry.copy(
                                authorHandle = freshBlogEntry.authorHandle,
                                title = fromHTML(title).toString()
                            )
                        }
                    }
                }

                if(blogEntry != originalBlogEntry) blogsDao.update(blogEntry)

                progress.value = ++done to blogEntries.size
            }

            progress.value = null
        }*/
    }

    //Required against new year color chaos
    private suspend fun List<CodeforcesBlogEntry>.fixedHandleColors(): List<CodeforcesBlogEntry> {
        val authors = CodeforcesUtils.getUsersInfo(map { blog -> blog.authorHandle })
        //TODO: if api load failed?..
        return map { blogEntry ->
            val userInfo = authors[blogEntry.authorHandle]
                ?.takeIf { it.status == STATUS.OK }
            if (userInfo == null) blogEntry
            else blogEntry.copy(authorColorTag = CodeforcesUtils.getTagByRating(rating = userInfo.rating))
        }
    }

    override suspend fun runWork(): Result {
        val locale = context.settingsNews.codeforcesLocale()

        val source = CodeforcesApi.getPageSource(
            urlString = CodeforcesApi.urls.main + "/recent-actions",
            locale = locale
        ) ?: return Result.retry()

        val recentBlogEntries: List<CodeforcesBlogEntry>
            = CodeforcesUtils.extractRecentBlogEntries(source).fixedHandleColors()

        if (recentBlogEntries.isEmpty()) return Result.failure()

        fun isNew(blogCreationTime: Instant) = currentTime - blogCreationTime < 24.hours
        fun isOldLost(blogCreationTime: Instant) = currentTime - blogCreationTime > 7.days

        val dao = context.lostBlogEntriesDao
        val minRatingColorTag = context.settingsNews.codeforcesLostMinRatingTag()

        //get current suspects with removing old ones
        //TODO: glorious code
        val suspects = dao.getSuspects()
            .partition { blogEntry ->
                isNew(blogEntry.creationTime) && blogEntry.authorColorTag >= minRatingColorTag
            }.also {
                dao.remove(it.second)
            }.first

        //catch new suspects from recent actions
        recentBlogEntries
            .filter { it.authorColorTag >= minRatingColorTag }
            .filter { blogEntry -> suspects.none { it.id == blogEntry.id } }
            .forEachWithProgress { blogEntry ->
                val creationTime = CodeforcesApi.runCatching {
                    getBlogEntry(
                        blogEntryId = blogEntry.id,
                        locale = locale
                    ).creationTime
                }.getOrNull() ?: return@forEachWithProgress //TODO: set distant_future and try to know later?

                if (isNew(creationTime)) {
                    dao.insert(
                        CodeforcesLostBlogEntry(
                            id = blogEntry.id,
                            title = blogEntry.title,
                            authorHandle = blogEntry.authorHandle,
                            authorColorTag = blogEntry.authorColorTag,
                            creationTime = creationTime,
                            isSuspect = true,
                            timeStamp = Instant.DISTANT_PAST
                        )
                    )
                }
            }

        val recentIds = recentBlogEntries.mapToSet { it.id }

        //remove from lost
        dao.remove(
            dao.getLost().filter { blogEntry ->
                isOldLost(blogEntry.creationTime) || blogEntry.id in recentIds
            }
        )

        //suspect become lost
        suspects.forEach { blogEntry ->
            if (blogEntry.id !in recentIds) {
                dao.insert(blogEntry.copy(
                    isSuspect = false,
                    timeStamp = currentTime
                ))
            }
        }

        return Result.success()
    }

}