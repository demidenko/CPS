package com.example.test3.utils

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.text.set
import com.example.test3.*
import com.example.test3.account_manager.*
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object CodeforcesUtils {

    suspend fun getBlogCreationTimeSeconds(blogId: Int): Long {
        return CodeforcesAPI.getBlogEntry(blogId)?.result?.creationTimeSeconds ?: return 0L
    }

    private val dateFormatRU = SimpleDateFormat("dd.MM.yyyy hh:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }
    private val dateFormatEN = SimpleDateFormat("MMM/dd/yyyy hh:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }
    fun parseRecentActionsPage(s: String): Pair<List<CodeforcesBlogEntry>,List<CodeforcesRecentAction>> {
        var dateFormat = dateFormatRU

        val comments = mutableListOf<CodeforcesRecentAction>()

        var i = 0
        while(true){
            i = s.indexOf("<table class=\"comment-table\">", i+1)
            if(i==-1) break

            i = s.indexOf("class=\"rated-user", i)
            val commentatorHandleColor = s.substring(s.indexOf(' ',i)+1, s.indexOf('"',i+10))

            i = s.lastIndexOf("/profile/",i)
            val commentatorHandle = s.substring(s.indexOf('/',i+1)+1, s.indexOf('"',i))

            i = s.indexOf("#comment-", i)
            val commentId = s.substring(s.indexOf('-',i)+1, s.indexOf('"',i)).toLong()

            val blogId = s.substring(s.lastIndexOf('/',i)+1, i).toInt()

            val blogTitle = fromHTML(s.substring(s.indexOf('>',i)+1, s.indexOf("</a>",i))).toString()

            i = s.lastIndexOf("class=\"rated-user", i)
            val blogAuthorHandleColor = s.substring(s.indexOf(' ',i)+1, s.indexOf('"',i+10))

            i = s.lastIndexOf("/profile/",i)
            val blogAuthorHandle = s.substring(s.indexOf('/',i+1)+1, s.indexOf('"',i))

            i = s.indexOf("<span class=\"format-humantime\"", i)
            i = s.indexOf('>', i)
            val commentTime = s.substring(s.lastIndexOf('"',i-2)+1, i-1)
            val commentTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(
                try{
                    dateFormat.parse(commentTime).time
                }catch (e : ParseException){
                    dateFormat = if(dateFormat == dateFormatRU) dateFormatEN else dateFormatRU
                    dateFormat.parse(commentTime).time
                }
            )

            i = s.indexOf("<div class=\"ttypography\">", i)
            val commentText = try{
                s.substring(s.indexOf(">",i)+1, s.indexOf("</div>",i))
            }catch (e: Exception){
                e.toString()
            }

            i = s.lastIndexOf("<span commentid=\"$commentId\">")
            i = s.indexOf("</span>", i)
            val commentRating = s.substring(s.lastIndexOf('>',i)+1, i).toInt()

            comments.add(
                CodeforcesRecentAction(
                    timeSeconds = commentTimeSeconds,
                    comment = CodeforcesComment(
                        id = commentId,
                        commentatorHandle = commentatorHandle,
                        commentatorHandleColorTag = commentatorHandleColor,
                        text = commentText,
                        rating = commentRating,
                        creationTimeSeconds = commentTimeSeconds
                    ),
                    blogEntry = CodeforcesBlogEntry(
                        id = blogId,
                        title = blogTitle,
                        authorHandle = blogAuthorHandle,
                        authorColorTag = blogAuthorHandleColor,
                        creationTimeSeconds = 0
                    )
                )
            )
        }

        val blogs = mutableListOf<CodeforcesBlogEntry>()

        i = s.indexOf("<div class=\"recent-actions\">")
        while(true){
            i = s.indexOf("<div style=\"font-size:0.9em;padding:0.5em 0;\">", i+1)
            if(i==-1) break

            i = s.indexOf("/profile/", i)
            val author = s.substring(i+9,s.indexOf('"',i))

            i = s.indexOf("rated-user user-",i)
            val authorColor = s.substring(s.indexOf(' ',i)+1, s.indexOf('"',i))

            i = s.indexOf("entry/", i)
            val id = s.substring(i+6, s.indexOf('"',i)).toInt()

            val title = fromHTML(s.substring(s.indexOf(">", i) + 1, s.indexOf("</a", i))).toString()

            blogs.add(
                CodeforcesBlogEntry(
                    id = id,
                    title = title,
                    authorHandle = author,
                    authorColorTag = authorColor,
                    creationTimeSeconds = 0L
                )
            )
        }

        return Pair(blogs, comments)
    }

    fun fromCodeforcesHTML(str: String): Spanned {
        var s = str
        s = s.replace("<code>", "<font face=monospace>").replace("</code>", "</font>")
        s = s.replace("\n", "<br/>")
        val res = fromHTML(s)
        return res.trimEnd() as Spanned
    }

    fun getTagByRating(rating: Int): String {
        return when {
            rating == NOT_RATED -> "user-black"
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

    fun getHandleColorByTag(tag: String, manager: CodeforcesAccountManager): Int? {
        return when (tag) {
            "user-gray" -> HandleColor.GRAY
            "user-green" -> HandleColor.GREEN
            "user-cyan" -> HandleColor.CYAN
            "user-blue" -> HandleColor.BLUE
            "user-violet" -> HandleColor.VIOLET
            "user-orange" -> HandleColor.ORANGE
            "user-red", "user-legendary" -> HandleColor.RED
            else -> null
        }?.getARGB(manager)
    }

    fun makeSpan(handle: String, tag: String, manager: CodeforcesAccountManager) = SpannableString(handle).apply {
        getHandleColorByTag(tag, manager)?.let {
            set(
                if(tag=="user-legendary") 1 else 0,
                handle.length,
                ForegroundColorSpan(it)
            )
        }
        if(tag!="user-black") set(0, handle.length, StyleSpan(Typeface.BOLD))
    }

    suspend fun getUsersInfo(handlesList: List<String>): List<CodeforcesAccountManager.CodeforcesUserInfo> {
        val handles = handlesList.toMutableList()
        while(true){
            val response = CodeforcesAPI.getUsers(handles) ?: break
            if(response.status == CodeforcesAPIStatus.FAILED){
                val comment = response.comment
                val badHandle = comment.removeSurrounding("handles: User with handle ", " not found")
                if(badHandle != comment){
                    handles.remove(badHandle)
                    continue
                }
                break
            }
            return response.result?.map { codeforcesUser ->
                CodeforcesAccountManager.CodeforcesUserInfo(
                    status = STATUS.OK,
                    handle = codeforcesUser.handle,
                    rating = codeforcesUser.rating
                )
            } ?: emptyList()
        }
        return handles.map { handle ->
            CodeforcesAccountManager.CodeforcesUserInfo(
                status = STATUS.FAILED,
                handle = handle
            )
        }
    }


}

enum class CodeforcesAPIStatus{
    OK, FAILED
}

@Serializable
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

@Serializable
data class CodeforcesAPIErrorResponse(
    val status: CodeforcesAPIStatus,
    val comment: String
)

@Serializable
data class CodeforcesUser(
    val handle: String,
    val rating: Int = NOT_RATED,
    val contribution: Int = 0
)

@Serializable
data class CodeforcesContest(
    val id: Int,
    val name: String,
    val phase: CodeforcesContestPhase,
    val type: CodeforcesContestType,
    val durationSeconds: Long,
    val startTimeSeconds: Long,
    val relativeTimeSeconds: Long
)

@Serializable
data class CodeforcesContestStandings(
    val contest: CodeforcesContest,
    val problems: List<CodeforcesProblem>,
    val rows: List<CodeforcesContestStandingsRow>
){
    @Serializable
    data class CodeforcesContestStandingsRow(
        val rank: Int,
        val points: Double,
        val party: CodeforcesContestParticipant,
        val problemResults: List<CodeforcesProblemResult>
    )

    @Serializable
    data class CodeforcesContestParticipant(
        val contestId: Int,
        val participantType: CodeforcesParticipationType,
        val members: List<CodeforcesUser>
    )
}

@Serializable
data class CodeforcesProblem(
    val name: String,
    val index: String,
    val points: Double = 0.0
)

@Serializable
data class CodeforcesProblemResult(
    val points: Double,
    val type: CodeforcesProblemStatus,
    val rejectedAttemptCount: Int
)

@Serializable
data class CodeforcesSubmission(
    val contestId: Int,
    val problem: CodeforcesProblem,
    val author: CodeforcesContestStandings.CodeforcesContestParticipant,
    val verdict: CodeforcesProblemVerdict = CodeforcesProblemVerdict.WAITING,
    val passedTestCount: Int,
    val id: Long,
    val testset: CodeforcesTestset
){
    fun makeVerdict(): String {
        if(verdict == CodeforcesProblemVerdict.OK) return "OK"
        return "${verdict.name} #${passedTestCount+1}"
    }
}


@Serializable
data class CodeforcesBlogEntry(
    val id: Int,
    val title: String,
    val authorHandle: String,
    val creationTimeSeconds: Long,
    val rating: Int = 0,
    val authorColorTag: String = ""
)

@Serializable
data class CodeforcesRatingChange(
    val contestId: Int,
    val handle: String,
    val rank: Int,
    val oldRating: Int,
    val newRating: Int,
    val ratingUpdateTimeSeconds: Long
)

@Serializable
data class CodeforcesComment(
    val id: Long,
    val creationTimeSeconds: Long,
    val commentatorHandle: String,
    val text: String,
    val rating: Int,
    val commentatorHandleColorTag: String = ""
)

@Serializable
data class CodeforcesRecentAction(
    val timeSeconds: Long,
    val blogEntry: CodeforcesBlogEntry? = null,
    val comment: CodeforcesComment? = null
)


object CodeforcesAPI {

    private const val callLimitExceededWaitTimeMillis: Long = 300

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
            @Query("blogEntryId") blogId: Int,
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

        @GET("contest.ratingChanges")
        fun getContestRatingChanges(
            @Query("contestId") contestId: Int
        ): Call<CodeforcesAPIResponse<List<CodeforcesRatingChange>>>

        @GET("user.rating")
        fun getUserRatingChanges(
            @Query("handle") handle: String
        ): Call<CodeforcesAPIResponse<List<CodeforcesRatingChange>>>
    }

    private val api = Retrofit.Builder()
        .baseUrl("https://codeforces.com/api/")
        .addConverterFactory(jsonConverterFactory)
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
                val er = jsonCPS.decodeFromString<CodeforcesAPIErrorResponse>(s)
                if(er.comment == "Call limit exceeded"){
                    delay(callLimitExceededWaitTimeMillis)
                    c = c.clone()
                    continue
                }
                return@withContext CodeforcesAPIResponse<T>(er)
            }catch (e : IOException){
                return@withContext null
            }catch (e: SerializationException){
                return@withContext null
            }
        }
        null
    }

    suspend fun getContests() = makeAPICall(api.getContests())

    suspend fun getUsers(handles: Collection<String>) = makeAPICall(api.getUser(handles.joinToString(separator = ";")))
    suspend fun getUser(handle: String) = getUsers(listOf(handle))?.let { CodeforcesAPIResponse(it.status, it.result?.get(0), it.comment) }

    suspend fun getBlogEntry(blogId: Int) = makeAPICall(api.getBlogEntry(blogId))

    suspend fun getContestStandings(contestId: Int, handles: Collection<String>, showUnofficial: Boolean) = makeAPICall(api.getContestStandings(contestId, handles.joinToString(separator = ";"), showUnofficial))
    suspend fun getContestStandings(contestId: Int, handle: String, showUnofficial: Boolean) = getContestStandings(contestId, listOf(handle), showUnofficial)

    suspend fun getContestSubmissions(contestId: Int, handle: String) = makeAPICall(api.getContestStatus(contestId, handle))

    suspend fun getUserBlogEntries(handle: String) = makeAPICall(api.getUserBlogs(handle))

    suspend fun getContestRatingChanges(contestId: Int) = makeAPICall(api.getContestRatingChanges(contestId))

    suspend fun getUserRatingChanges(handle: String) = makeAPICall(api.getUserRatingChanges(handle))


    interface WEB {
        @GET("data/handles")
        fun getHandleSuggestions(
            @Query("q") prefix: String,
            @Header("Cookie") cookie: String = "RCPC=$RCPC"
        ): Call<ResponseBody>

        @GET("{page}")
        fun getPage(
            @Path("page", encoded = true) page: String,
            @Query("locale") lang: String,
            @Header("Cookie") cookie: String = "RCPC=$RCPC"
        ): Call<ResponseBody>
    }

    private val web = Retrofit.Builder()
        .baseUrl("https://codeforces.com/")
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(httpClient)
        .build()
        .create(WEB::class.java)


    private val RCPC = object {

        private var rcpc_value: String = ""

        override fun toString(): String = rcpc_value

        private var last_c = ""
        fun recalc(source: String) = runBlocking {
            val i = source.indexOf("c=toNumbers(")
            val c = source.substring(source.indexOf("(\"",i)+2, source.indexOf("\")",i))
            if(c == last_c) return@runBlocking
            rcpc_value = decodeAES(c)
            last_c = c
        }
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
            RCPC.recalc(s)
            delay(300)
            s = invoker() ?: return@withContext null
        }
        return@withContext s
    }

    suspend fun getHandleSuggestions(str: String) = makeWEBCall(CallStringInvoker { web.getHandleSuggestions(str) })

    suspend fun getPageSource(page: String, lang: String) = makeWEBCall(CallStringInvoker { web.getPage(page,lang) })

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

enum class CodeforcesTestset {
    SAMPLES, PRETESTS, TESTS, CHALLENGES,
    TESTS1, TESTS2, TESTS3, TESTS4, TESTS5, TESTS6, TESTS7, TESTS8, TESTS9, TESTS10
}

object CodeforcesURLFactory {

    private const val main = "https://codeforces.com"

    fun user(handle: String) = "$main/profile/$handle"

    fun blog(blogId: Int) = "$main/blog/entry/$blogId"

    fun userBlogs(handle: String) = "$main/blog/$handle"

    fun comment(blogId: Int, commentId: Long) = blog(blogId) + "#comment-$commentId"

    fun contest(contestId: Int) = "$main/contest/$contestId"

    fun contestsWith(handle: String) = "$main/contests/with/$handle"

    fun submission(submission: CodeforcesSubmission) = "$main/contest/${submission.contestId}/submission/${submission.id}"
}