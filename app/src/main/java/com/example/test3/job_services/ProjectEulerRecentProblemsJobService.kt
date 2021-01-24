package com.example.test3.job_services

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.test3.*
import com.example.test3.news.SettingsNewsFragment
import com.example.test3.utils.ProjectEulerAPI
import com.example.test3.utils.SettingsDataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class ProjectEulerRecentProblemsJobService: CoroutineJobService() {

    companion object {
        suspend fun isEnabled(context: Context): Boolean =
            SettingsNewsFragment.getSettings(context).getNewsFeedEnabled(SettingsNewsFragment.NewsFeed.PROJECT_EULER_RECENT)
    }

    override suspend fun makeJobs(): List<Job> {
        if (isEnabled(this)) return listOf( launch { parseRecentProblems() } )
        else{
            JobServicesCenter.stopJobService(this, JobServiceIDs.project_euler_recent_problems)
            return emptyList()
        }
    }

    private val dataStore by lazy { ProjectEulerRecentProblemsJobServiceDataStore(this) }

    private suspend fun parseRecentProblems() {
        val s = ProjectEulerAPI.getRecentProblemsPage() ?: return

        val lastViewedProblemID = dataStore.getLastRecentProblemID()

        val newProblems = mutableListOf<Pair<Int,String>>()

        var i = 0
        while(true){
            i = s.indexOf("<td class=\"id_column\">", i+1)
            if(i==-1) break

            val problemID = s.substring(s.indexOf('>',i)+1, s.indexOf('<',i+1)).toInt()
            if(problemID == lastViewedProblemID) break

            i = s.indexOf("</a",i)
            val problemName = s.substring(s.lastIndexOf('>',i)+1, i)

            newProblems.add(Pair(problemID, problemName))
        }

        if(newProblems.isEmpty()) return

        if(lastViewedProblemID != 0){
            newProblems.forEach { (id, name) ->
                val n = notificationBuilder(this, NotificationChannels.project_euler_problems).apply {
                    setSubText("Project Euler • New problem published!")
                    setContentTitle("Problem $id")
                    setBigContent(name)
                    setSmallIcon(R.drawable.ic_projecteuler_logo)
                    setColor(NotificationColors.project_euler_main)
                    setShowWhen(true)
                    setAutoCancel(true)
                    setContentIntent(makePendingIntentOpenURL("https://projecteuler.net/problem=$id", this@ProjectEulerRecentProblemsJobService))
                }
                NotificationManagerCompat.from(this).notify(NotificationIDs.makeProjectEulerRecentProblemNotificationID(id), n.build())
            }
        }

        val firstID = newProblems.first().first
        if(firstID != lastViewedProblemID) {
            dataStore.setLastRecentProblemID(firstID)
        }
    }

    class ProjectEulerRecentProblemsJobServiceDataStore(context: Context): SettingsDataStore(context, "jobservice_project_euler_recent") {

        companion object {
            private val KEY_LAST_RECENT_PROBLEM_ID = intPreferencesKey("last_recent_problem_id")
        }

        suspend fun getLastRecentProblemID() = dataStore.data.first()[KEY_LAST_RECENT_PROBLEM_ID] ?: 0
        suspend fun setLastRecentProblemID(problemID: Int) {
            dataStore.edit { it[KEY_LAST_RECENT_PROBLEM_ID] = problemID }
        }
    }

}