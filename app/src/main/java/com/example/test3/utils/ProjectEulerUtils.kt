package com.example.test3.utils

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import java.io.IOException

object ProjectEulerAPI {

    interface WEB {
        @GET("news")
        fun getNewsPage(): Call<ResponseBody>

        @GET("recent")
        fun getRecentProblemsPage(): Call<ResponseBody>
    }

    private val projecteulerWEB = Retrofit.Builder()
        .baseUrl("https://projecteuler.net/")
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(httpClient)
        .build()
        .create(WEB::class.java)

    suspend fun getNewsPage(): String? {
        try {
            return withContext(Dispatchers.IO){
                projecteulerWEB.getNewsPage().execute().body()?.string()
            }
        }catch (e: IOException){
            return null
        }
    }

    suspend fun getRecentProblemsPage(): String? {
        try {
            return withContext(Dispatchers.IO){
                projecteulerWEB.getRecentProblemsPage().execute().body()?.string()
            }
        }catch (e: IOException){
            return null
        }
    }
}