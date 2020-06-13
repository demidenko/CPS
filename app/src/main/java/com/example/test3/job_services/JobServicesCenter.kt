package com.example.test3.job_services

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import java.util.concurrent.TimeUnit

class JobServiceIDs {
    companion object{
        private var id = 0
        val codeforces_lost_recent_news = ++id
    }
}

class JobServicesCenter {
    companion object {
        private fun makeSchedule(
            context: Context,
            id: Int,
            c: Class<*>?,
            millis: Long,
            network_type: Int
        ) {
            val builder = JobInfo.Builder(id, ComponentName(context, c!!)).apply {
                setPeriodic(millis)
                setRequiredNetworkType(network_type)
            }
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(builder.build())
        }

        fun getRunningJobServices(context: Context): List<JobInfo> {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            return scheduler.allPendingJobs
        }

        fun startJobServices(context: Context){
            val jobs = getRunningJobServices(context)
            if(jobs.none { it.id == JobServiceIDs.codeforces_lost_recent_news }) startCodeforcesNewsLostRecentJobService(context)
        }

        private fun startCodeforcesNewsLostRecentJobService(context: Context){
            makeSchedule(
                context,
                JobServiceIDs.codeforces_lost_recent_news,
                CodeforcesNewsLostRecentJobService::class.java,
                TimeUnit.MINUTES.toMillis(60),
                JobInfo.NETWORK_TYPE_UNMETERED
            )
            Toast.makeText(context, "lost scheduled", Toast.LENGTH_SHORT).show()
        }
    }
}

