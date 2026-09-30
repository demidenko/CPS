package com.example.test3.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import java.io.IOException

object ProjectEulerAPI {

    private interface WEB {
        @GET("news")
        fun getNewsPage(): Call<ResponseBody>

        @GET("recent")
        fun getRecentProblemsPage(): Call<ResponseBody>
    }

    private val web = createRetrofit<WEB>("https://projecteuler.net/")

    suspend fun getNewsPage(): String? {
        try {
            return withContext(Dispatchers.IO){
                web.getNewsPage().execute().body()?.string()
            }
        }catch (e: IOException){
            return null
        }
    }

    suspend fun getRecentProblemsPage(): String? {
        try {
            return withContext(Dispatchers.IO){
                web.getRecentProblemsPage().execute().body()?.string()
            }
        }catch (e: IOException){
            return null
        }
    }
}