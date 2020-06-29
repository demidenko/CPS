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

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }


    override fun onCreate() {
        super.onCreate()
        println("service onCreate")
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
                    setSmallIcon(R.drawable.ic_contest)
                    setSubText(handle)
                    //setShowWhen(false)
                    setNotificationSilent()
                    setStyle(NotificationCompat.DecoratedCustomViewStyle())
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
        startForeground(NotificationIDs.codeforces_contest_watcher, notification.build())

        val rv = RemoteViews(packageName, R.layout.cf_watcher_notification_small)


        watcher = CodeforcesContestWatcher(
            handle,
            contestID,
            scope
        ).apply {
            addCodeforcesContestWatchListener(object : CodeforcesContestWatchListener(){
                var changes = false
                var contestantRank = -1
                var contestantPoints = ""
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
                    rv.setChronometer(R.id.cf_watcher_notification_progress, SystemClock.elapsedRealtime(), null, false)
                    rv.setTextViewText(R.id.cf_watcher_notification_progress, "")
                    rv.setTextViewText(R.id.cf_watcher_notification_phase, phaseCodeforces.name)
                }

                override fun onSetRemainingTime(timeSeconds: Int) {
                    changes = true
                    rv.setChronometer(R.id.cf_watcher_notification_progress, SystemClock.elapsedRealtime() + TimeUnit.SECONDS.toMillis(timeSeconds.toLong()), null, true)
                }

                override fun onSetSysTestProgress(percents: Int) {
                    changes = true
                    rv.setTextViewText(R.id.cf_watcher_notification_progress, "$percents%")
                }

                override fun onSetContestantRank(rank: Int) {
                    changes = true
                    contestantRank = rank
                }

                override fun onSetContestantPoints(points: Double) {
                    changes = true
                    contestantPoints = points.toString().removeSuffix(".0") //genius
                }

                override fun onSetParticipationType(type: CodeforcesContestWatcher.ParticipationType) {
                    changes = true
                    participationType = type
                }

                override fun onSetProblemStatus(problem: String, status: String, points: Double) {
                    //TODO("Not yet implemented")
                }

                override fun commit() {
                    if(!changes) return
                    changes = false

                    rv.setTextViewText(R.id.cf_watcher_notification_rank,
                        when(participationType){
                            CodeforcesContestWatcher.ParticipationType.NOTPARTICIPATED -> "not participated"
                            CodeforcesContestWatcher.ParticipationType.OFFICIAL -> "rank: $contestantRank | points: $contestantPoints"
                            CodeforcesContestWatcher.ParticipationType.UNOFFICIAL -> "rank: *$contestantRank | points: $contestantPoints"
                        }
                    )

                    notification.setCustomContentView(rv)

                    notificationManager.notify(NotificationIDs.codeforces_contest_watcher, notification.build())
                }
            })
            start()
        }
    }
}