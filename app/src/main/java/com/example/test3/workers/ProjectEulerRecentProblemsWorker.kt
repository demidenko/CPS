package com.example.test3.workers

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.test3.*
import com.example.test3.news.NewsFeed
import com.example.test3.news.settingsNews
import com.example.test3.utils.CPSDataStore
import com.example.test3.utils.ProjectEulerAPI
import com.example.test3.utils.getColorFromResource

class ProjectEulerRecentProblemsWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        suspend fun isEnabled(context: Context): Boolean = context.settingsNews.getNewsFeedEnabled(NewsFeed.PROJECT_EULER_RECENT)
    }

    private val dataStore by lazy { ProjectEulerRecentProblemsWorkerDataStore(context) }

    override suspend fun doWork(): Result {

        if(!isEnabled(context)){
            WorkersCenter.stopWorker(context, WorkersNames.project_euler_recent_problems)
            return Result.success()
        }

        val s = ProjectEulerAPI.getRecentProblemsPage() ?: return Result.retry()

        val lastViewedProblemID = dataStore.lastRecentProblemID()

        val newProblems = mutableListOf<Pair<Int,String>>()

        var i = 0
        while(true){
            i = s.indexOf("<td class=\"id_column\">", i+1)
            if(i==-1) break

            val problemID = s.substring(s.indexOf('>',i)+1, s.indexOf('<',i+1)).toInt()
            if(problemID == lastViewedProblemID) break

            i = s.indexOf("<td>", i+1)
            val j = s.indexOf("</td>", i)

            i = s.indexOf("</a",i)
            if(i == -1 || i > j) continue
            val problemName = s.substring(s.lastIndexOf('>',i)+1, i)

            newProblems.add(Pair(problemID, problemName))
        }

        if(newProblems.isEmpty()) return Result.success()

        if(lastViewedProblemID != null){
            newProblems.forEach { (id, name) ->
                notificationBuildAndNotify(
                    context,
                    NotificationChannels.project_euler_problems,
                    NotificationIDs.makeProjectEulerRecentProblemNotificationID(id)
                ) {
                    setSubText("Project Euler • New problem published!")
                    setContentTitle("Problem $id")
                    setBigContent(name)
                    setSmallIcon(R.drawable.ic_logo_projecteuler)
                    setColor(getColorFromResource(context, R.color.project_euler_main))
                    setShowWhen(true)
                    setAutoCancel(true)
                    setContentIntent(makePendingIntentOpenURL("https://projecteuler.net/problem=$id", context))
                }
            }
        }

        val firstID = newProblems.first().first
        if(firstID != lastViewedProblemID) {
            dataStore.lastRecentProblemID(firstID)
        }

        return Result.success()
    }

    class ProjectEulerRecentProblemsWorkerDataStore(context: Context): CPSDataStore(context.pe_recent_worker_dataStore) {
        companion object {
            private val Context.pe_recent_worker_dataStore by preferencesDataStore("worker_project_euler_recent")
        }

        val lastRecentProblemID = ItemNullable(intPreferencesKey("last_recent_problem_id"))

    }

}