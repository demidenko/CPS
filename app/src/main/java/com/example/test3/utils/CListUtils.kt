package com.example.test3.utils

import android.content.Context
import android.widget.Toast
import com.example.test3.account_manager.*
import com.example.test3.contests.Contest
import com.example.test3.contests.settingsContests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import kotlin.time.Duration.Companion.days

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
class ClistApiResponse<T>(
    val objects: List<T>
)

@Serializable
data class ClistContest(
    val resource_id: Int,
    val id: Long,
    val start: String,
    val end: String,
    val event: String,
    val href: String,
    val host: String
) {
    fun getPlatform(): Contest.Platform = Contest.Platform.values().find { getClistApiResourceId(it) == resource_id } ?: Contest.Platform.unknown
}

@Serializable
data class ClistResource(
    val id: Int,
    val name: String
)

fun getClistApiResourceId(platform: Contest.Platform) =
    when(platform) {
        Contest.Platform.unknown -> 0
        Contest.Platform.codeforces -> 1
        Contest.Platform.atcoder -> 93
        Contest.Platform.topcoder -> 12
        Contest.Platform.codechef -> 2
        Contest.Platform.google -> 35
        Contest.Platform.dmoj -> 77
    }

object CListAPI {

    private interface API {
        @GET("contest/?format=json")
        fun getContests(
            @Query("username") login: String,
            @Query("api_key") apikey: String,
            @Query("start__gte") startTime: String,
            @Query("end__lte") endTime: String,
            @Query("resource_id__in") resources: String = ""
        ): Call<ClistApiResponse<ClistContest>>

        @GET("resource/?format=json&limit=1000")
        fun getResources(
            @Query("username") login: String,
            @Query("api_key") apikey: String
        ): Call<ClistApiResponse<ClistResource>>
    }

    private val api = createRetrofitWithJson<API>("https://clist.by/api/v2/")

    private interface WEB {
        @GET("coder/{login}")
        fun getUser(
            @Path("login") login: String
        ): Call<ResponseBody>

        @GET("coders")
        fun getUsersSearch(
            @Query("search") str: String
        ): Call<ResponseBody>
    }

    private val web = createRetrofit<WEB>("https://clist.by/")

    suspend fun getUser(login: String): Response<ResponseBody>? = withContext(Dispatchers.IO){
        try {
            web.getUser(login).execute()
        }catch (e: IOException){
            null
        }
    }

    suspend fun getUsersSearch(str: String): Response<ResponseBody>?  = withContext(Dispatchers.IO){
        try {
            web.getUsersSearch(str).execute()
        }catch (e: IOException){
            null
        }
    }

    private suspend fun getClistApiData(context: Context): Pair<String, String>? {
        val info = context.settingsContests.getClistApiLoginAndKey()
        if(info == null) Toast.makeText(context, "Clist api not set", Toast.LENGTH_LONG).show()
        return info
    }

    suspend fun getContests(
        context: Context,
        platforms: Collection<Contest.Platform>,
        startTime: Instant,
        endTime: Instant = startTime + 120.days
    ): List<ClistContest>? = withContext(Dispatchers.IO) {
        val (login, apikey) = getClistApiData(context) ?: return@withContext null
        try {
            val call = api.getContests(
                login,
                apikey,
                startTime.toString(),
                endTime.toString(),
                platforms.joinToString { getClistApiResourceId(it).toString() }
            )
            val r = call.execute()
            if(!r.isSuccessful) null
            else r.body()?.objects
        } catch (e: IOException) {
            null
        }
    }

    suspend fun getResources(context: Context): List<ClistResource>? = withContext(Dispatchers.IO) {
        val (login, apikey) = getClistApiData(context) ?: return@withContext null
        try {
            val call = api.getResources(login, apikey)
            val r = call.execute()
            if(!r.isSuccessful) null
            else r.body()?.objects
        } catch (e: IOException) {
            null
        }
    }
}
