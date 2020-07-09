package com.example.test3.utils

import com.example.test3.account_manager.NOT_RATED
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException

object CodeforcesUtils {

    suspend fun getBlogCreationTimeMillis(blogID: String): Long = withContext(Dispatchers.IO){
        val response = CodeforcesAPI.getBlogEntry(blogID.toInt()) ?: return@withContext 0L
        val blogInfo = response.result ?: return@withContext 0L
        return@withContext blogInfo.creationTimeSeconds * 1000
    }

}

enum class CodeforcesAPIStatus{
    OK, FAILED
}

@JsonClass(generateAdapter = true)
data class CodeforcesAPIResponse<T>(
    val status: CodeforcesAPIStatus,
    val result: T? = null,
    val comment: String = ""
){
    constructor(error: CodeforcesAPIErrorResponse) : this(
        status = error.status,
        comment = error.comment
    )
}

@JsonClass(generateAdapter = true)
data class CodeforcesAPIErrorResponse(
    val status: CodeforcesAPIStatus,
    val comment: String
){
    companion object{
        val jsonAdapter: JsonAdapter<CodeforcesAPIErrorResponse> = Moshi.Builder().build().adapter(CodeforcesAPIErrorResponse::class.java)
    }
}

@JsonClass(generateAdapter = true)
data class CodeforcesUser(
    val handle: String,
    val rating: Int = NOT_RATED,
    val contribution: Int = 0
)

@JsonClass(generateAdapter = true)
data class CodeforcesBlogEntry(
    val id: Int,
    val title: String,
    val authorHandle: String,
    val rating: Int,
    val creationTimeSeconds: Long
)


object CodeforcesAPI {

    interface API {
        @GET("user.info")
        fun getUser(
            @Query("handles") handle: String
        ): Call<CodeforcesAPIResponse<List<CodeforcesUser>>>

        @GET("blogEntry.view")
        fun getBlogEntry(
            @Query("blogEntryId") blogID: Int,
            @Query("locale") lang: String = "ru"
        ): Call<CodeforcesAPIResponse<CodeforcesBlogEntry>>

        @GET("user.blogEntries")
        fun getUserBlogs(
            @Query("handle") handle: String,
            @Query("locale") lang: String = "ru"
        ): Call<CodeforcesAPIResponse<List<CodeforcesBlogEntry>>>
    }

    interface WEB {
        @GET("data/handles")
        fun getHandleSuggestions(
            @Query("q") prefix: String
        ): Call<ResponseBody>

        @GET("{page}")
        fun getPage(
            @Path("page") page: String,
            @Query("locale") lang: String
        ): Call<ResponseBody>
    }

    private val api = Retrofit.Builder()
        .baseUrl("https://codeforces.com/api/")
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(httpClient)
        .build()
        .create(API::class.java)

    private val web = Retrofit.Builder()
        .baseUrl("https://codeforces.com/")
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(httpClient)
        .build()
        .create(WEB::class.java)

    private suspend fun <T> makeCall(call: Call<CodeforcesAPIResponse<T>>): CodeforcesAPIResponse<T>? {
        var c = call
        while(true){
            try{
                val r = c.execute()
                if(r.isSuccessful) return r.body()
                val s = r.errorBody()?.string() ?: return null
                val er: CodeforcesAPIErrorResponse = CodeforcesAPIErrorResponse.jsonAdapter.fromJson(s) ?: return null
                if(er.comment == "Call limit exceeded"){
                    delay(500)
                    c = c.clone()
                    continue
                }
                return CodeforcesAPIResponse(er)
            }catch (e : IOException){
                return null
            }
        }
    }

    suspend fun getUser(handle: String): CodeforcesAPIResponse<List<CodeforcesUser>>? {
        return makeCall(api.getUser(handle))
    }

    suspend fun getBlogEntry(blogID: Int): CodeforcesAPIResponse<CodeforcesBlogEntry>? {
        return makeCall(api.getBlogEntry(blogID))
    }

    suspend fun getHandleSuggestions(str: String): Response<ResponseBody>? {
        try {
            return web.getHandleSuggestions(str).execute()
        }catch (e: IOException){
            return null
        }
    }

    suspend fun getPageSource(page: String, lang: String): String? {
        try {
            return withContext(Dispatchers.IO){
                web.getPage(page, lang).execute().body()?.string()
            }
        }catch (e: IOException){
            return null
        }
    }
}