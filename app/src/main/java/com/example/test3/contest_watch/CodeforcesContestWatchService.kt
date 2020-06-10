package com.example.test3.contest_watch

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.test3.NotificationChannels
import com.example.test3.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class CodeforcesContestWatchService: Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    val notificationID = 1

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private lateinit var notificationManager: NotificationManager


    override fun onCreate() {
        super.onCreate()
        println("service onCreate")

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent!!.action

        when(action){
            "start" -> {
                val handle = intent.getStringExtra("handle")!!
                val contestID = intent.getIntExtra("contestID", -1)
                println("service onStartCommand $handle $contestID")
                stop()
                start(handle, contestID, NotificationCompat.Builder(this, NotificationChannels.codeforces_contest_watcher).apply {
                    setSmallIcon(R.drawable.ic_news)
                    setSubText(handle)
                    setShowWhen(false)
                    setNotificationSilent()
                })
            }
            "stop" -> {
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
                var participationType = CodeforcesContestWatcher.ParticipationType.NOTPARTICIPATED

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

                override fun onSetParticipationType(type: CodeforcesContestWatcher.ParticipationType) {
                    changes = true
                    participationType = type
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
                    notification.setContentText(
                        when(participationType){
                            CodeforcesContestWatcher.ParticipationType.NOTPARTICIPATED -> "not participated"
                            CodeforcesContestWatcher.ParticipationType.OFFICIAL -> "rank: $contestantRank | points: $contestantPoints"
                            CodeforcesContestWatcher.ParticipationType.UNOFFICIAL -> "rank: *$contestantRank | points: $contestantPoints"
                        }
                    )

                    notificationManager.notify(notificationID, notification.build())
                }
            })
            start()
        }
    }
}