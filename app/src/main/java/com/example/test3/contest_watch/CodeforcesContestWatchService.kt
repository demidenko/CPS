package com.example.test3.contest_watch

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.test3.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class CodeforcesContestWatchService: Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    val notificationID = 1

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private lateinit var notificationManager: NotificationManager

    private val notification = NotificationCompat.Builder(this, "test").apply {
        setSmallIcon(R.drawable.ic_news)
        setShowWhen(false)
        setNotificationSilent()
    }

    override fun onCreate() {
        super.onCreate()
        println("service onCreate")

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Toast.makeText(this, "created $watcher", Toast.LENGTH_SHORT).show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent!!.action
        Toast.makeText(this, "on start $action $intent", Toast.LENGTH_SHORT).show()

        when(action){
            "start" -> {
                val handle = intent.getStringExtra("handle")!!
                val contestID = intent.getIntExtra("contestID", -1)
                println("service onStartCommand $handle $contestID")
                stop()
                start(handle, contestID)
            }
            "stop" -> {
                stop()
                stopForeground(true)
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Toast.makeText(this, "destroy", Toast.LENGTH_SHORT).show()

        super.onDestroy()
    }


    private var watcher: CodeforcesContestWatcher? = null

    private fun stop(){
        watcher?.stop()
        watcher = null
    }

    private fun start(handle: String, contestID: Int){
        startForeground(notificationID, notification.build())

        watcher = CodeforcesContestWatcher(
            handle,
            contestID,
            scope
        ).apply {
            addCodeforcesContestWatchListener(object : CodeforcesContestWatchListener(){
                var changes = false
                var contestPhase = CodeforcesContestPhase.UNKNOWN
                var progress = ""
                var contestantRank = -1
                var contestantPoints = 0

                override fun onSetContestName(contestName: String) {
                    changes = true
                    notification.setSubText("$handle • $contestName")
                }

                override suspend fun onSetProblemNames(problemNames: Array<String>) {
                    //TODO("Not yet implemented")
                }

                override fun onSetContestPhase(phaseCodeforces: CodeforcesContestPhase) {
                    changes = true
                    progress = ""
                    contestPhase = phaseCodeforces
                }

                override fun onSetRemainingTime(timeSeconds: Int) {
                    changes = true
                    val SS = timeSeconds % 60
                    val MM = timeSeconds / 60 % 60
                    val HH = timeSeconds / 60 / 60
                    progress = String.format("%02d:%02d:%02d", HH, MM, SS)
                }

                override fun onSetSysTestProgress(percents: Int) {
                    changes = true
                    progress = "$percents%"
                }

                override fun onSetContestantRank(rank: Int) {
                    changes = true
                    contestantRank = rank
                }

                override fun onSetContestantPoints(points: Int) {
                    changes = true
                    contestantPoints = points
                }

                override fun onSetProblemStatus(problem: String, status: String, points: Int) {
                    //TODO("Not yet implemented")
                }

                override fun commit() {
                    if(!changes) return
                    changes = false

                    notification.setContentTitle(
                        contestPhase.name
                        + (if(progress.isEmpty()) "" else " • $progress")
                        //+ (if(contestPhase == CodeforcesContestPhase.FINISHED) " "+System.currentTimeMillis().toString() else "")
                    )

                    notification.setContentText("rank: $contestantRank | points: $contestantPoints")

                    notificationManager.notify(notificationID, notification.build())
                }
            })
            start()
        }
    }
}