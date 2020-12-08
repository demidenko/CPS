package com.example.test3.utils

import com.example.test3.MainActivity
import com.example.test3.account_manager.AccountManager
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException

object CListUtils {
    fun getManager(resource: String, userName: String, link: String, activity: MainActivity): Pair<AccountManager,String>? {
        return with(activity.accountsFragment){
            when(resource){
                "codeforces.com" -> Pair(codeforcesAccountManager, userName)
                "topcoder.com" -> Pair(topcoderAccountManager, userName)
                "atcoder.jp" -> Pair(atcoderAccountManager, userName)
                "acm.timus.ru", "timus.online" -> {
                    val userId = link.substring(link.lastIndexOf('=')+1)
                    Pair(timusAccountManager, userId)
                }
                else -> null
            }
        }
    }
}

object CListAPI {
    interface WEB {
        @GET("coder/{login}")
        fun getUser(
            @Path("login") login: String
        ): Call<ResponseBody>

        @GET("coders")
        fun getUsersSearch(
            @Query("search") str: String
        ): Call<ResponseBody>
    }

    private val clistWEB = Retrofit.Builder()
        .baseUrl("https://clist.by/")
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(httpClient)
        .build()
        .create(WEB::class.java)

    suspend fun getUser(login: String): Response<ResponseBody>? = withContext(Dispatchers.IO){
        try {
            clistWEB.getUser(login).execute()
        }catch (e: IOException){
            null
        }
    }

    suspend fun getUsersSearch(str: String): Response<ResponseBody>?  = withContext(Dispatchers.IO){
        try {
            clistWEB.getUsersSearch(str).execute()
        }catch (e: IOException){
            null
        }
    }
}
