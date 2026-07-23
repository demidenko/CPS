package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.utils.parseDocument

internal class CodeforcesContestPageParser: CodeforcesPageParser {
    private inline fun extractContestPhaseInfo(source: String, block: (String, String) -> Unit) {
        val sidebar = source.parseDocument().selectSidebar() ?: return
        val phaseText = sidebar.selectFirst("span.contest-state-phase")?.text() ?: return
        val infoText = sidebar.selectFirst("span.contest-state-regular")?.text() ?: return
        block(phaseText, infoText)
    }

    fun parseContestSystemTestingPercentageOrNull(source: String): Int? {
        extractContestPhaseInfo(source) { phase, text ->
            if (phase != "System testing") return null
            return text.removeSuffix("%").toIntOrNull()
        }
        return null
    }
}

suspend fun CodeforcesPageContentProvider.getSysTestPercentageOrNull(contestId: Int): Int? =
    runCatching { getContestPage(contestId = contestId) }
        .map { CodeforcesContestPageParser().parseContestSystemTestingPercentageOrNull(it) }
        .getOrNull()