package com.example.test3.contest_watch

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.test3.NotificationChannels
import com.example.test3.NotificationIDs
import com.example.test3.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.util.concurrent.TimeUnit

class CodeforcesContestWatchService: Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
    }

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }


    override fun onCreate() {
        super.onCreate()
        println("service onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent!!.action

        when(action){
            ACTION_START -> {
                val handle = intent.getStringExtra("handle")!!
                val contestID = intent.getIntExtra("contestID", -1)
                stop()
                start(handle, contestID, NotificationCompat.Builder(this, NotificationChannels.codeforces_contest_watcher).apply {
                    setSmallIcon(R.drawable.ic_contest)
                    setSubText(handle)
                    setShowWhen(false)
                    setNotificationSilent()
                    setStyle(NotificationCompat.DecoratedCustomViewStyle())
                })
            }
            ACTION_STOP -> {
                stop()
                stopForeground(true)
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }


    private var watcher: CodeforcesContestWatcher? = null

    private fun stop(){
        watcher?.stop()
        watcher = null
    }

    private fun start(handle: String, contestID: Int, notification: NotificationCompat.Builder){
        startForeground(NotificationIDs.codeforces_contest_watcher, notification.build())

        val rview_small = RemoteViews(packageName, R.layout.cf_watcher_notification_small)
        val rview_big = RemoteViews(packageName, R.layout.cf_watcher_notification_big)
        val rviews = arrayOf(rview_small, rview_big)


        watcher = CodeforcesContestWatcher(
            handle,
            contestID,
            scope
        ).apply {
            addCodeforcesContestWatchListener(object : CodeforcesContestWatchListener(){
                var contestType = CodeforcesContestType.UNDEFINED
                var changes = false
                var contestantRank = ""
                var contestantPoints = ""
                var participationType = CodeforcesParticipationType.NOTPARTICIPATED

                override fun onSetContestNameAndType(contestName: String, contestType: CodeforcesContestType) {
                    changes = true
                    notification.setSubText("$contestName • $handle")
                    this.contestType = contestType
                }

                private fun str_pts(p: Double, isTotal: Boolean = false): String {
                    if(isTotal){

                    }else{
                        if(p == 0.0) return ""
                        if(contestType == CodeforcesContestType.ICPC) return "+"
                    }

                    return p.toString().removeSuffix(".0")
                }

                private val rviewsByProblem = mutableMapOf<String,RemoteViews>()
                override suspend fun onSetProblemNames(problemNames: Array<String>) {
                    changes = true
                    rview_big.removeAllViews(R.id.cf_watcher_notification_table_tasks)
                    rviewsByProblem.clear()
                    problemNames.forEach { problemName ->
                        val r = RemoteViews(packageName, R.layout.cf_watcher_notification_table_column)
                        r.setTextViewText(R.id.cf_watcher_notification_table_column_header, problemName)
                        r.setTextViewText(R.id.cf_watcher_notification_table_column_cell, "")
                        rview_big.addView(R.id.cf_watcher_notification_table_tasks, r)
                        rviewsByProblem[problemName] = r
                    }
                }

                override fun onSetContestPhase(phaseCodeforces: CodeforcesContestPhase) {
                    changes = true
                    rviews.forEach { it.setChronometer(R.id.cf_watcher_notification_progress, SystemClock.elapsedRealtime(), null, false) }
                    rviews.forEach { it.setTextViewText(R.id.cf_watcher_notification_progress, "") }
                    rviews.forEach { it.setTextViewText(R.id.cf_watcher_notification_phase, phaseCodeforces.name) }
                }

                override fun onSetRemainingTime(timeSeconds: Long) {
                    changes = true
                    rviews.forEach { it.setChronometer(R.id.cf_watcher_notification_progress, SystemClock.elapsedRealtime() + TimeUnit.SECONDS.toMillis(timeSeconds), null, true) }
                }

                override fun onSetSysTestProgress(percents: Int) {
                    changes = true
                    rviews.forEach { it.setTextViewText(R.id.cf_watcher_notification_progress, "$percents%") }
                }

                override fun onSetContestantRank(rank: Int) {
                    changes = true
                    contestantRank =
                        if(participationType == CodeforcesParticipationType.CONTESTANT) "$rank"
                        else "*$rank"
                    rview_big.setTextViewText(R.id.cf_watcher_notification_rank, contestantRank)
                }

                override fun onSetContestantPoints(points: Double) {
                    changes = true
                    contestantPoints = str_pts(points, true)
                    rview_big.setTextViewText(R.id.cf_watcher_notification_points, contestantPoints)
                }

                override fun onSetParticipationType(type: CodeforcesParticipationType) {
                    changes = true
                    participationType = type
                }

                override fun onSetProblemStatus(problem: String, status: String, points: Double) {
                    changes = true
                    rviewsByProblem[problem]?.run{
                        setTextViewText(R.id.cf_watcher_notification_table_column_cell, str_pts(points))
                        if(status == "FINAL"){
                            setTextColor(R.id.cf_watcher_notification_table_column_cell, resources.getColor(R.color.blog_rating_positive, null))
                        }
                    }}

                override fun commit() {
                    if(!changes) return
                    changes = false


                    notification.setCustomContentView(rview_small.apply {
                        setTextViewText(R.id.cf_watcher_notification_rank,
                            if(participationType == CodeforcesParticipationType.NOTPARTICIPATED) "not participated"
                            else "rank: $contestantRank • points: $contestantPoints"
                        )
                    })

                    notification.setCustomBigContentView(
                        if (participationType == CodeforcesParticipationType.NOTPARTICIPATED) null
                        else rview_big
                    )


                    notificationManager.notify(NotificationIDs.codeforces_contest_watcher, notification.build())
                }
            })
            start()
        }
    }
}