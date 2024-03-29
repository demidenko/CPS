package com.example.test3.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException


@Serializable
data class AtCoderRatingChange(
    val NewRating: Int,
    val OldRating: Int,
    val Place: Int,
    @Serializable(with = InstantAsSecondsSerializer::class)
    val EndTime: Instant,
    val ContestName: String,
    val StandingsUrl: String
){
    fun getContestID(): String {
        val s = StandingsUrl.removePrefix("/contests/")
        return s.substring(0, s.indexOf('/'))
    }
}

object AtCoderAPI {

    private interface WEB {
        @GET("users/{handle}?graph=rating")
        fun getUser(
            @Path("handle") handle: String
        ): Call<ResponseBody>

        @GET("ranking/all")
        fun getRankingSearch(
            @Query("f.UserScreenName") str: String
        ): Call<ResponseBody>
    }

    private val web = createRetrofit<WEB>("https://atcoder.jp/")

    suspend fun getUser(handle: String): Response<ResponseBody>? = withContext(Dispatchers.IO){
        try {
            web.getUser(handle).execute()
        }catch (e: IOException){
            null
        }
    }

    suspend fun getRankingSearch(str: String): Response<ResponseBody>? = withContext(Dispatchers.IO){
        try {
            web.getRankingSearch(str).execute()
        }catch (e: IOException){
            null
        }
    }

    suspend fun getRatingChanges(handle: String): List<AtCoderRatingChange>? = withContext(Dispatchers.IO){
        try {
            val response = web.getUser(handle).execute() ?: return@withContext null
            if(!response.isSuccessful) return@withContext null
            val s = response.body()?.string() ?: return@withContext null
            val i = s.lastIndexOf("<script>var rating_history=[{")
            if(i==-1) return@withContext null
            val j = s.indexOf("}];</script>", i)
            val str = s.substring(s.indexOf('=',i)+1, j+2)
            return@withContext jsonCPS.decodeFromString<List<AtCoderRatingChange>>(str)
        }catch (e: IOException){
            return@withContext null
        }
    }
}


object AtCoderURLFactory {

    private const val main = "https://atcoder.jp"

    fun user(handle: String) = "$main/users/$handle"

    fun userContestResult(handle: String, contestID: String) = "$main/users/$handle/history/share/$contestID"

}