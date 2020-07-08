package com.example.test3

import com.example.test3.account_manager.NOT_RATED
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException

object CodeforcesUtils {

    suspend fun getBlogCreationTimeMillis(blogID: String): Long = withContext(Dispatchers.IO){
        val blogInfo = CF.getBlog(blogID.toInt()) ?: return@withContext 0L
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


object CF {

    interface CFAPI {
        @GET("user.info")
        fun getUser(
            @Query("handles") handle: String
        ): Call<CodeforcesAPIResponse<List<CodeforcesUser>>>

        @GET("blogEntry.view")
        fun getBlog(
            @Query("blogEntryId") blogID: Int,
            @Query("locale") lang: String = "ru"
        ): Call<CodeforcesAPIResponse<CodeforcesBlogEntry>>

        @GET("user.blogEntries")
        fun getUserBlogs(
            @Query("handle") handle: String,
            @Query("locale") lang: String = "ru"
        ): Call<CodeforcesAPIResponse<List<CodeforcesBlogEntry>>>
    }

    private val codeforcesAPI = Retrofit.Builder()
        .baseUrl("https://codeforces.com/api/")
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()
        .create(CFAPI::class.java)

    private suspend fun <T> makeCall(call: Call<CodeforcesAPIResponse<T>>): CodeforcesAPIResponse<T>? {
        var c = call
        while(true){
            try{
                val r = c.execute()
                if(!r.isSuccessful){
                    val s = r.errorBody()?.string() ?: return null
                    val er: CodeforcesAPIErrorResponse = CodeforcesAPIErrorResponse.jsonAdapter.fromJson(s) ?: return null
                    if(er.comment == "Call limit exceeded"){
                        delay(500)
                        c = c.clone()
                        continue
                    }
                    return CodeforcesAPIResponse(er)
                }
                return r.body()
            }catch (e : IOException){
                return null
            }
        }
    }

    suspend fun getUser(handle: String): CodeforcesAPIResponse<List<CodeforcesUser>>? {
        return makeCall(codeforcesAPI.getUser(handle))
    }

    suspend fun getBlog(blogID: Int): CodeforcesBlogEntry? {
        val res = makeCall(codeforcesAPI.getBlog(blogID)) ?: return null

        if(res.status == CodeforcesAPIStatus.OK){
            return res.result
        }else{
            return null
        }
    }

    suspend fun getUserBlogs(handle: String): List<CodeforcesBlogEntry>? {
        val res = makeCall(codeforcesAPI.getUserBlogs(handle)) ?: return null

        if(res.status == CodeforcesAPIStatus.OK){
            return res.result
        }else{
            return null
        }
    }
}