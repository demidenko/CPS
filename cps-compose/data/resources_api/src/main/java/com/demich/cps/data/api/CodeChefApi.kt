package com.demich.cps.data.api

import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.time.Duration.Companion.seconds

object CodeChefApi: ResourceApi {
    private val json get() = defaultJson

    override val client = cpsHttpClient(
        json = json,
        connectionTimeout = 30.seconds,
        requestTimeout = 60.seconds
    ) {
        followRedirects = false
        //BrowserUserAgent()
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
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

    private val somethingWentWrongMessage get() = "{\"status\":\"apierror\",\"message\":\"Something went wrong\"}"

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
                    require(it.length == 64)
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
        val s = getUserPage(handle = handle)
        val i = s.indexOf("var all_rating = ")
        if (i == -1) return emptyList()
        val ar = s.substring(s.indexOf("[", i), s.indexOf("];", i) + 1)
        return json.decodeFromString(ar)
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
    require(i != -1)
    i = source.indexOf('=', i + 1)
    i = source.indexOf('"', i)
    return source.substring(i+1, source.indexOf('"', i+1))
}