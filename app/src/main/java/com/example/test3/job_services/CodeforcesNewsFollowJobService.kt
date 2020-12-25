package com.example.test3.job_services

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.example.test3.*
import com.example.test3.utils.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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

        fun isEnabled(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.news_codeforces_follow_enabled), false)
        }
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

        suspend fun add(handle: String): Boolean {
            if(handles.contains(handle)) return false

            val userBlogs = CodeforcesAPI.getUserBlogEntries(handle)?.result?.map { it.id.toString() }

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

    override suspend fun makeJobs(): ArrayList<Job> {
        if (isEnabled(this)) return arrayListOf( launch { parseBlogs() })
        else{
            JobServicesCenter.stopJobService(this, JobServiceIDs.codeforces_news_follow)
            return arrayListOf()
        }
    }

    private suspend fun parseBlogs(){

        val connector = FollowDataConnector(this)
        val savedHandles = connector.getHandles()
        val savedBlogs = connector.getBlogsMap()

        savedHandles.forEach { handle ->
            val response = CodeforcesAPI.getUserBlogEntries(handle) ?: return@forEach
            if(response.status == CodeforcesAPIStatus.FAILED){
                if(response.comment == "handle: User with handle $handle not found"){
                    connector.remove(handle)
                }
                return@forEach
            }

            val result = response.result ?: return@forEach

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
            setContentIntent(makePendingIntentOpenURL(CodeforcesURLFactory.blog(blogEntry.id), this@CodeforcesNewsFollowJobService))
        }
        NotificationManagerCompat.from(this).notify(NotificationIDs.makeCodeforcesFollowBlogID(blogEntry.id), n.build())
    }
}