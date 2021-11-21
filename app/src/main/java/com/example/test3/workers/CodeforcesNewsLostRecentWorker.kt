package com.example.test3.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.test3.NewsFragment
import com.example.test3.account_manager.STATUS
import com.example.test3.news.settingsNews
import com.example.test3.room.LostBlogEntry
import com.example.test3.room.getLostBlogsDao
import com.example.test3.utils.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours


class CodeforcesNewsLostRecentWorker(private val context: Context, params: WorkerParameters): CoroutineWorker(context, params){
    companion object {

        suspend fun updateInfo(context: Context, progress: MutableStateFlow<Pair<Int,Int>?>) {
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
        }

        suspend fun isEnabled(context: Context): Boolean = context.settingsNews.lostEnabled()
    }

    override suspend fun doWork(): Result {

        if(!isEnabled(context)){
            WorkersCenter.stopWorker(context, WorkersNames.codeforces_news_lost_recent)
            return Result.success()
        }

        val recentBlogEntries = CodeforcesUtils.parseRecentBlogEntriesPage(
            CodeforcesAPI.getPageSource("recent-actions", NewsFragment.getCodeforcesContentLanguage(context)) ?: return Result.failure()
        ).let { list ->
            val authors = CodeforcesUtils.getUsersInfo(list.map { blog -> blog.authorHandle })
            list.map { blogEntry ->
                authors[blogEntry.authorHandle]?.takeIf { it.status==STATUS.OK }
                ?.let {
                    blogEntry.copy(authorColorTag = CodeforcesUtils.getTagByRating(it.rating))
                } ?: blogEntry
            }
        }

        if(recentBlogEntries.isEmpty()) return Result.failure()

        val currentTime = getCurrentTime()

        fun isNew(blogCreationTime: Instant) = currentTime - blogCreationTime < 24.hours
        fun isOldLost(blogCreationTime: Instant) = currentTime - blogCreationTime > 7.days

        val blogsDao = getLostBlogsDao(context)
        val minRatingColorTag = context.settingsNews.lostMinRating()

        //get current suspects with removing old ones
        val suspects = blogsDao.getSuspects()
            .partition { blogEntry ->
                isNew(blogEntry.creationTime) && blogEntry.authorColorTag>=minRatingColorTag
            }.also {
                blogsDao.remove(it.second)
            }.first

        //catch new suspects from recent actions
        recentBlogEntries.forEach { blog ->
            if(blog.authorColorTag>=minRatingColorTag && suspects.none { it.id == blog.id }) {
                val creationTime = CodeforcesUtils.getBlogCreationTime(blog.id)
                if(isNew(creationTime)) {
                    val newSuspect = LostBlogEntry(
                        id = blog.id,
                        title = blog.title,
                        authorHandle = blog.authorHandle,
                        authorColorTag = blog.authorColorTag,
                        creationTime = creationTime,
                        isSuspect = true,
                        timeStamp = Instant.DISTANT_PAST
                    )
                    blogsDao.insert(newSuspect)
                }
            }
        }

        val recentBlogIDs = recentBlogEntries.map { it.id }

        //remove from lost
        blogsDao.remove(
            blogsDao.getLost().filter { blogEntry ->
                isOldLost(blogEntry.creationTime) || blogEntry.id in recentBlogIDs
            }
        )

        //suspect become lost
        suspects.forEach { blogEntry ->
            if(blogEntry.id !in recentBlogIDs){
                blogsDao.update(blogEntry.copy(
                    isSuspect = false,
                    timeStamp = currentTime
                ))
            }
        }

        return Result.success()
    }

}