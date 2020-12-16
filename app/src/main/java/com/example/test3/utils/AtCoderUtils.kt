package com.example.test3.utils

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException


@Serializable
data class AtCoderRatingChange(
    val NewRating: Int,
    val OldRating: Int,
    val Place: Int,
    val EndTime: Long,
    val ContestName: String
    //val StandingsUrl: String
)

object AtCoderAPI {
    interface WEB {
        @GET("users/{handle}")
        fun getUser(
            @Path("handle") handle: String
        ): Call<ResponseBody>

        @GET("ranking/all")
        fun getRankingSearch(
            @Query("f.UserScreenName") str: String
        ): Call<ResponseBody>
    }

    private val atcoderWEB = Retrofit.Builder()
        .baseUrl("https://atcoder.jp/")
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(httpClient)
        .build()
        .create(WEB::class.java)

    suspend fun getUser(handle: String): Response<ResponseBody>? = withContext(Dispatchers.IO){
        try {
            atcoderWEB.getUser(handle).execute()
        }catch (e: IOException){
            null
        }
    }

    suspend fun getRankingSearch(str: String): Response<ResponseBody>? = withContext(Dispatchers.IO){
        try {
            atcoderWEB.getRankingSearch(str).execute()
        }catch (e: IOException){
            null
        }
    }

    suspend fun getRatingChanges(handle: String): List<AtCoderRatingChange>? = withContext(Dispatchers.IO){
        try {
            val response = atcoderWEB.getUser(handle).execute() ?: return@withContext null
            if(!response.isSuccessful) return@withContext null
            val s = response.body()?.string() ?: return@withContext null
            val i = s.lastIndexOf("<script>rating_history=[{")
            if(i==-1) return@withContext null
            val j = s.indexOf("}];</script>", i)
            val str = s.substring(s.indexOf('=',i)+1, j+2)
            return@withContext jsonCPS.decodeFromString<List<AtCoderRatingChange>>(str)
        }catch (e: IOException){
            return@withContext null
        }
    }
}