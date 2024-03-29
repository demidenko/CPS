package com.example.test3.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.Charset

object ACMPAPI {

    private interface WEB {
        @GET(".")
        fun getMainPage(): Call<ResponseBody>

        @GET("index.asp?main=user")
        fun getUser(
            @Query("id") id: String
        ): Call<ResponseBody>

        @GET("index.asp?main=rating")
        fun getUserSearch(
            @Query(value = "str", encoded = true) str: String
        ): Call<ResponseBody>
    }

    private val web = createRetrofit<WEB>("https://acmp.ru/")

    private val windows1251 = Charset.forName("windows-1251")
    private fun decode(r: Response<ResponseBody>?): String? =
        r?.body()?.bytes()?.toString(windows1251)


    suspend fun getMainPage(): String? {
        try {
            return withContext(Dispatchers.IO) {
                decode(web.getMainPage().execute())
            }
        }catch (e: IOException){
            return null
        }
    }

    suspend fun getUser(id: String): String? {
        try {
            return withContext(Dispatchers.IO){
                decode(web.getUser(id).execute())
            }
        }catch (e: IOException){
            return null
        }
    }

    suspend fun getUserSearch(str: String): String? {
        try {
            return withContext(Dispatchers.IO){
                decode(web.getUserSearch(URLEncoder.encode(str, "windows-1251")).execute())
            }
        }catch (e: IOException){
            return null
        }
    }
}