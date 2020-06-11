package com.example.test3

import android.app.job.JobParameters
import android.app.job.JobService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
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
        val blogs = CodeforcesNewsItemsRecentAdapter.parsePage(readURLData("https://codeforces.com/recent-actions?locale=ru") ?: return)

        val currentTime = System.currentTimeMillis()
        val suspects = blogs
            .filter { it.authorColorTag in highRated }
            .filter {
                val creationTime = CodeforcesUtils.getBlogCreationTimeSeconds(it.blogID) * 1000L
                currentTime - creationTime < TimeUnit.DAYS.toMillis(1)
            }

        val handles = suspects.map { it.author }

        makeSimpleNotification(
            this,
            NotificationIDs.test,
            "recent job test: ${handles.size}",
            handles.joinToString(),
            handles.isEmpty()
        )
    }
}