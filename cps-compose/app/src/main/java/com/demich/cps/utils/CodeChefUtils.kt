package com.demich.cps.utils

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable

object CodeChefAPI {
    private val client = cpsHttpClient {
        followRedirects = false
        //BrowserUserAgent()
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
        HttpResponseValidator {
            handleResponseException { exception ->
                if (exception !is ResponseException) return@handleResponseException
                val response = exception.response
                val text = response.readText()
                if (response.status == HttpStatusCode.fromValue(403) && text == "{\"status\":\"apierror\",\"message\":\"Something went wrong\"}") {
                    throw CodeChefCSRFTokenExpiredException()
                }
            }
        }
    }

    private class CodeChefCSRFTokenExpiredException: Throwable()


    private var tokenDeferred: Deferred<String>? = null
    private suspend fun getToken(): String {
        val d = tokenDeferred ?: client.async {
            println("codechef x-csrf-token start recalc...")
            val page = client.get<String>("${URLFactory.main}/ratings/all")
            var i = page.indexOf("window.csrfToken=")
            require(i != -1)
            i = page.indexOf('"', i)
            page.substring(i+1, page.indexOf('"', i+1)).also {
                println("codechef x-csrf-token = $it")
                require(it.length == 64)
            }
        }.also { tokenDeferred = it }
        return d.await()
    }

    private suspend inline fun<reified T> HttpClient.getCodeChef(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): T {
        return runCatching {
            get<T>(urlString) {
                header("x-csrf-token", getToken())
                block()
            }
        }.getOrElse {
            if (it is CodeChefCSRFTokenExpiredException) {
                tokenDeferred = null
                get<T>(urlString) {
                    header("x-csrf-token", getToken())
                    block()
                }
            } else throw it
        }
    }

    suspend fun getUserPage(handle: String): String {
        return client.get(URLFactory.user(handle))
    }

    suspend fun getSuggestions(str: String): CodeChefSearchResult {
        return client.getCodeChef("${URLFactory.main}/api/ratings/all") {
            parameter("itemsPerPage", 40)
            parameter("order", "asc")
            parameter("page", 1)
            parameter("sortBy", "global_rank")
            parameter("search", str)
        }
    }

    object URLFactory {
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
