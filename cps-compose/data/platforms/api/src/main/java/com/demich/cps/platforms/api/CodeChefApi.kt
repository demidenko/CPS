package com.demich.cps.platforms.api

import com.demich.kotlin_stdlib_boost.ifBetweenFirstFirst
import com.demich.kotlin_stdlib_boost.ifBetweenFirstLast
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

object CodeChefApi: PlatformClient {
    private val json get() = defaultJson

    override val client = cpsHttpClient(
        json = json,
        useCookies = true,
        connectionTimeout = 30.seconds,
        requestTimeout = 60.seconds
    ) {
        followRedirects = false
        //BrowserUserAgent()
        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, _ ->
                if (exception !is ResponseException) return@handleResponseExceptionWithRequest
                val response = exception.response
                val text = response.bodyAsText()
                if (response.status.value == 403 && text == somethingWentWrongMessage) {
                    throw CodeChefCSRFTokenExpiredException()
                }
            }
        }
    }

    private class CodeChefCSRFTokenExpiredException: Throwable()

    private object CSRFToken {
        private var tokenDeferred: Deferred<String>? = null
        fun clear() {
            tokenDeferred?.cancel()
            tokenDeferred = null
        }
        suspend operator fun invoke(): String {
            val d = tokenDeferred ?: client.async {
                println("codechef x-csrf-token start recalc...")
                extractCSRFToken(source = client.getText("${urls.main}/ratings/all")).also {
                    println("codechef x-csrf-token = $it")
                    check(it.length == 64)
                }
            }.also { tokenDeferred = it }
            return d.await()
        }
    }

    private suspend inline fun<reified T> getCodeChef(
        urlString: String,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): T {
        val callGet = suspend {
            client.getAs<T>(urlString) {
                header("x-csrf-token", CSRFToken())
                block()
            }
        }
        return kotlin.runCatching {
            callGet()
        }.getOrElse {
            if (it is CodeChefCSRFTokenExpiredException) {
                CSRFToken.clear()
                callGet()
            } else throw it
        }
    }

    suspend fun getUserPage(handle: String): String {
        return client.getText(urls.user(handle))
    }

    suspend fun getSuggestions(str: String): CodeChefSearchResult {
        return getCodeChef("${urls.main}/api/ratings/all") {
            parameter("itemsPerPage", 40)
            parameter("order", "asc")
            parameter("page", 1)
            parameter("sortBy", "global_rank")
            parameter("search", str)
        }
    }

    suspend fun getRatingChanges(handle: String): List<CodeChefRatingChange> {
        ifBetweenFirstFirst(
            str = getUserPage(handle = handle),
            from = "var all_rating",
            to = ";"
        ) { str ->
            ifBetweenFirstLast(str, from = "[", to = "]", include = true) {
                return json.decodeFromString(it)
            }
        }
        return emptyList()
    }

    object urls {
        const val main = "https://www.codechef.com"
        fun user(username: String) = "$main/users/$username"
    }
}

@Serializable
data class CodeChefUser(
    val name: String,
    val username: String,
    val rating: Int
)

@Serializable
data class CodeChefSearchResult(
    val list: List<CodeChefUser>
)


@Serializable
data class CodeChefRatingChange(
    val code: String,
    val name: String,
    val rating: String,
    val rank: String,
    val end_date: String
)

private fun extractCSRFToken(source: String): String {
    var i = source.indexOf("window.csrfToken")
    check(i != -1)
    i = source.indexOf('=', i + 1)
    i = source.indexOf('"', i)
    return source.substring(i+1, source.indexOf('"', i+1))
}

private const val somethingWentWrongMessage = "{\"status\":\"apierror\",\"message\":\"Something went wrong\"}"