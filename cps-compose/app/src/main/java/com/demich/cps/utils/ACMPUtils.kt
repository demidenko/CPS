package com.demich.cps.utils

import com.demich.cps.accounts.managers.UserSuggestion
import com.demich.cps.accounts.userinfo.ACMPUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import org.jsoup.Jsoup

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

    fun extractUsersSuggestions(source: String): List<UserSuggestion> =
        Jsoup.parse(source).expectFirst("table.main")
            .select("tr.white")
            .map { row ->
                val cols = row.select("td")
                val userId = cols[1].expectFirst("a")
                    .attr("href").removePrefix("/?main=user&id=")
                val userName = cols[1].text()
                val tasks = cols[3].text()
                UserSuggestion(
                    userId = userId,
                    title = userName,
                    info = tasks
                )
            }
}