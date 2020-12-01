package com.example.test3.job_services

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.example.test3.NotificationChannels
import com.example.test3.NotificationIDs
import com.example.test3.R
import com.example.test3.makePendingIntentOpenURL
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
    }

    override suspend fun makeJobs(): ArrayList<Job> {
        return arrayListOf(
            launch { parseBlogs() }
        )
    }

    private suspend fun parseBlogs(){

        val savedHandles = getSavedHandles(this)
        val handles = savedHandles.toMutableList()

        val savedBlogs = getSavedBlogIDs(this)

        val toSave = savedBlogs.toMutableMap()

        var isBlogsNeedToSave = false
        savedHandles.forEach { handle ->
            val response = CodeforcesAPI.getUserBlogEntries(handle) ?: return@forEach
            if(response.status == CodeforcesAPIStatus.FAILED){
                if(response.comment == "handle: User with handle $handle not found"){
                    handles.remove(handle)
                    toSave.remove(handle)
                    isBlogsNeedToSave = true
                }
                return@forEach
            }

            val result = response.result ?: return@forEach

            var hasNewBlog = false
            val saved = savedBlogs.getOrDefault(handle, null)?.toSet()

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
                toSave[handle] = result.map { it.id.toString() }
                isBlogsNeedToSave = true
            }
        }

        if(isBlogsNeedToSave){
            for(handle in handles) if(!toSave.containsKey(handle)) toSave.remove(handle)
            saveBlogIDs(this, toSave)
        }
        if(handles.size < savedHandles.size) saveHandles(this, handles)
    }

    private fun notifyNewBlog(blogEntry: CodeforcesBlogEntry){
        val title = fromHTML(blogEntry.title.removeSurrounding("<p>", "</p>")).toString()
        val n = NotificationCompat.Builder(this, NotificationChannels.codeforces_follow_new_blog).apply {
            setSubText("New codeforces blog")
            setContentTitle(blogEntry.authorHandle)
            setContentText(title)
            setStyle(NotificationCompat.BigTextStyle())
            setSmallIcon(R.drawable.ic_new_post)
            setAutoCancel(true)
            setContentIntent(makePendingIntentOpenURL(CodeforcesURLFactory.blog(blogEntry.id), applicationContext))
        }
        NotificationManagerCompat.from(this).notify(NotificationIDs.makeCodeforcesFollowBlogID(blogEntry.id), n.build())
    }
}