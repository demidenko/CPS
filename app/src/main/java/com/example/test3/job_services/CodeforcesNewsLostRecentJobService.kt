package com.example.test3.job_services

import android.content.Context
import androidx.preference.PreferenceManager
import com.example.test3.CodeforcesNewsItemsRecentAdapter
import com.example.test3.R
import com.example.test3.utils.*
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashSet


class CodeforcesNewsLostRecentJobService : CoroutineJobService(){
    companion object {
        private val CF_LOST_SUSPECTS = "cf_lost_suspects.txt"
        val CF_LOST = "cf_lost.txt"

        private fun getSavedBlogs(context: Context, file_name: String): List<CodeforcesBlogEntry> {
            try {
                val res = mutableListOf<CodeforcesBlogEntry>()
                val sc = Scanner(context.openFileInput(file_name))
                while(sc.hasNextLine()){
                    val str = sc.nextLine()
                    res.add(CodeforcesBlogEntry.jsonAdapter.fromJson(str)!!)
                }
                return res
            }catch (e: FileNotFoundException){
                return emptyList()
            }
        }

        fun getSavedLostBlogs(context: Context) = getSavedBlogs(context, CF_LOST)
        fun getSavedSuspectBlogs(context: Context) = getSavedBlogs(context, CF_LOST_SUSPECTS)

        fun saveBlogs(context: Context, file_name: String, blogs: Collection<CodeforcesBlogEntry>){
            println("save $file_name: $blogs")
            val out = PrintWriter(context.openFileOutput(file_name, Context.MODE_PRIVATE))
            blogs.forEach {
                val str = CodeforcesBlogEntry.jsonAdapter.toJson(it)
                out.println(str)
            }
            out.flush()
            out.close()
        }

        suspend fun updateInfo(context: Context) {
            val blogEntries = getSavedLostBlogs(context)
                .toTypedArray()

            CodeforcesAPI.getUsers(blogEntries.map { it.authorHandle })?.result?.let { users ->
                for(i in blogEntries.indices) {
                    val blogEntry = blogEntries[i]
                    users.find { it.handle == blogEntry.authorHandle }?.let { user ->
                        blogEntries[i] = blogEntry.copy(
                            authorColorTag = CodeforcesUtils.getTagByRating(user.rating)
                        )
                    }
                }
            }

            val blogIDsToRemove = mutableSetOf<Int>()
            blogEntries.forEachIndexed { index, blogEntry ->
                CodeforcesAPI.getBlogEntry(blogEntry.id)?.let { response ->
                    if(response.status == CodeforcesAPIStatus.FAILED && response.comment == "blogEntryId: Blog entry with id ${blogEntry.id} not found"){
                        blogIDsToRemove.add(blogEntry.id)
                    } else {
                        if(response.status == CodeforcesAPIStatus.OK) response.result?.let { freshBlogEntry ->
                            val title = freshBlogEntry.title
                                .removePrefix("<p>")
                                .removeSuffix("</p>")
                            blogEntries[index] = blogEntry.copy(title = fromHTML(title))
                        } else {
                            //god bless kotlin
                        }
                    }
                }
            }

            saveBlogs(
                context,
                CF_LOST,
                blogEntries.filterNot { blogIDsToRemove.contains(it.id) }
            )
        }
    }

    override suspend fun makeJobs() = arrayListOf(launch { parseRecent() })

    private val highRated = arrayListOf("user-orange", "user-red", "user-legendary")
    private suspend fun parseRecent(){
        val enabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.news_codeforces_lost_enabled), false)
        if(!enabled) return


        val recentBlogs = CodeforcesNewsItemsRecentAdapter.parsePage(
            CodeforcesAPI.getPageSource("recent-actions", "ru") ?: return
        )
        if(recentBlogs.isEmpty()) return

        val currentTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())

        val suspects = getSavedSuspectBlogs(this)
            .filter {
                TimeUnit.SECONDS.toDays(currentTimeSeconds - it.creationTimeSeconds) <= 1
            }.toHashSet()

        val newSuspects = mutableListOf<CodeforcesBlogEntry>()
        recentBlogs.forEach { blog ->
            val blogID = blog.blogID.toInt()
            if(blog.authorColorTag in highRated && suspects.find { it.id == blogID } == null){
                val creationTimeSeconds = CodeforcesUtils.getBlogCreationTimeSeconds(blog.blogID)
                if(TimeUnit.SECONDS.toDays(currentTimeSeconds - creationTimeSeconds) <= 1){
                    newSuspects.add(CodeforcesBlogEntry(
                        id = blogID,
                        creationTimeSeconds = creationTimeSeconds,
                        title = blog.title,
                        authorHandle = blog.author,
                        authorColorTag = blog.authorColorTag
                    ))
                }
            }
        }

        suspects.addAll(newSuspects)
        println("suspects = $suspects")

        val recentBlogIDs = recentBlogs.mapTo(HashSet()){ it.blogID.toInt() }
        val lost = getSavedLostBlogs(this)
            .filter { blog ->
                TimeUnit.SECONDS.toDays(currentTimeSeconds - blog.creationTimeSeconds) <= 7
                    &&
                blog.id !in recentBlogIDs
            }.toHashSet()

        saveBlogs(this, CF_LOST_SUSPECTS,
            suspects.filter {
                if (it.id !in recentBlogIDs) {
                    lost.add(it)
                    false
                } else {
                    true
                }
            }
        )

        saveBlogs(this, CF_LOST, lost)
    }

}