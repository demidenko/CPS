package com.example.test3.utils

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import java.io.IOException
import java.nio.charset.Charset

object OlympiadsZaochAPI {
    interface WEB {
        @GET(".")
        fun getMainPage(): Call<ResponseBody>

    }

    private val zaochWEB = Retrofit.Builder()
        .baseUrl("https://olympiads.ru/zaoch/")
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(httpClient)
        .build()
        .create(WEB::class.java)

    private val koi8r = Charset.forName("koi8-r")
    private fun decode(r: Response<ResponseBody>?): String? =
        r?.body()?.bytes()?.toString(koi8r)

    suspend fun getMainPage(): String? {
        try {
            return withContext(Dispatchers.IO) {
                decode(zaochWEB.getMainPage().execute())
            }
        }catch (e: IOException){
            return null
        }
    }

}