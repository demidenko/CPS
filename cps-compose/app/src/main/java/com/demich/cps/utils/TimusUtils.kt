package com.demich.cps.utils

import com.demich.cps.accounts.managers.AccountSuggestion
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.accounts.managers.TimusUserInfo
import io.ktor.client.request.*
import org.jsoup.Jsoup

object TimusApi: ResourceApi {

    suspend fun getUserPage(id: Int): String {
        return client.getText(urlString = urls.user(id)) {
            parameter("locale", "en")
        }
    }

    suspend fun getSearchPage(str: String): String {
        return client.getText(urlString = "${urls.main}/search.aspx") {
            parameter("Str", str)
        }
    }

    object urls {
        const val main = "https://timus.online"
        fun user(id: Int) = "$main/author.aspx?id=$id"
    }
}


object TimusUtils {
    fun extractUserInfo(source: String, handle: String): TimusUserInfo {
        with(Jsoup.parse(source)) {
            val userName = selectFirst("h2.author_name")?.text()
                ?: return TimusUserInfo(status = STATUS.NOT_FOUND, id = handle)
            val rows = select("td.author_stats_value").map { row ->
                row.text().let { it.substring(0, it.indexOf(" out of ")) }
            }
            return TimusUserInfo(
                status = STATUS.OK,
                id = handle,
                userName = userName,
                rating = rows[3].toInt(),
                solvedTasks = rows[1].toInt(),
                rankTasks = rows[0].toInt(),
                rankRating = rows[2].toInt()
            )
        }
    }

    fun extractUsersSuggestions(source: String): List<AccountSuggestion> =
        Jsoup.parse(source).expectFirst("table.ranklist")
            .select("td.name")
            .mapNotNull { nameColumn ->
                val userId = nameColumn.selectFirst("a")
                    ?.attr("href")
                    ?.let {
                        it.substring(it.indexOf("id=")+3)
                    } ?: return@mapNotNull null
                val tasks = nameColumn.nextElementSibling()?.nextElementSibling()?.text() ?: ""
                AccountSuggestion(
                    userId = userId,
                    title = nameColumn.text(),
                    info = tasks
                )
            }
}