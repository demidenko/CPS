package com.example.test3.job_services

import android.content.Context
import com.example.test3.BottomProgressInfo
import com.example.test3.MainActivity
import com.example.test3.NewsFragment
import com.example.test3.account_manager.STATUS
import com.example.test3.news.SettingsNewsFragment
import com.example.test3.room.LostBlogEntry
import com.example.test3.room.getLostBlogsDao
import com.example.test3.utils.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class CodeforcesNewsLostRecentJobService : CoroutineJobService(){
    companion object {

        suspend fun updateInfo(context: Context) {
            val blogsDao = getLostBlogsDao(context)

            val blogEntries = blogsDao.getLost()
                .toTypedArray()

            val progressInfo = BottomProgressInfo(blogEntries.size, "update info of lost", context as MainActivity)

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

            val blogIDsToRemove = mutableSetOf<Int>()
            blogEntries.forEachIndexed { index, blogEntry ->
                CodeforcesAPI.getBlogEntry(blogEntry.id,locale)?.let { response ->
                    if(response.status == CodeforcesAPIStatus.FAILED && response.isBlogNotFound(blogEntry.id)){
                        blogIDsToRemove.add(blogEntry.id)
                    } else {
                        if(response.status == CodeforcesAPIStatus.OK) response.result?.let { freshBlogEntry ->
                            val title = freshBlogEntry.title.removeSurrounding("<p>", "</p>")
                            blogEntries[index] = blogEntry.copy(
                                authorHandle = freshBlogEntry.authorHandle,
                                title = fromHTML(title).toString()
                            )
                        }
                    }
                    progressInfo.increment()
                }
            }

            blogsDao.insert(
                blogEntries.filterNot { blogIDsToRemove.contains(it.id) }
            )

            progressInfo.finish()
        }

        suspend fun isEnabled(context: Context): Boolean = SettingsNewsFragment.getSettings(context).getLostEnabled()
    }

    override suspend fun makeJobs(): List<Job> {
        if (isEnabled(this)) return listOf( launch { parseRecent() })
        else{
            JobServicesCenter.stopJobService(this, JobServiceIDs.codeforces_news_lost_recent)
            return emptyList()
        }
    }

    private suspend fun parseRecent(){


        val recentBlogs = CodeforcesUtils.parseRecentActionsPage(
            CodeforcesAPI.getPageSource("recent-actions", NewsFragment.getCodeforcesContentLanguage(this)) ?: return
        ).first.let { list ->
            val authors = CodeforcesUtils.getUsersInfo(list.map { blog -> blog.authorHandle })
            list.map { blog ->
                authors[blog.authorHandle]?.takeIf { it.status==STATUS.OK }
                ?.let {
                    blog.copy(authorColorTag = CodeforcesUtils.getTagByRating(it.rating))
                } ?: blog
            }
        }

        if(recentBlogs.isEmpty()) return

        val currentTimeSeconds = getCurrentTimeSeconds()
        val blogsDao = getLostBlogsDao(this)

        val suspects = blogsDao.getSuspects()
            .filter { blog ->
                TimeUnit.SECONDS.toHours(currentTimeSeconds - blog.creationTimeSeconds) < 24
            }.toMutableList()

        val highRated = SettingsNewsFragment.getSettings(this).getLostMinRating()
        val newSuspects = mutableListOf<LostBlogEntry>()
        recentBlogs.forEach { blog ->
            if(blog.authorColorTag>=highRated && suspects.none { it.id == blog.id }){
                val creationTimeSeconds = CodeforcesUtils.getBlogCreationTimeSeconds(blog.id)
                if(TimeUnit.SECONDS.toHours(currentTimeSeconds - creationTimeSeconds) < 24){
                    newSuspects.add(LostBlogEntry(
                        id = blog.id,
                        title = blog.title,
                        authorHandle = blog.authorHandle,
                        authorColorTag = blog.authorColorTag,
                        creationTimeSeconds = creationTimeSeconds,
                        isSuspect = true,
                        timeStamp = 0
                    ))
                }
            }
        }

        suspects.addAll(newSuspects)

        val recentBlogIDs = recentBlogs.map { it.id }
        val lost = blogsDao.getLost()
            .filter { blog ->
                TimeUnit.SECONDS.toDays(currentTimeSeconds - blog.creationTimeSeconds) <= 7
                    &&
                blog.id !in recentBlogIDs
            }.toMutableList()

        blogsDao.insert(
            suspects.filter { blog ->
                if (blog.id !in recentBlogIDs) {
                    lost.add(blog.copy(
                        isSuspect = false,
                        timeStamp = currentTimeSeconds
                    ))
                    false
                } else {
                    true
                }
            }
        )

        blogsDao.insert(lost)
    }

}