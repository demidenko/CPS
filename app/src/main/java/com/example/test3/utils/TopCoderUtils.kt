package com.example.test3.utils

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.IOException

@JsonClass(generateAdapter = true)
data class TopCoderUser(
    val handle: String = "",
    val ratingSummary: List<TopCoderRatingSummary> = emptyList(),
    val error: TopCoderError? = null
){
    @JsonClass(generateAdapter = true)
    data class TopCoderRatingSummary(
        val name: String,
        val rating: Int
    )

    @JsonClass(generateAdapter = true)
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
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(httpClient)
        .build()
        .create(API::class.java)

    suspend fun getUser(handle: String): TopCoderUser? {
        try {
            return topcoderAPI.getUser(handle).execute().body()
        }catch (e: IOException){
            return null
        }catch (e: JsonDataException){
            return null
        }
    }
}