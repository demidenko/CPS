package com.example.test3.utils

import android.content.Context
import android.text.Spanned
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.test3.*
import com.example.test3.account_manager.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import okhttp3.ResponseBody
import retrofit2.Call
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

    suspend fun getBlogCreationTime(blogId: Int): Instant {
        return CodeforcesAPI.getBlogEntry(blogId,CodeforcesLocale.RU)?.result?.let {
            Instant.fromEpochSeconds(it.creationTimeSeconds)
        } ?: Instant.DISTANT_PAST
    }

    private val dateFormatRU = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }
    private val dateFormatEN = SimpleDateFormat("MMM/dd/yyyy HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }

    private fun parseTimeString(str: String): Long {
        val parser = if(str.contains('.')) dateFormatRU else dateFormatEN
        return try {
            TimeUnit.MILLISECONDS.toSeconds(parser.parse(str).time)
        } catch (e: ParseException) {
            0L
        }
    }

    fun parseBlogEntriesPage(s: String): List<CodeforcesBlogEntry> {
        val res = mutableListOf<CodeforcesBlogEntry>()
        var i = 0
        while (true) {
            i = s.indexOf("<div class=\"topic\"", i + 1)
            if (i == -1) break

            val title = fromHTML(s.substring(s.indexOf("<p>", i) + 3, s.indexOf("</p>", i))).toString()

            i = s.indexOf("entry/", i)
            val id = s.substring(i+6, s.indexOf('"',i)).toInt()

            i = s.indexOf("<div class=\"info\"", i)
            i = s.indexOf("/profile/", i)
            val author = s.substring(i+9,s.indexOf('"',i))

            i = s.indexOf("rated-user user-",i)
            val authorColorTag = ColorTag.fromString(
                s.substring(s.indexOf(' ',i)+1, s.indexOf('"',i))
            )

            i = s.indexOf("<span class=\"format-humantime\"", i)
            i = s.indexOf('>', i)
            val time = parseTimeString(s.substring(s.lastIndexOf('"',i-2)+1, i-1))

            i = s.indexOf("<div class=\"roundbox meta\"", i)
            i = s.indexOf("</span>", i)
            val rating = s.substring(s.lastIndexOf('>',i-1)+1,i).toInt()

            i = s.indexOf("<div class=\"right-meta\">", i)
            i = s.indexOf("</ul>", i)
            i = s.lastIndexOf("</a>", i)
            val comments = s.substring(s.lastIndexOf('>',i-1)+1,i).trim().toInt()

            res.add(
                CodeforcesBlogEntry(
                    id = id,
                    title = title,
                    authorHandle = author,
                    authorColorTag = authorColorTag,
                    creationTimeSeconds = time,
                    commentsCount = comments,
                    rating = rating
                )
            )
        }

        return res
    }

    fun parseCommentsPage(s: String): List<CodeforcesRecentAction> {
        val comments = mutableListOf<CodeforcesRecentAction>()
        var i = 0
        while(true){
            i = s.indexOf("<table class=\"comment-table\">", i+1)
            if(i==-1) break

            i = s.indexOf("class=\"rated-user", i)
            val commentatorHandleColorTag = ColorTag.fromString(
                s.substring(s.indexOf(' ',i)+1, s.indexOf('"',i+10))
            )

            i = s.lastIndexOf("/profile/",i)
            val commentatorHandle = s.substring(s.indexOf('/',i+1)+1, s.indexOf('"',i))

            i = s.indexOf("#comment-", i)
            val commentId = s.substring(s.indexOf('-',i)+1, s.indexOf('"',i)).toLong()

            val blogId = s.substring(s.lastIndexOf('/',i)+1, i).toInt()

            val blogTitle = fromHTML(s.substring(s.indexOf('>',i)+1, s.indexOf("</a>",i))).toString()

            i = s.lastIndexOf("class=\"rated-user", i)
            val blogAuthorHandleColorTag = ColorTag.fromString(
                s.substring(s.indexOf(' ',i)+1, s.indexOf('"',i+10))
            )

            i = s.lastIndexOf("/profile/",i)
            val blogAuthorHandle = s.substring(s.indexOf('/',i+1)+1, s.indexOf('"',i))

            i = s.indexOf("<span class=\"format-humantime\"", i)
            i = s.indexOf('>', i)
            val commentTime = s.substring(s.lastIndexOf('"',i-2)+1, i-1)
            val commentTimeSeconds = parseTimeString(commentTime)

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
                        commentatorHandleColorTag = commentatorHandleColorTag,
                        text = commentText,
                        rating = commentRating,
                        creationTimeSeconds = commentTimeSeconds
                    ),
                    blogEntry = CodeforcesBlogEntry(
                        id = blogId,
                        title = blogTitle,
                        authorHandle = blogAuthorHandle,
                        authorColorTag = blogAuthorHandleColorTag,
                        creationTimeSeconds = 0
                    )
                )
            )
        }

        return comments
    }

    fun parseRecentBlogEntriesPage(s: String): List<CodeforcesBlogEntry> {
        val blogEntries = mutableListOf<CodeforcesBlogEntry>()

        var i = s.indexOf("<div class=\"recent-actions\">")
        while(true){
            i = s.indexOf("<div style=\"font-size:0.9em;padding:0.5em 0;\">", i+1)
            if(i==-1) break

            i = s.indexOf("/profile/", i)
            val author = s.substring(i+9,s.indexOf('"',i))

            i = s.indexOf("rated-user user-",i)
            val authorColorTag = ColorTag.fromString(
                s.substring(s.indexOf(' ',i)+1, s.indexOf('"',i))
            )

            i = s.indexOf("entry/", i)
            val id = s.substring(i+6, s.indexOf('"',i)).toInt()

            val title = fromHTML(s.substring(s.indexOf(">", i) + 1, s.indexOf("</a", i))).toString()

            blogEntries.add(
                CodeforcesBlogEntry(
                    id = id,
                    title = title,
                    authorHandle = author,
                    authorColorTag = authorColorTag,
                    creationTimeSeconds = 0L
                )
            )
        }

        return blogEntries
    }

    fun fromCodeforcesHTML(str: String): Spanned {
        var s = str
        s = s.replace("<code>", "<font face=monospace>").replace("</code>", "</font>")
        s = s.replace("\n", "<br/>")
        val res = fromHTML(s)
        return res.trimEnd() as? Spanned ?: res
    }

    enum class ColorTag {
        BLACK,
        GRAY,
        GREEN,
        CYAN,
        BLUE,
        VIOLET,
        ORANGE,
        RED,
        LEGENDARY,
        ADMIN;

        companion object {
            fun fromString(str: String): ColorTag =
                valueOf(str.removePrefix("user-").uppercase(Locale.ENGLISH))
        }
    }

    fun getTagByRating(rating: Int): ColorTag {
        return when {
            rating == NOT_RATED -> ColorTag.BLACK
            rating < 1200 -> ColorTag.GRAY
            rating < 1400 -> ColorTag.GREEN
            rating < 1600 -> ColorTag.CYAN
            rating < 1900 -> ColorTag.BLUE
            rating < 2100 -> ColorTag.VIOLET
            rating < 2400 -> ColorTag.ORANGE
            rating < 3000 -> ColorTag.RED
            else -> ColorTag.LEGENDARY
        }
    }

    fun getHandleColorByTag(tag: ColorTag): HandleColor? {
        return when (tag) {
            ColorTag.GRAY -> HandleColor.GRAY
            ColorTag.GREEN -> HandleColor.GREEN
            ColorTag.CYAN -> HandleColor.CYAN
            ColorTag.BLUE -> HandleColor.BLUE
            ColorTag.VIOLET -> HandleColor.VIOLET
            ColorTag.ORANGE -> HandleColor.ORANGE
            ColorTag.RED, ColorTag.LEGENDARY -> HandleColor.RED
            else -> null
        }
    }

    suspend fun getUsersInfo(handlesList: List<String>, doRedirect: Boolean = false): Map<String, CodeforcesUserInfo> {
        val res = handlesList.associateWith { handle ->
            CodeforcesUserInfo(STATUS.FAILED, handle)
        }.toMutableMap()

        val handles = handlesList.toMutableList()
        val realHandles = mutableMapOf<String,String>()
        while(handles.isNotEmpty()){
            val response = CodeforcesAPI.getUsers(handles) ?: break
            if(response.status == CodeforcesAPIStatus.FAILED){
                val badHandle = response.isHandleNotFound()
                if(badHandle != null){
                    if(doRedirect){
                        val (realHandle, status) = getRealHandle(badHandle)
                        when(status){
                            STATUS.OK -> {
                                realHandles[badHandle] = realHandle
                                handles[handles.indexOf(badHandle)] = realHandle
                            }
                            else -> {
                                handles.remove(badHandle)
                                res[badHandle]!!.status = status
                            }
                        }
                    }else{
                        handles.remove(badHandle)
                        res[badHandle]!!.status = STATUS.NOT_FOUND
                    }
                    continue
                }
                break
            }

            response.result?.let { resultList ->
                res.keys.forEach { handle ->
                    (realHandles[handle] ?: handle).let { realHandle ->
                        resultList.find { it.handle.equals(realHandle, true) }
                    }?.let { codeforcesUser ->
                        res[handle] = CodeforcesUserInfo(codeforcesUser)
                    }
                }
            }

            break
        }

        return res
    }

    suspend fun getRealHandle(handle: String): Pair<String, STATUS> {
        val page = CodeforcesAPI.getPageSource(CodeforcesURLFactory.user(handle), CodeforcesLocale.EN) ?: return handle to STATUS.FAILED
        val realHandle = extractRealHandle(page) ?: return handle to STATUS.NOT_FOUND
        return realHandle to STATUS.OK
    }

    private fun extractRealHandle(s: String): String? {
        var i = s.indexOf(" <div class=\"userbox\">")
        if(i == -1) return null
        i = s.indexOf("<div class=\"user-rank\">", i)
        i = s.indexOf("class=\"rated-user", i)
        return s.substring(s.indexOf('>', i)+1, s.indexOf("</a", i))
    }

    suspend fun getRealColorTag(handle: String): ColorTag = withContext(Dispatchers.IO) {
        CodeforcesAPI.getPageSource(CodeforcesURLFactory.user(handle), CodeforcesLocale.EN)
            ?.let { page -> extractRealColorTag(page) } ?: ColorTag.BLACK
    }

    private fun extractRealColorTag(s: String): ColorTag? {
        var i = s.indexOf(" <div class=\"userbox\">")
        if(i == -1) return null
        i = s.indexOf("<div class=\"user-rank\">", i)
        i = s.indexOf("class=\"rated-user", i)
        return ColorTag.fromString(
            s.substring(s.indexOf(' ',i)+1, s.indexOf('"',i+10))
        )
    }

    fun setVotedView(rating: Int, ratingTextView: TextView, ratingGroupView: View, context: Context = ratingTextView.context) {
        if(rating == 0) ratingGroupView.isVisible = false
        else {
            ratingTextView.text = signedToString(rating)
            ratingTextView.setTextColor(getColorFromResource(context,
                if(rating>0) R.color.voted_rating_positive
                else R.color.voted_rating_negative
            ))
            ratingGroupView.isVisible = true
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

    fun isHandleNotFound(): String? {
        val cut = comment.removeSurrounding("handles: User with handle ", " not found")
        if(cut == comment) return null
        return cut
    }

    fun isBlogNotFound(blogId: Int): Boolean {
        if(comment == "blogEntryId: Blog entry with id $blogId not found") return true
        if(comment == "Blog entry with id $blogId not found") return true
        return false
    }

    fun isBlogHandleNotFound(handle: String): Boolean {
        if(comment == "handle: User with handle $handle not found") return true
        if(comment == "handle: Field should contain between 3 and 24 characters, inclusive") return true
        if(comment == "handle: Поле должно содержать от 3 до 24 символов, включительно") return true
        return false
    }

    fun isNotAllowedToReadThatBlog(): Boolean {
        if(comment == "handle: You are not allowed to read that blog") return true
        return false
    }

    fun isContestRatingUnavailable(): Boolean {
        if(comment == "contestId: Rating changes are unavailable for this contest") return true
        return false
    }

    fun isContestNotStarted(contestId: Int): Boolean {
        if(comment == "contestId: Contest with id $contestId has not started") return true
        return false
    }
}

@Serializable
data class CodeforcesAPIErrorResponse(
    val status: CodeforcesAPIStatus,
    val comment: String
){
    fun isCallLimitExceeded() = comment == "Call limit exceeded"
}

@Serializable
data class CodeforcesUser(
    val handle: String,
    val rating: Int = NOT_RATED,
    val contribution: Int = 0,
    val lastOnlineTimeSeconds: Long = -1
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
    val creationTimeSeconds: Long,
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
    val commentsCount: Int = 0,
    val authorColorTag: CodeforcesUtils.ColorTag = CodeforcesUtils.ColorTag.BLACK
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
    val commentatorHandleColorTag: CodeforcesUtils.ColorTag = CodeforcesUtils.ColorTag.BLACK
)

@Serializable
data class CodeforcesRecentAction(
    val timeSeconds: Long,
    val blogEntry: CodeforcesBlogEntry? = null,
    val comment: CodeforcesComment? = null
)


object CodeforcesAPI {

    private const val callLimitExceededWaitTimeMillis: Long = 500

    private interface API {
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
            @Query("locale") lang: String
        ): Call<CodeforcesAPIResponse<CodeforcesBlogEntry>>

        @GET("user.blogEntries")
        fun getUserBlogs(
            @Query("handle") handle: String,
            @Query("locale") lang: String
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

        @GET("user.status")
        fun getUserStatus(
            @Query("handle") handle: String,
            @Query("count") count: Int = 10,
            @Query("from") from: Int = 1
        ): Call<CodeforcesAPIResponse<List<CodeforcesSubmission>>>
    }

    private val api = createRetrofitWithJson<API>("https://codeforces.com/api/")

    private tailrec suspend fun<T> makeAPICall(call: Call<CodeforcesAPIResponse<T>>, callLimit: Int): CodeforcesAPIResponse<T>? {
        if(callLimit == 0) return null
        val r = call.execute()
        if (r.isSuccessful) return r.body()
        if(r.code() == 503) { //mike wtf?
            delay(callLimitExceededWaitTimeMillis)
            return makeAPICall(call.clone(), callLimit-1)
        }
        val s = r.errorBody()?.string() ?: return null
        val er = jsonCPS.decodeFromString<CodeforcesAPIErrorResponse>(s)
        if (er.isCallLimitExceeded()) {
            delay(callLimitExceededWaitTimeMillis)
            return makeAPICall(call.clone(), callLimit-1)
        }
        return CodeforcesAPIResponse<T>(er)
    }

    private suspend fun <T> makeAPICall(call: Call<CodeforcesAPIResponse<T>>): CodeforcesAPIResponse<T>? = withContext(Dispatchers.IO){
        return@withContext try{
            makeAPICall(call, 10)
        }catch (e : IOException){
            null
        }catch (e: SerializationException){
            e.printStackTrace()
            null
        }
    }

    suspend fun getContests() = makeAPICall(api.getContests())

    suspend fun getUsers(handles: Collection<String>) = makeAPICall(api.getUser(handles.joinToString(separator = ";")))
    suspend fun getUser(handle: String) = getUsers(listOf(handle))?.let { CodeforcesAPIResponse(it.status, it.result?.get(0), it.comment) }

    suspend fun getBlogEntry(blogId: Int, locale: CodeforcesLocale) = makeAPICall(api.getBlogEntry(blogId,locale.toString()))

    suspend fun getContestStandings(contestId: Int, handles: Collection<String>, showUnofficial: Boolean) = makeAPICall(api.getContestStandings(contestId, handles.joinToString(separator = ";"), showUnofficial))
    suspend fun getContestStandings(contestId: Int, handle: String, showUnofficial: Boolean) = getContestStandings(contestId, listOf(handle), showUnofficial)

    suspend fun getContestSubmissions(contestId: Int, handle: String) = makeAPICall(api.getContestStatus(contestId, handle))

    suspend fun getUserBlogEntries(handle: String, locale: CodeforcesLocale) = makeAPICall(api.getUserBlogs(handle,locale.toString()))

    suspend fun getContestRatingChanges(contestId: Int) = makeAPICall(api.getContestRatingChanges(contestId))

    suspend fun getUserRatingChanges(handle: String) = makeAPICall(api.getUserRatingChanges(handle))

    suspend fun getUserSubmissions(handle: String, count: Int = 10, from: Int = 1) = makeAPICall(api.getUserStatus(handle,count,from))


    private interface WEB {
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

    private val web = createRetrofit<WEB>("https://codeforces.com/")

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

    suspend fun getPageSource(page: String, locale: CodeforcesLocale) = makeWEBCall(CallStringInvoker { web.getPage(page,locale.toString()) })

    suspend fun getContestSystemTestingPercentage(contestId: Int): Int? {
        val s = getPageSource("contest/$contestId", CodeforcesLocale.EN) ?: return null
        var i = s.indexOf("<span class=\"contest-state-regular\">")
        if (i != -1) {
            i = s.indexOf(">", i + 1)
            val progress = s.substring(i + 1, s.indexOf("</", i + 1))
            return progress.removeSuffix("%").toIntOrNull()
        }
        return null
    }

    suspend fun getContestProblemsAcceptedsCount(contestId: Int): Map<String, Int>? {
        val str = getPageSource("contest/$contestId", CodeforcesLocale.EN) ?: return null
        val cnt = mutableMapOf<String, Int>()
        var i = 0
        while (true) {
            i = str.indexOf("<td class=\"id\">", i+1)
            if(i==-1) break
            i = str.indexOf("href", i)
            val problemIndex = str.substring(str.indexOf(">", i)+1, str.indexOf("</a", i)).trim()
            val p = str.indexOf("/contest/$contestId/status/$problemIndex", i)
            val solvedBy: Int = if(p == -1) 0 else run {
                i = str.indexOf("</a", p)
                str.substring(str.lastIndexOf('x', i)+1, i).toInt()
            }
            cnt[problemIndex] = solvedBy
        }
        return cnt
    }
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

    fun comment(blogId: Int, commentId: Long) = blog(blogId) + "#comment-$commentId"

    fun contest(contestId: Int) = "$main/contest/$contestId"

    fun contestOuter(contestId: Int) = "$main/contests/$contestId"

    fun contestsWith(handle: String) = "$main/contests/with/$handle"

    fun submission(submission: CodeforcesSubmission) = "$main/contest/${submission.contestId}/submission/${submission.id}"

    fun problem(contestId: Int, problemIndex: String) = "$main/contest/$contestId/problem/$problemIndex"
}