package com.example.test3

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashSet
import kotlin.coroutines.CoroutineContext

class CodeforcesNewsLostRecentJobService : JobService(), CoroutineScope{
    override val coroutineContext: CoroutineContext = Job() + Dispatchers.Main

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        launch {
            job()
            jobFinished(params, false)
        }
        return true
    }

    private val highRated = arrayListOf("user-orange", "user-red", "user-legendary")
    suspend fun job(){
        val recentBlogs = CodeforcesNewsItemsRecentAdapter.parsePage(readURLData("https://codeforces.com/recent-actions?locale=ru") ?: return)
        if(recentBlogs.isEmpty()) return

        val currentTime = System.currentTimeMillis()

        val suspects = getSuspects()
            .filter { (id,time) ->
                currentTime - time <= TimeUnit.DAYS.toMillis(1)
            }
            .toHashSet()

        val newSuspects = mutableListOf<Pair<Int,Long>>()
        recentBlogs.forEach { blog ->
            val blogID = blog.blogID.toInt()
            if(blog.authorColorTag in highRated && suspects.find { it.first == blogID } == null){
                val creationTime = CodeforcesUtils.getBlogCreationTimeMillis(blog.blogID)
                if(currentTime - creationTime <= TimeUnit.DAYS.toMillis(1)){
                    newSuspects.add(Pair(blogID,creationTime))
                }
            }
        }

        suspects.addAll(newSuspects)
        println("suspects = $suspects")

        val recentBlogIDs = recentBlogs.mapTo(HashSet()){ it.blogID.toInt() }
        val newLost = mutableListOf<Int>()
        saveSuspects(
            suspects
            .filter { (id, time) ->
                if(id !in recentBlogIDs){
                    newLost.add(id)
                    false
                }else{
                    true
                }
            }
        )

        if(newLost.isNotEmpty()){
            makeSimpleNotification(
                this,
                NotificationIDs.test,
                "lost detected",
                newLost.joinToString(),
                false
            )
        }

    }

    private val CF_LOST_SUSPECTS = "cf_lost_suspects.txt"

    private fun getSuspects(): List<Pair<Int,Long>> {
        try {
            val input = Scanner(openFileInput(CF_LOST_SUSPECTS))
            val res = mutableListOf<Pair<Int, Long>>()
            while (input.hasNext()) {
                val blogID = input.nextInt()
                val creationTime = input.nextLong()
                res.add(Pair(blogID, creationTime))
            }
            return res
        }catch (e: FileNotFoundException){
            return emptyList()
        }
    }

    private fun saveSuspects(suspects: Collection<Pair<Int,Long>>){
        val out = PrintWriter(openFileOutput(CF_LOST_SUSPECTS, Context.MODE_PRIVATE))
        suspects.forEach { (blogID, creationTime) ->
            out.print(blogID)
            out.print(' ')
            out.println(creationTime)
        }
        out.flush()
        out.close()
    }
}