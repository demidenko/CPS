package com.example.test3.utils

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.text.set
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.ColoredHandles
import com.example.test3.account_manager.HandleColor
import com.example.test3.account_manager.NOT_RATED
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.net.SocketTimeoutException

object CodeforcesUtils : ColoredHandles {

    suspend fun getBlogCreationTimeSeconds(blogID: String): Long {
        return CodeforcesAPI.getBlogEntry(blogID.toInt())?.result?.creationTimeSeconds ?: return 0L
    }




    override fun getHandleColor(rating: Int): HandleColor {
        return when {
            rating < 1200 -> HandleColor.GRAY
            rating < 1400 -> HandleColor.GREEN
            rating < 1600 -> HandleColor.CYAN
            rating < 1900 -> HandleColor.BLUE
            rating < 2100 -> HandleColor.VIOLET
            rating < 2400 -> HandleColor.ORANGE
            else -> HandleColor.RED
        }
    }

    override fun getColor(tag: HandleColor): Int {
        return when (tag){
            HandleColor.GRAY -> 0x808080
            HandleColor.GREEN -> 0x008000
            HandleColor.CYAN -> 0x03A89E
            HandleColor.BLUE -> 0x0000FF
            HandleColor.VIOLET -> 0xAA00AA
            HandleColor.ORANGE -> 0xFF8C00
            HandleColor.RED -> 0xFF0000
            else -> throw HandleColor.UnknownHandleColorException(tag)
        }
    }

    fun getTagByRating(rating: Int): String {
        return when {
            rating < 1200 -> "user-gray"
            rating < 1400 -> "user-green"
            rating < 1600 -> "user-cyan"
            rating < 1900 -> "user-blue"
            rating < 2100 -> "user-violet"
            rating < 2400 -> "user-orange"
            rating < 3000 -> "user-red"
            else -> "user-legendary"
        }
    }

    fun getHandleColorByTag(tag: String): Int? {
        return when (tag) {
            "user-gray" -> HandleColor.GRAY
            "user-green" -> HandleColor.GREEN
            "user-cyan" -> HandleColor.CYAN
            "user-blue" -> HandleColor.BLUE
            "user-violet" -> HandleColor.VIOLET
            "user-orange" -> HandleColor.ORANGE
            "user-red", "user-legendary" -> HandleColor.RED
            else -> null
        }?.getARGB(this)
    }

    fun makeSpan(handle: String, tag: String) = SpannableString(handle).apply {
        getHandleColorByTag(tag)?.let {
            set(
                if(tag=="user-legendary") 1 else 0,
                handle.length,
                ForegroundColorSpan(it)
            )
        }
        if(tag!="user-black") set(0, handle.length, StyleSpan(Typeface.BOLD))
    }

    fun makeSpan(info: CodeforcesAccountManager.CodeforcesUserInfo) = makeSpan(info.handle, getTagByRating(info.rating))
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
    val creationTimeSeconds: Long,
    val rating: Int = 0,
    val authorColorTag: String = ""
){
    companion object {
        val jsonAdapter = CodeforcesBlogEntryJsonAdapter(Moshi.Builder().build())
    }
}


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

    private val api = Retrofit.Builder()
        .baseUrl("https://codeforces.com/api/")
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(httpClient)
        .build()
        .create(API::class.java)

    private suspend fun <T> makeAPICall(call: Call<CodeforcesAPIResponse<T>>): CodeforcesAPIResponse<T>? = withContext(Dispatchers.IO){
        var c = call
        while(true){
            try{
                val r = c.execute()
                if(r.isSuccessful) return@withContext r.body()
                val s = r.errorBody()?.string() ?: return@withContext null
                val er: CodeforcesAPIErrorResponse = CodeforcesAPIErrorResponse.jsonAdapter.fromJson(s) ?: return@withContext null
                if(er.comment == "Call limit exceeded"){
                    delay(500)
                    c = c.clone()
                    continue
                }
                return@withContext CodeforcesAPIResponse<T>(er)
            }catch (e : IOException){
                return@withContext null
            }
        }
        null
    }

    suspend fun getContests() = makeAPICall(api.getContests())

    suspend fun getUsers(handles: Collection<String>) = makeAPICall(api.getUser(handles.joinToString(separator = ";")))
    suspend fun getUser(handle: String) = getUsers(listOf(handle))?.let { CodeforcesAPIResponse(it.status, it.result?.get(0), it.comment) }

    suspend fun getBlogEntry(blogID: Int) = makeAPICall(api.getBlogEntry(blogID))

    suspend fun getContestStandings(contestID: Int, handles: Collection<String>, showUnofficial: Boolean) = makeAPICall(api.getContestStandings(contestID, handles.joinToString(separator = ";"), showUnofficial))
    suspend fun getContestStandings(contestID: Int, handle: String, showUnofficial: Boolean) = getContestStandings(contestID, listOf(handle), showUnofficial)

    suspend fun getContestSubmissions(contestID: Int, handle: String) = makeAPICall(api.getContestStatus(contestID, handle))

    suspend fun getUserBlogEntries(handle: String) = makeAPICall(api.getUserBlogs(handle))



    interface WEB {
        @GET("data/handles")
        fun getHandleSuggestions(
            @Query("q") prefix: String,
            @Header("Cookie") cookie: String = "RCPC=$RCPC"
        ): Call<ResponseBody>

        @GET("{page}")
        fun getPage(
            @Path("page") page: String,
            @Query("locale") lang: String,
            @Header("Cookie") cookie: String = "RCPC=$RCPC"
        ): Call<ResponseBody>

        @GET("contest/{contestID}")
        fun getContestPage(
            @Path("contestID") contestID: Int,
            @Header("Cookie") cookie: String = "RCPC=$RCPC"
        ): Call<ResponseBody>
    }

    private val web = Retrofit.Builder()
        .baseUrl("https://codeforces.com/")
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(httpClient)
        .build()
        .create(WEB::class.java)


    private var RCPC = ""
    private var last_c = ""
    private fun recalcRCPC(source: String) = runBlocking {
        val i = source.indexOf("c=toNumbers(")
        val c = source.substring(source.indexOf("(\"",i)+2, source.indexOf("\")",i))
        //println("c = $c")
        if(c == last_c) return@runBlocking
        RCPC = decodeAES(c)
        last_c = c
        //println("new RCPC = $RCPC")
    }

    class CallStringInvoker(val block: ()->Call<ResponseBody> ){
        operator fun invoke(): String? {
            try {
                return block().execute().body()?.string()
            }catch (e: SocketTimeoutException){
                return null
            }catch (e: IOException){
                return null
            }
        }
    }

    private suspend fun makeWEBCall(invoker: CallStringInvoker):String? = withContext(Dispatchers.IO) {
        var s = invoker() ?: return@withContext null
        if (s.startsWith("<html><body>Redirecting... Please, wait.")) {
            recalcRCPC(s)
            delay(300)
            s = invoker() ?: return@withContext null
        }
        return@withContext s
    }

    suspend fun getHandleSuggestions(str: String) = makeWEBCall(CallStringInvoker { web.getHandleSuggestions(str) })

    suspend fun getPageSource(page: String, lang: String) = makeWEBCall(CallStringInvoker { web.getPage(page,lang) })

    suspend fun getContestPageSource(contestID: Int) = makeWEBCall(CallStringInvoker { web.getContestPage(contestID) })
}


enum class CodeforcesContestPhase(private val title: String? = null) {
    UNDEFINED,
    BEFORE,
    CODING,
    PENDING_SYSTEM_TEST("PENDING SYSTEM TESTING"),
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