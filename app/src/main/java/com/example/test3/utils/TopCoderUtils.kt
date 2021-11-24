package com.example.test3.utils

import com.example.test3.account_manager.STATUS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException

object TopCoderUtils {
    private const val pageSize = 64
    suspend fun findRankPageForHandle(targetHandle: String): Triple<STATUS, Int, Int> {
        var from = 1
        var to = 3000
        while (from < to) {
            val mid = (from + to) / 2
            val usersInfo = getRankPage(mid) ?: return Triple(STATUS.FAILED, 0, 0)
            if(usersInfo.isEmpty()) {
                to = mid
                continue
            }

            val firstHandle = usersInfo.first().first
            val lastHandle = usersInfo.last().first
            if(targetHandle.compareTo(firstHandle,true) < 0) to = mid
            else {
                if(targetHandle.compareTo(lastHandle,true) <= 0) {
                    val pos = usersInfo.indexOfFirst { targetHandle.equals(it.first,true) }
                    if (pos == -1) break
                    return Triple(STATUS.OK, usersInfo[pos].second, mid+pos)
                }
                from = mid + pageSize
            }
        }
        return Triple(STATUS.NOT_FOUND, 0, 0)
    }

    suspend fun getRankPage(from: Int): List<Pair<String,Int>>? {
        val s = TopCoderAPI.getRankingPage(from, pageSize)?.body()?.string() ?: return null
        val result = mutableListOf<Pair<String,Int>>()
        try {
            var i = 0
            while(true){
                i = s.indexOf("tc?module=MemberProfile", i+1)
                if (i==-1) break
                i = s.indexOf('>', i)
                val handle = s.substring(i+1, s.indexOf('<',i))
                i = s.indexOf("<td class=\"valueR\"", i)
                val rating = s.substring(s.indexOf('>',i)+1, s.indexOf('<',i+1)).toInt()
                result.add(handle to rating)
            }
        } catch (e: RuntimeException) {
            return null
        }
        return result
    }
}

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

    private interface API {
        @GET("v2/users/{handle}")
        fun getUser(
            @Path("handle") handle: String
        ): Call<TopCoderUser>

        @GET("v3/members/{handle}/stats/history")
        fun getStatsHistory(
            @Path("handle") handle: String
        ): Call<TopCoderAPIv3Response<List<TopCoderUserStatsHistory>>>
    }

    private val api = createRetrofitWithJson<API>("https://api.topcoder.com/")

    suspend fun getUser(handle: String): TopCoderUser? = withContext(Dispatchers.IO){
        try {
            api.getUser(handle).execute().body()
        }catch (e: IOException){
            null
        }catch (e: SerializationException){
            null
        }
    }

    suspend fun getStatsHistory(handle: String): TopCoderAPIv3Result<List<TopCoderUserStatsHistory>>? = withContext(Dispatchers.IO){
        try {
            api.getStatsHistory(handle).execute().body()?.result
        }catch (e: IOException){
            null
        }catch (e: SerializationException){
            null
        }
    }

    private interface WEB {
        @GET("/tc?module=AlgoRank&sc=4&sd=asc")
        fun algoRankPage(
            @Query("nr") pageSize: Int,
            @Query("sr") from: Int
        ): Call<ResponseBody>
    }

    private val web = createRetrofit<WEB>("https://www.topcoder.com/")

    suspend fun getRankingPage(from: Int, size: Int): Response<ResponseBody>? = withContext(Dispatchers.IO) {
        try {
            web.algoRankPage(size, from).execute()
        } catch (e: IOException) {
            null
        }
    }
}