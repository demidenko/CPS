package com.example.test3.job_services

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.example.test3.*
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.news.SettingsNewsFragment
import com.example.test3.utils.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class CodeforcesNewsFollowJobService: CoroutineJobService() {
    companion object {

        private const val CF_FOLLOW_HANDLES = "cf_follow_handles"
        private const val CF_FOLLOW_BLOGS = "cf_follow_blogs"

        private val adapterList = Moshi.Builder().build().adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
        private val adapterMap = Moshi.Builder().build().adapter<Map<String,List<String>?>>(Types.newParameterizedType(Map::class.java, String::class.java, List::class.java))

        fun saveHandles(context: Context, handles: List<String>) {
            with(PreferenceManager.getDefaultSharedPreferences(context).edit()){
                val str = adapterList.toJson(handles)
                putString(CF_FOLLOW_HANDLES, str)
                commit()
            }
        }

        fun getSavedHandles(context: Context): List<String> {
            val str = PreferenceManager.getDefaultSharedPreferences(context).getString(CF_FOLLOW_HANDLES, null) ?: return emptyList()
            return adapterList.fromJson(str) ?: emptyList()
        }

        fun saveBlogIDs(context: Context, blogs: Map<String,List<String>?>) {
            with(PreferenceManager.getDefaultSharedPreferences(context).edit()){
                val str = adapterMap.toJson(blogs)
                putString(CF_FOLLOW_BLOGS, str)
                commit()
            }
        }

        fun getSavedBlogIDs(context: Context): Map<String,List<String>?>{
            val str = PreferenceManager.getDefaultSharedPreferences(context).getString(CF_FOLLOW_BLOGS, null) ?: return emptyMap()
            return adapterMap.fromJson(str) ?: emptyMap()
        }

        suspend fun isEnabled(context: Context): Boolean = SettingsNewsFragment.getSettings(context).getFollowEnabled()
    }

    class FollowDataConnector(private val context: Context) {

        private val handles by lazy { getSavedHandles(context).toMutableList() }
        private var handlesChanged = false
        @JvmName("getHandles1")
        fun getHandles() = handles.toList()

        private val blogsMap by lazy { getSavedBlogIDs(context).toMutableMap() }
        private var dataChanged = false
        @JvmName("getBlogsMap1")
        fun getBlogsMap() = blogsMap.toMap()

        private fun handleIndex(handle: String) = handles.indexOfFirst { handle.equals(it,true) }

        suspend fun add(handle: String): Boolean {
            if(handleIndex(handle) != -1) return false

            val locale = NewsFragment.getCodeforcesContentLanguage(context)
            val userBlogs = CodeforcesAPI.getUserBlogEntries(handle,locale)?.result?.map { it.id.toString() }

            handles.add(0, handle)
            blogsMap[handle] = userBlogs

            handlesChanged = true
            dataChanged = true

            return true
        }

        fun remove(handle: String){
            handles.remove(handle)
            blogsMap.remove(handle)
            dataChanged = true
            handlesChanged = true
        }

        fun changeHandle(fromHandle: String, toHandle: String){
            val fromIndex = handleIndex(fromHandle)
            val toIndex = handleIndex(toHandle)
            if(fromIndex == -1 || fromIndex == toIndex) return

            if(toIndex != -1){
                handles.removeAt(fromIndex)
            }else{
                handles[fromIndex] = toHandle
                blogsMap[toHandle] = blogsMap[fromHandle]
            }
            blogsMap.remove(fromHandle)

            handlesChanged = true
            dataChanged = true
        }

        fun setBlogs(handle: String, blogs: List<String>?){
            blogsMap[handle] = blogs
            dataChanged = true
        }

        fun save(){
            if(!dataChanged) return
            if(handlesChanged){
                saveHandles(context, handles)
                handlesChanged = false
            }
            for(handle in handles) if(!blogsMap.containsKey(handle)) blogsMap.remove(handle)
            saveBlogIDs(context, blogsMap)
            dataChanged = false
        }
    }

    override suspend fun makeJobs(): List<Job> {
        if (isEnabled(this)) return listOf( launch { parseBlogs() })
        else{
            JobServicesCenter.stopJobService(this, JobServiceIDs.codeforces_news_follow)
            return emptyList()
        }
    }

    private suspend fun parseBlogs(){

        val connector = FollowDataConnector(this)
        val savedHandles = connector.getHandles()
        val savedBlogs = connector.getBlogsMap()
        val locale = NewsFragment.getCodeforcesContentLanguage(this)
        val codeforcesAccountManager by lazy { CodeforcesAccountManager(this) }

        val proceeded = mutableSetOf<String>()
        suspend fun proceedUser(handle: String){
            if(!proceeded.add(handle.toLowerCase())) return

            val response = CodeforcesAPI.getUserBlogEntries(handle, locale) ?: return
            if(response.status == CodeforcesAPIStatus.FAILED){
                //"handle: You are not allowed to read that blog" -> no activity
                if(response.isBlogHandleNotFound(handle)){
                    val info = codeforcesAccountManager.loadInfo(handle, 1) as CodeforcesAccountManager.CodeforcesUserInfo
                    when(info.status){
                        STATUS.OK -> {
                            connector.changeHandle(handle, info.handle)
                            proceedUser(info.handle)
                            return
                        }
                        STATUS.NOT_FOUND -> connector.remove(handle)
                        STATUS.FAILED -> return
                    }
                }
                return
            }

            val result = response.result ?: return

            var hasNewBlog = false
            val saved = savedBlogs[handle]?.toSet()

            if(saved == null){
                hasNewBlog = true
            }else{
                result.forEach { blogEntry ->
                    if(!saved.contains(blogEntry.id.toString())){
                        hasNewBlog = true
                        notifyNewBlog(blogEntry)
                    }
                }
            }
            if(hasNewBlog){
                connector.setBlogs(handle, result.map { it.id.toString() })
            }
        }

        savedHandles.forEach { handle -> proceedUser(handle) }

        connector.save()
    }

    private fun notifyNewBlog(blogEntry: CodeforcesBlogEntry){
        val title = fromHTML(blogEntry.title.removeSurrounding("<p>", "</p>")).toString()
        val n = notificationBuilder(this, NotificationChannels.codeforces_follow_new_blog).apply {
            setSubText("New codeforces blog")
            setContentTitle(blogEntry.authorHandle)
            setBigContent(title)
            setSmallIcon(R.drawable.ic_new_post)
            setAutoCancel(true)
            setShowWhen(true)
            setWhen(TimeUnit.SECONDS.toMillis(blogEntry.creationTimeSeconds))
            setContentIntent(makePendingIntentOpenURL(CodeforcesURLFactory.blog(blogEntry.id), this@CodeforcesNewsFollowJobService))
        }
        NotificationManagerCompat.from(this).notify(NotificationIDs.makeCodeforcesFollowBlogID(blogEntry.id), n.build())
    }
}