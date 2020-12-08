package com.example.test3.utils

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.IOException

@Serializable
data class TopCoderUser(
    val handle: String = "",
    val ratingSummary: List<TopCoderRatingSummary> = emptyList(),
    val error: TopCoderError? = null
){
    @Serializable
    data class TopCoderRatingSummary(
        val name: String,
        val rating: Int
    )

    @Serializable
    data class TopCoderError(
        val name: String
    )
}

object TopCoderAPI {
    interface API {
        @GET("users/{handle}")
        fun getUser(
            @Path("handle") handle: String
        ): Call<TopCoderUser>
    }

    private val topcoderAPI = Retrofit.Builder()
        .baseUrl("https://api.topcoder.com/v2/")
        .addConverterFactory(jsonConverterFactory)
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(httpClient)
        .build()
        .create(API::class.java)

    suspend fun getUser(handle: String): TopCoderUser? = withContext(Dispatchers.IO){
        try {
            topcoderAPI.getUser(handle).execute().body()
        }catch (e: IOException){
            null
        }catch (e: SerializationException){
            null
        }
    }
}