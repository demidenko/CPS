package com.demich.cps.utils

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.features.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable

object CodeChefAPI {
    private val client = cpsHttpClient {
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

    private object csrftoken {
        private var token: String = ""
        suspend operator fun invoke(): String {
            if (token.isBlank()) recalc()
            return token
        }
        suspend fun recalc() {
            println("codechef x-csrf-token start recalc")
            val page = client.get<String>("https://www.codechef.com/ratings/all")
            var i = page.indexOf("window.csrfToken=")
            require(i != -1)
            i = page.indexOf('"', i)
            token = page.substring(i+1, page.indexOf('"', i+1))
            require(token.length == 64)
        }
    }


    private suspend inline fun<reified T> HttpClient.getCodeChef(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): T {
        return runCatching {
            get<T>(urlString) {
                header("x-csrf-token", csrftoken())
                block()
            }
        }.getOrElse {
            if (it is CodeChefCSRFTokenExpiredException) {
                csrftoken.recalc()
                get<T>(urlString) {
                    header("x-csrf-token", csrftoken())
                    block()
                }
            } else throw it
        }
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

object CodeChefUtils {
    object CodeChefURLFactory {
        const val main = "https://www.codechef.com"
        fun user(username: String) = "$main/users/$username"
    }
}