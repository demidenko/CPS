package com.demich.cps.utils

import io.ktor.client.features.*
import io.ktor.client.features.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.serialization.Serializable

object CodeChefApi {
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
            val page = client.get<String>("${urls.main}/ratings/all")
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

    private suspend inline fun<reified T> getCodeChef(
        urlString: String,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): T {
        val callGet = suspend {
            client.get<T>(urlString) {
                header("x-csrf-token", getToken())
                block()
            }
        }
        return kotlin.runCatching {
            callGet()
        }.getOrElse {
            if (it is CodeChefCSRFTokenExpiredException) {
                tokenDeferred = null
                callGet()
            } else throw it
        }
    }

    suspend fun getUserPage(handle: String): String {
        return client.get(urls.user(handle))
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
