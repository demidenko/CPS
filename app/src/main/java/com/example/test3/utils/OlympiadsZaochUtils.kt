package com.example.test3.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import java.io.IOException
import java.nio.charset.Charset

object OlympiadsZaochAPI {

    private interface WEB {
        @GET(".")
        fun getMainPage(): Call<ResponseBody>
    }

    private val web = createRetrofit<WEB>("https://olympiads.ru/zaoch/")

    private val koi8r = Charset.forName("koi8-r")
    private fun decode(r: Response<ResponseBody>?): String? =
        r?.body()?.bytes()?.toString(koi8r)

    suspend fun getMainPage(): String? {
        try {
            return withContext(Dispatchers.IO) {
                decode(web.getMainPage().execute())
            }
        }catch (e: IOException){
            return null
        }
    }

}