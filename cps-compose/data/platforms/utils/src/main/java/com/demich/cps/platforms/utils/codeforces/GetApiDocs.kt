package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup

@Serializable
data class CodeforcesApiHelpMethod(
    val name: String,
    val parameters: List<CodeforcesApiHelpMethodParameter>
)

@Serializable
data class CodeforcesApiHelpMethodParameter(
    val name: String,
    val description: String
)

suspend fun CodeforcesPageContentProvider.getApiHelpMethods(): List<CodeforcesApiHelpMethod> {
    return extractMethods(page = getApiHelpMethodsPage())
}

private fun extractMethods(page: String): List<CodeforcesApiHelpMethod> {
    val main = Jsoup.parse(page)
        .expectFirst("#pageContent")
        .expectFirst("div.ttypography")

    val h3 = main.select("h3")
    val tables = main.select("table")

    check(h3.size == tables.size) {
        "${h3.size} of h3 / ${tables.size} of tables"
    }

    return h3.zip(tables).map { (h3, table) ->
        val name = h3.text().trim()

        val parameters = table.select("tbody > tr").map { tr ->
            val cols = tr.select("td")
            check(cols.size == 2) {
                "expected exactly 2 columns, but ${cols.size} found"
            }

            CodeforcesApiHelpMethodParameter(
                name = cols[0].expectFirst("strong").text().trim(),
                description = cols[1].text().trim()
            )
        }

        CodeforcesApiHelpMethod(
            name = name,
            parameters = parameters
        )
    }
}
