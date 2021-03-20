package com.example.test3.contest_watch

import android.app.ActivityManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.test3.*
import com.example.test3.utils.*
import com.example.test3.workers.CodeforcesContestWatchLauncherWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class CodeforcesContestWatchService: Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"

        private var startedWithHandle: String? = null
        private var startedWithContestID: Int? = null

        fun makeStopIntent(context: Context) = Intent(context, CodeforcesContestWatchService::class.java).setAction(ACTION_STOP)

        fun startService(context: Context, handle: String, contestID: Int){
            if(isStarted(context)){
                if(startedWithContestID == contestID && startedWithHandle == handle) return
            }
            val intent = Intent(context, CodeforcesContestWatchService::class.java)
                .setAction(ACTION_START)
                .putExtra("handle", handle)
                .putExtra("contestID", contestID)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        private fun isStarted(context: Context): Boolean {
            val m = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            return m.getRunningServices(Int.MAX_VALUE).any {
                it.service.className == CodeforcesContestWatchService::class.java.name
            }
        }
    }

    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent!!.action){
            ACTION_START -> {
                val handle = intent.getStringExtra("handle")!!
                val contestID = intent.getIntExtra("contestID", -1)
                stopWatcher()
                val notification = notificationBuilder(this, NotificationChannels.codeforces_contest_watcher).apply {
                    setSmallIcon(R.drawable.ic_contest)
                    setSubText(handle)
                    setShowWhen(false)
                    setNotificationSilent()
                    setStyle(NotificationCompat.DecoratedCustomViewStyle())
                }

                val closeIcon = if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O){
                    IconCompat.createWithResource(this, R.drawable.ic_delete_item)
                } else null
                val browseIcon = if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O){
                    IconCompat.createWithResource(this, R.drawable.ic_open_in_browser)
                } else null

                notification.addAction(NotificationCompat.Action(closeIcon, "Close", PendingIntent.getService(this, 0, makeStopIntent(this), 0)))
                notification.addAction(NotificationCompat.Action(browseIcon, "Browse", makePendingIntentOpenURL(CodeforcesURLFactory.contest(contestID),this)))
                start(handle, contestID, notification)
            }
            ACTION_STOP -> {
                CodeforcesContestWatchLauncherWorker.stopWatcher(this, startedWithContestID!!)
                stopWatcher()
                stopForeground(true)
                stopSelf()
            }
        }

        return START_REDELIVER_INTENT
    }


    private var watcherJob: Job? = null

    private fun stopWatcher(){
        watcherJob?.cancel()
        watcherJob = null
    }

    private fun start(handle: String, contestID: Int, notification: NotificationCompat.Builder){
        startedWithContestID = contestID
        startedWithHandle = handle

        startForeground(NotificationIDs.codeforces_contest_watcher, notification.build())

        watcherJob = CodeforcesContestWatcher(
            handle,
            contestID
        ).apply {
            addCodeforcesContestWatchListener(
                CodeforcesContestWatcherTableNotification(
                    this@CodeforcesContestWatchService,
                    handle,
                    notification
                )
            )
        }.start(scope)
    }

}