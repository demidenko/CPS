package com.example.test3.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException

object TimusAPI {

    private interface WEB {
        @GET("author.aspx")
        fun getUser(
            @Query("id") id: String,
            @Query("locale") locale: String = "en"
        ): Call<ResponseBody>

        @GET("search.aspx")
        fun getUserSearch(
            @Query("Str") str: String
        ): Call<ResponseBody>
    }

    private val web = createRetrofit<WEB>("https://timus.online/")

    suspend fun getUser(id: String): String? = withContext(Dispatchers.IO){
        try {
            web.getUser(id).execute().body()?.string()
        }catch (e: IOException){
            null
        }
    }

    suspend fun getUserSearch(str: String): String? = withContext(Dispatchers.IO){
        try {
            web.getUserSearch(str).execute().body()?.string()
        }catch (e: IOException){
            null
        }
    }
}