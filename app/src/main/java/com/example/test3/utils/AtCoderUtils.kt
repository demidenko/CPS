package com.example.test3.utils

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException


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

    suspend fun getUser(handle: String): Response<ResponseBody>? {
        try {
            return atcoderWEB.getUser(handle).execute()
        }catch (e: IOException){
            return null
        }
    }

    suspend fun getRankingSearch(str: String): Response<ResponseBody>? {
        try {
            return atcoderWEB.getRankingSearch(str).execute()
        }catch (e: IOException){
            return null
        }
    }
}