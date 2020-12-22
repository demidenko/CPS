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

@Serializable
data class TopCoderAPIv3Response<T>(
    val result: TopCoderAPIv3Result<T>
)

@Serializable
data class TopCoderAPIv3Result<T>(
    val success: Boolean,
    val status: Int,
    val content: T
)

@Serializable
data class TopCoderRatingChange(
    val rating: Double,
    val placement: Int,
    val date: String,
    val challengeId: Int,
    val challengeName: String
)

@Serializable
data class TopCoderUserStatsHistory(
    val handle: String,
    val DATA_SCIENCE: TopCoderUserStatsHistoryDataScience
)

@Serializable
data class TopCoderUserStatsHistoryDataScience(
    val SRM: TopCoderRatingChanges,
    val MARATHON_MATCH: TopCoderRatingChanges,
){
    @Serializable
    data class TopCoderRatingChanges(
        val history: List<TopCoderRatingChange>
    )
}


object TopCoderAPI {
    interface API {
        @GET("v2/users/{handle}")
        fun getUser(
            @Path("handle") handle: String
        ): Call<TopCoderUser>

        @GET("v3/members/{handle}/stats/history")
        fun getStatsHistory(
            @Path("handle") handle: String
        ): Call<TopCoderAPIv3Response<List<TopCoderUserStatsHistory>>>
    }

    private val topcoderAPI = Retrofit.Builder()
        .baseUrl("https://api.topcoder.com/")
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

    suspend fun getStatsHistory(handle: String): TopCoderAPIv3Result<List<TopCoderUserStatsHistory>>? = withContext(Dispatchers.IO){
        try {
            topcoderAPI.getStatsHistory(handle).execute().body()?.result
        }catch (e: IOException){
            null
        }catch (e: SerializationException){
            null
        }
    }
}