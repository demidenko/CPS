package com.example.test3.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.test3.BottomProgressInfo
import com.example.test3.NewsFragment
import com.example.test3.account_manager.STATUS
import com.example.test3.news.settingsNews
import com.example.test3.room.LostBlogEntry
import com.example.test3.room.getLostBlogsDao
import com.example.test3.utils.*
import java.util.concurrent.TimeUnit


class CodeforcesNewsLostRecentWorker(private val context: Context, params: WorkerParameters): CoroutineWorker(context, params){
    companion object {

        suspend fun updateInfo(context: Context, progressInfo: BottomProgressInfo?) {
            val blogsDao = getLostBlogsDao(context)

            val blogEntries = blogsDao.getLost()
                .toTypedArray()

            progressInfo?.start(blogEntries.size)

            //updates author's handle color
            CodeforcesUtils.getUsersInfo(blogEntries.map { it.authorHandle }).let { users ->
                for(i in blogEntries.indices) {
                    val blogEntry = blogEntries[i]
                    users[blogEntry.authorHandle]?.takeIf { it.status==STATUS.OK }?.let { user ->
                        blogEntries[i] = blogEntry.copy(
                            authorColorTag = CodeforcesUtils.getTagByRating(user.rating)
                        )
                    }
                }
            }

            val locale = NewsFragment.getCodeforcesContentLanguage(context)

            for(i in blogEntries.indices) {
                val blogEntry = blogEntries[i]
                CodeforcesAPI.getBlogEntry(blogEntry.id,locale)?.let { response ->
                    if(response.status == CodeforcesAPIStatus.FAILED && response.isBlogNotFound(blogEntry.id)){
                        blogsDao.remove(blogEntry)
                    } else {
                        if(response.status == CodeforcesAPIStatus.OK) response.result?.let { freshBlogEntry ->
                            val title = freshBlogEntry.title.removeSurrounding("<p>", "</p>")
                            blogEntries[i] = blogEntry.copy(
                                authorHandle = freshBlogEntry.authorHandle,
                                title = fromHTML(title).toString()
                            )
                        }
                    }
                    progressInfo?.increment()
                }
            }

            blogsDao.update(blogEntries)

            progressInfo?.finish()
        }

        suspend fun isEnabled(context: Context): Boolean = context.settingsNews.getLostEnabled()
    }

    override suspend fun doWork(): Result {

        if(!isEnabled(context)){
            WorkersCenter.stopWorker(context, WorkersNames.codeforces_news_lost_recent)
            return Result.success()
        }

        val recentBlogs = CodeforcesUtils.parseRecentActionsPage(
            CodeforcesAPI.getPageSource("recent-actions", NewsFragment.getCodeforcesContentLanguage(context)) ?: return Result.failure()
        ).first.let { list ->
            val authors = CodeforcesUtils.getUsersInfo(list.map { blog -> blog.authorHandle })
            list.map { blog ->
                authors[blog.authorHandle]?.takeIf { it.status==STATUS.OK }
                ?.let {
                    blog.copy(authorColorTag = CodeforcesUtils.getTagByRating(it.rating))
                } ?: blog
            }
        }

        if(recentBlogs.isEmpty()) return Result.failure()

        val currentTimeSeconds = getCurrentTimeSeconds()

        fun isNew(blogCreationTimeSeconds: Long): Boolean =
            TimeUnit.SECONDS.toHours(currentTimeSeconds - blogCreationTimeSeconds) < 24

        fun isOldLost(blogCreationTimeSeconds: Long): Boolean =
            TimeUnit.SECONDS.toDays(currentTimeSeconds - blogCreationTimeSeconds) > 7

        val blogsDao = getLostBlogsDao(context)
        val minRating = context.settingsNews.getLostMinRating()

        //get current suspects with removing old ones
        val suspects = blogsDao.getSuspects().let { list ->
            val res = mutableListOf<LostBlogEntry>()
            list.forEach { blog ->
                if (isNew(blog.creationTimeSeconds) && blog.authorColorTag>=minRating) res.add(blog)
                else blogsDao.remove(blog)
            }
            res.toList()
        }

        //catch new suspects from recent actions
        recentBlogs.forEach { blog ->
            if(blog.authorColorTag>=minRating && suspects.none { it.id == blog.id }){
                val creationTimeSeconds = CodeforcesUtils.getBlogCreationTimeSeconds(blog.id)
                if(isNew(creationTimeSeconds)){
                    val newSuspect = LostBlogEntry(
                        id = blog.id,
                        title = blog.title,
                        authorHandle = blog.authorHandle,
                        authorColorTag = blog.authorColorTag,
                        creationTimeSeconds = creationTimeSeconds,
                        isSuspect = true,
                        timeStamp = 0
                    )
                    blogsDao.insert(newSuspect)
                }
            }
        }

        val recentBlogIDs = recentBlogs.map { it.id }

        //remove from lost
        blogsDao.getLost().forEach { blog ->
            if(isOldLost(blog.creationTimeSeconds) || blog.id in recentBlogIDs){
                blogsDao.remove(blog)
            }
        }

        //suspect become lost
        suspects.forEach { blog ->
            if(blog.id !in recentBlogIDs){
                blogsDao.update(blog.copy(
                    isSuspect = false,
                    timeStamp = currentTimeSeconds
                ))
            }
        }

        return Result.success()
    }

}