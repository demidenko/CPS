package com.demich.cps.utils

import com.demich.cps.accounts.managers.ACMPUserInfo
import com.demich.cps.accounts.managers.AccountSuggestion
import com.demich.cps.accounts.managers.STATUS
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.nio.charset.Charset

object ACMPApi: ResourceApi {
    private val windows1251 = Charset.forName("windows-1251")
    override val client = cpsHttpClient {
        Charsets {
            register(windows1251)
            responseCharsetFallback = windows1251
        }
    }

    class ACMPPageNotFoundException : Throwable()

    //TODO: ktor can't get charset from client
    private suspend fun HttpResponse.bodyAsText() = bodyAsText(fallbackCharset = windows1251)

    //TODO: this function is copy of top level
    private suspend inline fun HttpClient.getText(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): String = this.get(urlString = urlString, block = block).bodyAsText()

    suspend fun getMainPage(): String {
        return client.getText(urls.main)
    }

    suspend fun getUserPage(id: Int): String {
        with(client.get(urls.user(id))) {
            //acmp redirects to main page if user not found
            if (request.url.parameters.isEmpty()) throw ACMPPageNotFoundException()
            return bodyAsText()
        }
    }

    suspend fun getUsersSearch(str: String): String {
        return client.getText(urls.main + "/index.asp?main=rating") {
            url.encodedParameters.append("str", URLEncoder.encode(str, windows1251.name()))
        }
    }

    object urls {
        const val main = "https://acmp.ru"
        fun user(id: Int) = "$main/index.asp?main=user&id=$id"
    }
}


object ACMPUtils {
    fun extractUserInfo(source: String, id: String): ACMPUserInfo =
        with(Jsoup.parse(source)) {
            val userName = title().trim()
            val box = body().select("h4").firstOrNull { it.text() == "Общая статистика" }?.parent()!!
            val bs = box.select("b.btext").map { it.text() }
            val solvedTasks = bs.first { it.startsWith("Решенные задачи") }.let {
                it.substring(it.indexOf('(')+1, it.indexOf(')')).toInt()
            }
            val rating = bs.first { it.startsWith("Рейтинг:") }.let {
                it.substring(it.indexOf(':')+2, it.indexOf('/')-1).toInt()
            }
            val rank = bs.first { it.startsWith("Место:") }.let {
                it.substring(it.indexOf(':')+2, it.indexOf('/')-1).toInt()
            }
            ACMPUserInfo(
                status = STATUS.OK,
                id = id,
                userName = userName,
                rating = rating,
                solvedTasks = solvedTasks,
                rank = rank
            )
        }

    fun extractUsersSuggestions(source: String): List<AccountSuggestion> =
        Jsoup.parse(source).expectFirst("table.main")
            .select("tr.white")
            .map { row ->
                val cols = row.select("td")
                val userId = cols[1].expectFirst("a")
                    .attr("href").removePrefix("/?main=user&id=")
                val userName = cols[1].text()
                val tasks = cols[3].text()
                AccountSuggestion(
                    userId = userId,
                    title = userName,
                    info = tasks
                )
            }
}