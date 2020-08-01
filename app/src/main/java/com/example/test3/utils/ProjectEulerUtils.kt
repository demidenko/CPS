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
        fun getNews(): Call<ResponseBody>

        @GET("recent")
        fun getRecent(): Call<ResponseBody>
    }

    private val projecteulerWEB = Retrofit.Builder()
        .baseUrl("https://projecteuler.net/")
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(httpClient)
        .build()
        .create(WEB::class.java)

    suspend fun getNews(): String? {
        try {
            return withContext(Dispatchers.IO){
                projecteulerWEB.getNews().execute().body()?.string()
            }
        }catch (e: IOException){
            return null
        }
    }

    suspend fun getRecent(): String? {
        try {
            return withContext(Dispatchers.IO){
                projecteulerWEB.getRecent().execute().body()?.string()
            }
        }catch (e: IOException){
            return null
        }
    }
}