package com.example.test3.utils

import android.content.Context
import com.example.test3.account_manager.*
import com.example.test3.contests.Contest
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object CListUtils {
    fun getManager(resource: String, userName: String, link: String, context: Context): Pair<AccountManager<out UserInfo>,String>? {
        return when(resource){
            "codeforces.com" -> Pair(CodeforcesAccountManager(context), userName)
            "topcoder.com" -> Pair(TopCoderAccountManager(context), userName)
            "atcoder.jp" -> Pair(AtCoderAccountManager(context), userName)
            "acm.timus.ru", "timus.online" -> {
                val userId = link.substring(link.lastIndexOf('=')+1)
                Pair(TimusAccountManager(context), userId)
            }
            else -> null
        }
    }
}

@Serializable
class ClistApiContestsResponse(
    val objects: List<ClistContest>
)

@Serializable
data class ClistContest(
    val resource_id: Int,
    val id: Long,
    val start: String,
    val duration: Long,
    val event: String,
    val href: String,
) {
    fun getPlatform(): Contest.Platform = Contest.Platform.values().find { getClistApiResourceId(it) == resource_id } ?: Contest.Platform.unknown
}

fun getClistApiResourceId(platform: Contest.Platform) =
    when(platform) {
        Contest.Platform.unknown -> 0
        Contest.Platform.codeforces -> 1
        Contest.Platform.atcoder -> 93
    }

object CListAPI {
    interface API {
        @GET("contest/?format=json")
        fun getContests(
            @Query("username") login: String,
            @Query("api_key") apikey: String,
            @Query("start__gte") startTime: String,
            @Query("resource_id__in") resources: String = ""
        ): Call<ClistApiContestsResponse>
    }

    private val clistAPI = Retrofit.Builder()
        .baseUrl("https://clist.by/api/v2/")
        .addConverterFactory(jsonConverterFactory)
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(httpClient)
        .build()
        .create(API::class.java)

    interface WEB {
        @GET("coder/{login}")
        fun getUser(
            @Path("login") login: String
        ): Call<ResponseBody>

        @GET("coders")
        fun getUsersSearch(
            @Query("search") str: String
        ): Call<ResponseBody>
    }

    private val clistWEB = Retrofit.Builder()
        .baseUrl("https://clist.by/")
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(httpClient)
        .build()
        .create(WEB::class.java)

    suspend fun getUser(login: String): Response<ResponseBody>? = withContext(Dispatchers.IO){
        try {
            clistWEB.getUser(login).execute()
        }catch (e: IOException){
            null
        }
    }

    suspend fun getUsersSearch(str: String): Response<ResponseBody>?  = withContext(Dispatchers.IO){
        try {
            clistWEB.getUsersSearch(str).execute()
        }catch (e: IOException){
            null
        }
    }

    suspend fun getContests(
        login: String,
        apikey: String,
        platforms: List<Contest.Platform>,
        startTimeSeconds: Long
    ): List<ClistContest>? = withContext(Dispatchers.IO) {
        try {
            val call = clistAPI.getContests(
                login,
                apikey,
                secondsToString(startTimeSeconds),
                platforms.joinToString { getClistApiResourceId(it).toString() }
            )
            val r = call.execute()
            if(!r.isSuccessful) null
            else r.body()?.objects
        } catch (e: IOException) {
            null
        }
    }

    private val clistDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    fun secondsToString(seconds: Long): String = clistDateFormat.format(Date(TimeUnit.SECONDS.toMillis(seconds)))
    fun dateToSeconds(str: String): Long = TimeUnit.MILLISECONDS.toSeconds(clistDateFormat.parse(str).time)
}
