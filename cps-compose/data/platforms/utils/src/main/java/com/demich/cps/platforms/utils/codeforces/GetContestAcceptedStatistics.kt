package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.api.codeforces.models.CodeforcesProblem
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

suspend fun CodeforcesPageContentProvider.getContestAcceptedStatistics(contestId: Int): Map<CodeforcesProblem, Int> {
    val page = getContestPage(contestId = contestId)
    return buildMap {
        Jsoup.parse(page).expectFirst("table.problems").select("tr")
            .forEach {
                extractProblemWithAcceptedCount(it, contestId, ::put)
            }
    }
}

private inline fun extractProblemWithAcceptedCount(
    problemRow: Element,
    contestId: Int,
    block: (CodeforcesProblem, Int) -> Unit
) {
    val td = problemRow.select("td")
    if (td.isEmpty()) return //th row
    val acceptedCount = td[3].text().trim().run {
        if (!startsWith('x')) return
        drop(1).toInt()
    }
    val problem = CodeforcesProblem(
        index = td[0].text().trim(),
        name = td[1].expectFirst("a").text(),
        contestId = contestId
    )
    block(problem, acceptedCount)
}