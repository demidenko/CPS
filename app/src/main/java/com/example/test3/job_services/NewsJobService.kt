package com.example.test3.job_services

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import com.example.test3.*
import com.example.test3.news.SettingsNewsFragment
import com.example.test3.utils.ACMPAPI
import com.example.test3.utils.OlympiadsZaochAPI
import com.example.test3.utils.ProjectEulerAPI
import com.example.test3.utils.fromHTML
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NewsJobService : CoroutineJobService() {

    override suspend fun makeJobs(): List<Job> {
        val jobs = mutableListOf<Job>()
        with(SettingsNewsFragment.getSettings(this)){
            if(getNewsFeedEnabled(SettingsNewsFragment.NewsFeed.PROJECT_EULER_NEWS)) jobs.add(launch { parseProjectEuler() })
            if(getNewsFeedEnabled(SettingsNewsFragment.NewsFeed.ACMP_NEWS)) jobs.add(launch { parseACMP() })
            if(getNewsFeedEnabled(SettingsNewsFragment.NewsFeed.ZAOCH_NEWS)) jobs.add(launch { parseZaoch() })
        }
        if(jobs.isEmpty()){
            JobServicesCenter.stopJobService(this, JobServiceIDs.news_parsers)
        }
        return jobs
    }

    private val dataStore by lazy { NewsJobServiceDataStore(this) }

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
                val n = notificationBuilder(this, NotificationChannels.acmp_news).apply {
                    setSubText("acmp news")
                    setBigContent(content)
                    setSmallIcon(R.drawable.ic_news)
                    setColor(NotificationColors.acmp_main)
                    setShowWhen(true)
                    setAutoCancel(true)
                    setGroup(group)
                    setContentIntent(makePendingIntentOpenURL("https://acmp.ru", this@NewsJobService))
                }
                NotificationManagerCompat.from(this).notify( NotificationIDs.makeACMPNewsNotificationID(id), n.build())
            }

            val n = notificationBuilder(this, NotificationChannels.acmp_news).apply {
                setStyle(NotificationCompat.InboxStyle().setSummaryText("acmp news"))
                setSmallIcon(R.drawable.ic_news)
                setColor(NotificationColors.acmp_main)
                setAutoCancel(true)
                setGroup(group)
                setGroupSummary(true)
            }
            NotificationManagerCompat.from(this).notify( NotificationIDs.makeACMPNewsNotificationID(0), n.build())
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
                val n = notificationBuilder(this, NotificationChannels.project_euler_news).apply {
                    setSubText("Project Euler news")
                    setContentTitle(title)
                    setBigContent(fromHTML(content))
                    setSmallIcon(R.drawable.ic_news)
                    setColor(NotificationColors.project_euler_main)
                    setShowWhen(true)
                    setAutoCancel(true)
                    setContentIntent(makePendingIntentOpenURL("https://projecteuler.net/news", this@NewsJobService))
                }
                NotificationManagerCompat.from(this).notify( NotificationIDs.makeProjectEulerNewsNotificationID(title), n.build())
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
                val n = notificationBuilder(this, NotificationChannels.olympiads_zaoch_news).apply {
                    setSubText("zaoch news")
                    setContentTitle(title)
                    setBigContent(fromHTML(content))
                    setSmallIcon(R.drawable.ic_news)
                    setColor(NotificationColors.zaoch_main)
                    setShowWhen(true)
                    //setAutoCancel(true)
                    setContentIntent(makePendingIntentOpenURL("https://olympiads.ru/zaoch/", this@NewsJobService))
                }
                NotificationManagerCompat.from(this).notify(NotificationIDs.makeZaochNewsNotificationID(title), n.build())
            }
        }

        val firstID = news.first().first
        if(firstID != lastNewsID) {
            dataStore.setOlympiadsZaochLastNewsID(firstID)
        }
    }

    class NewsJobServiceDataStore(context: Context){
        private val dataStore by lazy { context.createDataStore(name = "jobservice_news") }

        companion object {
            private val KEY_ACMP_LAST_NEWS = preferencesKey<Int>("acmp_last_news")
            private val KEY_PROJECT_EULER_LAST_NEWS = preferencesKey<String>("project_euler_last_news")
            private val KEY_OLYMPIADS_ZAOCH_LAST_NEWS = preferencesKey<String>("olympiads_zaoch_last_news")
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