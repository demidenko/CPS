package com.example.test3.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.test3.*
import com.example.test3.news.NewsFeed
import com.example.test3.news.settingsNews
import com.example.test3.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class NewsWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result  = withContext(Dispatchers.IO){
        val jobs = mutableListOf<Job>()
        with(context.settingsNews){
            if(getNewsFeedEnabled(NewsFeed.PROJECT_EULER_NEWS)) jobs.add(launch { parseProjectEuler() })
            if(getNewsFeedEnabled(NewsFeed.ACMP_NEWS)) jobs.add(launch { parseACMP() })
            if(getNewsFeedEnabled(NewsFeed.ZAOCH_NEWS)) jobs.add(launch { parseZaoch() })
        }
        if(jobs.isEmpty()) WorkersCenter.stopWorker(context, WorkersNames.news_parsers)
        jobs.joinAll()
        return@withContext Result.success()
    }

    private val dataStore by lazy { NewsJobServiceDataStore(context) }

    private suspend fun parseACMP() {
        val s = ACMPAPI.getMainPage() ?: return

        val lastNewsID = dataStore.getACMPLastNewsID()
        val news = mutableListOf<Pair<Int, String>>()
        var i = 0
        while (true) {
            i = s.indexOf("<a name=news_", i + 1)
            if(i==-1) break

            val currentID = s.substring(s.indexOf("_", i) + 1, s.indexOf(">", i)).toInt()
            if(lastNewsID!=-1 && currentID<=lastNewsID) break

            val content = fromHTML(s.substring(i, s.indexOf("<br><br>", i))).toString()

            news.add(Pair(currentID, content))
        }

        if(news.isEmpty()) return

        if(lastNewsID != 0){
            val group = "acmp_news_group"

            news.forEach { (id, content) ->
                val n = notificationBuilder(context, NotificationChannels.acmp_news).apply {
                    setSubText("acmp news")
                    setBigContent(content)
                    setSmallIcon(R.drawable.ic_news)
                    setColor(getColorFromResource(context, R.color.acmp_main))
                    setShowWhen(true)
                    setAutoCancel(true)
                    setGroup(group)
                    setContentIntent(makePendingIntentOpenURL("https://acmp.ru", context))
                }
                NotificationManagerCompat.from(context).notify( NotificationIDs.makeACMPNewsNotificationID(id), n.build())
            }

            val n = notificationBuilder(context, NotificationChannels.acmp_news).apply {
                setStyle(NotificationCompat.InboxStyle().setSummaryText("acmp news"))
                setSmallIcon(R.drawable.ic_news)
                setColor(getColorFromResource(context, R.color.acmp_main))
                setAutoCancel(true)
                setGroup(group)
                setGroupSummary(true)
            }
            NotificationManagerCompat.from(context).notify( NotificationIDs.makeACMPNewsNotificationID(0), n.build())
        }

        val firstID = news.first().first
        if(firstID != lastNewsID) {
            dataStore.setACMPLastNewsID(firstID)
        }
    }

    private suspend fun parseProjectEuler() {
        val s = ProjectEulerAPI.getNewsPage() ?: return

        val lastNewsID = dataStore.getProjectEulerLastNewsID()

        val news = mutableListOf<Pair<String, String>>()
        var i = 0
        while (true) {
            i = s.indexOf("<div class=\"news\">", i + 1)
            if(i == -1) break

            val currentID = s.substring(s.indexOf("<h4>", i) + 4, s.indexOf("</h4>", i))
            if(currentID == lastNewsID) break

            val content = s.substring(s.indexOf("<div>", i) + 5, s.indexOf("</div>", i))

            news.add(Pair(currentID, content))
        }

        if(news.isEmpty()) return

        if(lastNewsID!=""){
            news.forEach { (title, content) ->
                val n = notificationBuilder(context, NotificationChannels.project_euler_news).apply {
                    setSubText("Project Euler news")
                    setContentTitle(title)
                    setBigContent(fromHTML(content))
                    setSmallIcon(R.drawable.ic_news)
                    setColor(getColorFromResource(context, R.color.project_euler_main))
                    setShowWhen(true)
                    setAutoCancel(true)
                    setContentIntent(makePendingIntentOpenURL("https://projecteuler.net/news", context))
                }
                NotificationManagerCompat.from(context).notify( NotificationIDs.makeProjectEulerNewsNotificationID(title), n.build())
            }
        }

        val firstID = news.first().first
        if(firstID != lastNewsID) {
            dataStore.setProjectEulerLastNewsID(firstID)
        }
    }

    private suspend fun parseZaoch() {
        val s = OlympiadsZaochAPI.getMainPage() ?: return

        val lastNewsID = dataStore.getOlympiadsZaochLastNewsID()

        val news = mutableListOf<Triple<String, String, String>>()
        var i = 0
        val tit = "<font color=\"#B00000\">"
        while(true){
            i = s.indexOf(tit, i+1)
            if(i==-1) break

            val date = s.substring(i+tit.length, s.indexOf("</font",i))

            var j = s.indexOf("<p>", i)
            if(j==-1) j = s.indexOf("</td", i)
            val content = fromHTML(s.substring(s.indexOf(".",i)+1,j).trim()).toString()

            val currentID = "$date@${content.hashCode()}"

            if(currentID == lastNewsID) break

            news.add(Triple(currentID, date, content))
        }

        if(news.isEmpty()) return

        if(lastNewsID!=""){
            news.forEach { (_, title, content) ->
                val n = notificationBuilder(context, NotificationChannels.olympiads_zaoch_news).apply {
                    setSubText("zaoch news")
                    setContentTitle(title)
                    setBigContent(fromHTML(content))
                    setSmallIcon(R.drawable.ic_news)
                    setColor(getColorFromResource(context, R.color.zaoch_main))
                    setShowWhen(true)
                    //setAutoCancel(true)
                    setContentIntent(makePendingIntentOpenURL("https://olympiads.ru/zaoch/", context))
                }
                NotificationManagerCompat.from(context).notify(NotificationIDs.makeZaochNewsNotificationID(title), n.build())
            }
        }

        val firstID = news.first().first
        if(firstID != lastNewsID) {
            dataStore.setOlympiadsZaochLastNewsID(firstID)
        }
    }

    class NewsJobServiceDataStore(context: Context): SettingsDataStore(context, "jobservice_news"){

        companion object {
            private val KEY_ACMP_LAST_NEWS = intPreferencesKey("acmp_last_news")
            private val KEY_PROJECT_EULER_LAST_NEWS = stringPreferencesKey("project_euler_last_news")
            private val KEY_OLYMPIADS_ZAOCH_LAST_NEWS = stringPreferencesKey("olympiads_zaoch_last_news")
        }

        suspend fun getACMPLastNewsID() = dataStore.data.first()[KEY_ACMP_LAST_NEWS] ?: 0
        suspend fun setACMPLastNewsID(newsID: Int) {
            dataStore.edit { it[KEY_ACMP_LAST_NEWS] = newsID }
        }

        suspend fun getProjectEulerLastNewsID() = dataStore.data.first()[KEY_PROJECT_EULER_LAST_NEWS] ?: ""
        suspend fun setProjectEulerLastNewsID(newsID: String) {
            dataStore.edit { it[KEY_PROJECT_EULER_LAST_NEWS] = newsID }
        }

        suspend fun getOlympiadsZaochLastNewsID() = dataStore.data.first()[KEY_OLYMPIADS_ZAOCH_LAST_NEWS] ?: ""
        suspend fun setOlympiadsZaochLastNewsID(newsID: String) {
            dataStore.edit { it[KEY_OLYMPIADS_ZAOCH_LAST_NEWS] = newsID }
        }
    }

}