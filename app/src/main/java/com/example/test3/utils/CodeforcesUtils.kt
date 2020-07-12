package com.example.test3.utils

import com.example.test3.account_manager.NOT_RATED
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
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

    suspend fun getBlogCreationTimeMillis(blogID: String): Long {
        val response = CodeforcesAPI.getBlogEntry(blogID.toInt()) ?: return 0L
        val blogInfo = response.result ?: return 0L
        return blogInfo.creationTimeSeconds * 1000
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
        //val jsonAdapter: JsonAdapter<CodeforcesAPIErrorResponse> = Moshi.Builder().build().adapter(CodeforcesAPIErrorResponse::class.java)
        val jsonAdapter = CodeforcesAPIErrorResponseJsonAdapter(Moshi.Builder().build())
    }
}

@JsonClass(generateAdapter = true)
data class CodeforcesUser(
    val handle: String,
    val rating: Int = NOT_RATED,
    val contribution: Int = 0
)

@JsonClass(generateAdapter = true)
data class CodeforcesContest(
    val id: Int,
    val name: String,
    val phase: CodeforcesContestPhase,
    val type: CodeforcesContestType,
    val durationSeconds: Long,
    val startTimeSeconds: Long,
    val relativeTimeSeconds: Long
)

@JsonClass(generateAdapter = true)
data class CodeforcesContestStandings(
    val contest: CodeforcesContest,
    val problems: List<CodeforcesProblem>,
    val rows: List<CodeforcesContestStandingsRow>
){
    @JsonClass(generateAdapter = true)
    data class CodeforcesContestStandingsRow(
        val rank: Int,
        val points: Double,
        val party: CodeforcesContestParticipant,
        val problemResults: List<CodeforcesProblemResult>
    )

    @JsonClass(generateAdapter = true)
    data class CodeforcesContestParticipant(
        val contestId: Int,
        val participantType: CodeforcesParticipationType,
        val members: List<CodeforcesUser>
    )
}

@JsonClass(generateAdapter = true)
data class CodeforcesProblem(
    val name: String,
    val index: String,
    val points: Int = 0
)

@JsonClass(generateAdapter = true)
data class CodeforcesProblemResult(
    val points: Double,
    val type: CodeforcesProblemStatus,
    val rejectedAttemptCount: Int
)

@JsonClass(generateAdapter = true)
data class CodeforcesSubmission(
    val contestId: Int,
    val problem: CodeforcesProblem,
    val author: CodeforcesContestStandings.CodeforcesContestParticipant,
    val verdict: CodeforcesProblemVerdict = CodeforcesProblemVerdict.WAITING,
    val passedTestCount: Int,
    val id: Long,
    val testset: String
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
        @GET("contest.list")
        fun getContests(
            @Query("locale") lang: String = "en",
            @Query("gym") gym: Boolean = false
        ): Call<CodeforcesAPIResponse<List<CodeforcesContest>>>

        @GET("user.info")
        fun getUser(
            @Query("handles") handles: String
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

        @GET("contest.standings")
        fun getContestStandings(
            @Query("contestId") contestId: Int,
            @Query("handles") handles: String,
            @Query("showUnofficial") showUnofficial: Boolean
            //@Query("count") count: Int = 10,
            //@Query("from") from: Int = 1
        ): Call<CodeforcesAPIResponse<CodeforcesContestStandings>>

        @GET("contest.status")
        fun getContestStatus(
            @Query("contestId") contestId: Int,
            @Query("handle") handle: String,
            @Query("count") count: Int = 1000000000
        ): Call<CodeforcesAPIResponse<List<CodeforcesSubmission>>>
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

        @GET("contest/{contestID}")
        fun getContestPage(
            @Path("contestID") contestID: Int
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

    suspend fun getContests() = withContext(Dispatchers.IO){ makeCall(api.getContests()) }

    suspend fun getUsers(handles: Collection<String>) = withContext(Dispatchers.IO){ makeCall(api.getUser(handles.joinToString(separator = ";"))) }
    suspend fun getUser(handle: String) = getUsers(listOf(handle))

    suspend fun getBlogEntry(blogID: Int) = withContext(Dispatchers.IO){ makeCall(api.getBlogEntry(blogID)) }

    suspend fun getContestStandings(contestID: Int, handles: Collection<String>, showUnofficial: Boolean) = withContext(Dispatchers.IO){ makeCall(api.getContestStandings(contestID, handles.joinToString(separator = ";"), showUnofficial)) }
    suspend fun getContestStandings(contestID: Int, handle: String, showUnofficial: Boolean) = getContestStandings(contestID, listOf(handle), showUnofficial)

    suspend fun getContestSubmissions(contestID: Int, handle: String) = withContext(Dispatchers.IO){ makeCall(api.getContestStatus(contestID, handle)) }

    suspend fun getUserBlogEntries(handle: String) = withContext(Dispatchers.IO){ makeCall(api.getUserBlogs(handle)) }

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

    suspend fun getContestPageSource(contestID: Int): String? {
        try {
            return withContext(Dispatchers.IO){
                web.getContestPage(contestID).execute().body()?.string()
            }
        }catch (e: IOException){
            return null
        }
    }
}


enum class CodeforcesContestPhase(private val title: String? = null) {
    UNDEFINED,
    BEFORE,
    CODING,
    PENDING_SYSTEM_TEST("WAITING SYSTEM TESTING"),
    SYSTEM_TEST("SYSTEM TESTING"),
    FINISHED;

    fun getTitle(): String = title ?: name

    fun isFutureOrRunning(): Boolean {
        return this != UNDEFINED && this != FINISHED
    }
}

enum class CodeforcesContestType {
    UNDEFINED,
    CF, ICPC, IOI
}

enum class CodeforcesParticipationType {
    NOT_PARTICIPATED,
    CONTESTANT, PRACTICE, VIRTUAL, MANAGER, OUT_OF_COMPETITION;

    fun participatedInContest(): Boolean = (this == CONTESTANT || this == OUT_OF_COMPETITION)
}

enum class CodeforcesProblemStatus {
    FINAL, PRELIMINARY
}

enum class CodeforcesProblemVerdict {
    WAITING,
    FAILED, OK, PARTIAL, COMPILATION_ERROR, RUNTIME_ERROR, WRONG_ANSWER, PRESENTATION_ERROR, TIME_LIMIT_EXCEEDED, MEMORY_LIMIT_EXCEEDED, IDLENESS_LIMIT_EXCEEDED, SECURITY_VIOLATED, CRASHED, INPUT_PREPARATION_CRASHED, CHALLENGED, SKIPPED, TESTING, REJECTED
}