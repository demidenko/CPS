package com.demich.cps.utils

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
                val exceptionResponse = exception.response
                val text = exceptionResponse.readText()
                if (exceptionResponse.status == HttpStatusCode.fromValue(403) && text == "{\"status\":\"apierror\",\"message\":\"Something went wrong\"}") {
                    throw CodeChefCSRFTokenExpiredException()
                }
            }
        }
    }

    class CodeChefCSRFTokenExpiredException: Throwable()

    private suspend fun createAsync(): Deferred<String> {
        return coroutineScope {
            async {
                println("codechef x-csrf-token start recalc...")
                val page = client.get<String>("https://www.codechef.com/ratings/all")
                var i = page.indexOf("window.csrfToken=")
                require(i != -1)
                i = page.indexOf('"', i)
                page.substring(i+1, page.indexOf('"', i+1)).also {
                    println("codechef x-csrf-token = $it")
                    require(it.length == 64)
                }
            }
        }
    }

    private var tokenDeferred: Deferred<String>? = null
    private suspend fun getToken(): String {
        //TODO shit actually
        val d = tokenDeferred ?: createAsync().also { tokenDeferred = it }
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
        return client.getCodeChef("https://www.codechef.com/api/ratings/all") {
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
