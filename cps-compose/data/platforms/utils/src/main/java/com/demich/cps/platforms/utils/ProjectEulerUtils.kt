package com.demich.cps.platforms.utils

import org.jsoup.Jsoup

object ProjectEulerUtils {
    class RecentProblem(
        val name: String,
        override val id: String
    ): NewsPostEntry

    fun extractRecentProblems(source: String): Sequence<RecentProblem?> =
        Jsoup.parse(source).expectFirst("#problems_table").select("td.id_column")
            .asSequence()
            .map { idCell ->
                idCell.nextElementSibling()?.let { nameCell ->
                    RecentProblem(
                        name = nameCell.text(),
                        id = idCell.text()
                    )
                }
            }

    class NewsPost(
        val title: String,
        val descriptionHtml: String,
        override val id: String
    ): NewsPostEntry

    fun extractNews(source: String): Sequence<NewsPost?> =
        Jsoup.parse(source).select("item")
            .asSequence()
            .map { item ->
                val idFull = item.expectFirst("guid").text()
                val id = idFull.removePrefix("news_id_")
                if (id != idFull) {
                    NewsPost(
                        title = item.expectFirst("title").text(),
                        descriptionHtml = item.expectFirst("description").html(),
                        id = id
                    )
                } else {
                    null
                }
            }

}

