package com.example.test3.job_services

import android.content.Context
import androidx.preference.PreferenceManager
import com.example.test3.BottomProgressInfo
import com.example.test3.MainActivity
import com.example.test3.NewsFragment
import com.example.test3.R
import com.example.test3.account_manager.STATUS
import com.example.test3.utils.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.TimeUnit


class CodeforcesNewsLostRecentJobService : CoroutineJobService(){
    companion object {
        private const val CF_LOST_SUSPECTS = "cf_lost_suspects.txt"
        private const val CF_LOST = "cf_lost.txt"

        private fun getSavedBlogs(context: Context, file_name: String): List<CodeforcesBlogEntry> {
            try {
                val res = mutableListOf<CodeforcesBlogEntry>()
                val sc = Scanner(context.openFileInput(file_name))
                while(sc.hasNextLine()){
                    val str = sc.nextLine()
                    res.add(jsonCPS.decodeFromString(str))
                }
                return res
            }catch (e: FileNotFoundException){
                return emptyList()
            }
        }

        fun getSavedLostBlogs(context: Context) = getSavedBlogs(context, CF_LOST)
        fun getSavedSuspectBlogs(context: Context) = getSavedBlogs(context, CF_LOST_SUSPECTS)

        fun saveBlogs(context: Context, file_name: String, blogs: Collection<CodeforcesBlogEntry>){
            val out = PrintWriter(context.openFileOutput(file_name, Context.MODE_PRIVATE))
            blogs.forEach {
                val str = jsonCPS.encodeToString(it)
                out.println(str)
            }
            out.flush()
            out.close()
        }

        suspend fun updateInfo(context: Context) {
            val blogEntries = getSavedLostBlogs(context)
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

            saveBlogs(
                context,
                CF_LOST,
                blogEntries.filterNot { blogIDsToRemove.contains(it.id) }
            )

            progressInfo.finish()
        }

        fun isEnabled(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.news_codeforces_lost_enabled), false)
        }
    }

    override suspend fun makeJobs(): ArrayList<Job> {
        if (isEnabled(this)) return arrayListOf( launch { parseRecent() })
        else{
            JobServicesCenter.stopJobService(this, JobServiceIDs.codeforces_news_lost_recent)
            return arrayListOf()
        }
    }

    private val highRated = arrayListOf("user-orange", "user-red", "user-legendary")
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

        val currentTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())

        val suspects = getSavedSuspectBlogs(this)
            .filter { blog ->
                TimeUnit.SECONDS.toHours(currentTimeSeconds - blog.creationTimeSeconds) < 24
            }.toMutableList()

        val newSuspects = mutableListOf<CodeforcesBlogEntry>()
        recentBlogs.forEach { blog ->
            if(blog.authorColorTag in highRated && suspects.find { it.id == blog.id } == null){
                val creationTimeSeconds = CodeforcesUtils.getBlogCreationTimeSeconds(blog.id)
                if(TimeUnit.SECONDS.toHours(currentTimeSeconds - creationTimeSeconds) < 24){
                    newSuspects.add(blog.copy(creationTimeSeconds = creationTimeSeconds))
                }
            }
        }

        suspects.addAll(newSuspects)

        val recentBlogIDs = recentBlogs.map { it.id }
        val lost = getSavedLostBlogs(this)
            .filter { blog ->
                TimeUnit.SECONDS.toDays(currentTimeSeconds - blog.creationTimeSeconds) <= 7
                    &&
                blog.id !in recentBlogIDs
            }.toMutableList()

        saveBlogs(this, CF_LOST_SUSPECTS,
            suspects.filter { blog ->
                if (blog.id !in recentBlogIDs) {
                    lost.add(blog)
                    false
                } else {
                    true
                }
            }
        )

        saveBlogs(this, CF_LOST, lost)
    }

}