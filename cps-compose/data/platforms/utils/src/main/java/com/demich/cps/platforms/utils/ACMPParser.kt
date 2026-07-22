package com.demich.cps.platforms.utils

import com.demich.cps.profiles.userinfo.ACMPUserInfo
import com.demich.cps.profiles.userinfo.UserSuggestion

class ACMPParser {
    fun extractUserInfo(source: String, id: String): ACMPUserInfo =
        with(source.parseDocument()) {
            val userName = title()
            val box = body().select("h4").first { it.text() == "Общая статистика" }.parent()
            requireNotNull(box)
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
                id = id,
                userName = userName,
                rating = rating,
                solvedTasks = solvedTasks,
                rank = rank
            )
        }

    fun extractUsersSuggestions(source: String): List<UserSuggestion> =
        source.parseDocument().expectFirst("table.main")
            .select("tr.white")
            .map { row ->
                val cols = row.select("td")
                val userId = cols[1].expectFirst("a").href.removePrefix("/?main=user&id=")
                val userName = cols[1].text()
                val tasks = cols[3].text()
                UserSuggestion(
                    userId = userId,
                    title = userName,
                    info = tasks
                )
            }
}