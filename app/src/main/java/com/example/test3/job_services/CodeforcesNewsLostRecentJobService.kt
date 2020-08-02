package com.example.test3.job_services

import android.content.Context
import androidx.preference.PreferenceManager
import com.example.test3.CodeforcesNewsItemsRecentAdapter
import com.example.test3.R
import com.example.test3.utils.CodeforcesAPI
import com.example.test3.utils.CodeforcesUtils
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashSet


@JsonClass(generateAdapter = true)
data class BlogInfo(
    val id: Int,
    val title: String,
    val author: String,
    val authorColorTag: String,
    val creationTime: Long
){
    companion object{
        val jsonAdapter: JsonAdapter<BlogInfo> = Moshi.Builder().build().adapter(BlogInfo::class.java)
    }
}


class CodeforcesNewsLostRecentJobService : CoroutineJobService(){
    companion object {
        private val CF_LOST_SUSPECTS = "cf_lost_suspects.txt"
        val CF_LOST = "cf_lost.txt"

        fun getBlogs(context: Context, file_name: String): List<BlogInfo> {
            try {
                val res = mutableListOf<BlogInfo>()
                val sc = Scanner(context.openFileInput(file_name))
                while(sc.hasNextLine()){
                    val str = sc.nextLine()
                    res.add(BlogInfo.jsonAdapter.fromJson(str)!!)
                }
                return res
            }catch (e: FileNotFoundException){
                return emptyList()
            }
        }

        private fun saveBlogs(context: Context, file_name: String, blogs: Collection<BlogInfo>){
            println("save $file_name: $blogs")
            val out = PrintWriter(context.openFileOutput(file_name, Context.MODE_PRIVATE))
            blogs.forEach {
                val str = BlogInfo.jsonAdapter.toJson(it)
                out.println(str)
            }
            out.flush()
            out.close()
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

        val currentTime = System.currentTimeMillis()

        val suspects = getBlogs(this, CF_LOST_SUSPECTS)
            .filter {
                currentTime - it.creationTime <= TimeUnit.DAYS.toMillis(1)
            }.toHashSet()

        val newSuspects = mutableListOf<BlogInfo>()
        recentBlogs.forEach { blog ->
            val blogID = blog.blogID.toInt()
            if(blog.authorColorTag in highRated && suspects.find { it.id == blogID } == null){
                val creationTime = CodeforcesUtils.getBlogCreationTimeMillis(blog.blogID)
                if(currentTime - creationTime <= TimeUnit.DAYS.toMillis(1)){
                    newSuspects.add(BlogInfo(
                        id = blogID,
                        creationTime = creationTime,
                        title = blog.title,
                        author = blog.author,
                        authorColorTag = blog.authorColorTag
                    ))
                }
            }
        }

        suspects.addAll(newSuspects)
        println("suspects = $suspects")

        val recentBlogIDs = recentBlogs.mapTo(HashSet()){ it.blogID.toInt() }
        val lost = getBlogs(this, CF_LOST)
            .filter { blog ->
                currentTime - blog.creationTime <= TimeUnit.DAYS.toMillis(7)
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