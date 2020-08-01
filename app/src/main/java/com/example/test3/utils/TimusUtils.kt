package com.example.test3.utils

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException

object TimusAPI {

    interface WEB {
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

    private val timusWEB = Retrofit.Builder()
        .baseUrl("https://timus.online/")
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(httpClient)
        .build()
        .create(WEB::class.java)

    suspend fun getUser(id: String): String? {
        try {
            return timusWEB.getUser(id).execute().body()?.string()
        }catch (e: IOException){
            return null
        }
    }

    suspend fun getUserSearch(str: String): String? {
        try {
            return withContext(Dispatchers.IO){
                timusWEB.getUserSearch(str).execute().body()?.string()
            }
        }catch (e: IOException){
            return null
        }
    }
}