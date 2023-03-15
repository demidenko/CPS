package com.demich.cps.utils

import com.demich.cps.accounts.userinfo.CodeChefUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import org.jsoup.Jsoup

object CodeChefUtils {
    fun extractUserInfo(source: String, handle: String): CodeChefUserInfo =
        Jsoup.parse(source).run {
            val rating = expectFirst("div.rating-ranks")
                .select("a")
                .takeIf { it.any { it.text() != "Inactive" } }
                ?.let { selectFirst("div.rating-header > div.rating-number")?.text()?.toInt() }
            val userName = expectFirst("section.user-details")
                .selectFirst("span.m-username--link")
                ?.text() ?: handle
            return CodeChefUserInfo(
                status = STATUS.OK,
                handle = userName,
                rating = rating
            )
        }
}