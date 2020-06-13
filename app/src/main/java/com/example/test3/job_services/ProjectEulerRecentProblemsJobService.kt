package com.example.test3.job_services

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.test3.NotificationChannels
import com.example.test3.R
import com.example.test3.readURLData

class ProjectEulerRecentProblemsJobService: CoroutineJobService() {

    override suspend fun doJob() {
        val s = readURLData("https://projecteuler.net/recent") ?: return

        val prefs = getSharedPreferences("test", Context.MODE_PRIVATE)
        val lastViewedProblemID = prefs.getInt("pe_problem_id", 0)

        val newProblems = mutableListOf<Pair<Int,String>>()

        var i = 0
        while(true){
            i = s.indexOf("<td class=\"id_column\">", i+1)
            if(i==-1) break

            val problemID = s.substring(s.indexOf('>',i)+1, s.indexOf('<',i+1)).toInt()

            i = s.indexOf("</a",i)
            val problemName = s.substring(s.lastIndexOf('>',i)+1, i)

            if(problemID == lastViewedProblemID) break

            newProblems.add(Pair(problemID, problemName))
        }

        if(newProblems.isEmpty()) return

        if(lastViewedProblemID != 0){
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            newProblems.forEach { (id, name) ->
                val n = NotificationCompat.Builder(this, NotificationChannels.project_euler_problems).apply {
                    setSmallIcon(R.drawable.ic_news)
                    setSubText("Project Euler • New problem published!")
                    setShowWhen(true)
                    setContentTitle("Problem $id")
                    setContentText(name)
                }
                notificationManager.notify(id, n.build())
            }
        }

        with(prefs.edit()){
            val firstID = newProblems.first().first
            putInt("pe_problem_id", firstID)
            apply()
        }
    }

}