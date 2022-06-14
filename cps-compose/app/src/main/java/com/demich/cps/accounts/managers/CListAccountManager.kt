package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.utils.CListApi
import io.ktor.client.plugins.*
import io.ktor.http.*
import org.jsoup.Jsoup


data class CListUserInfo(
    override val status: STATUS,
    val login: String,
    val accounts: Map<String, Pair<String, String>> = emptyMap()
): UserInfo() {
    override val userId: String
        get() = login

    override fun link(): String = CListApi.urls.user(login)
}

class CListAccountManager(context: Context):
    AccountManager<CListUserInfo>(context, AccountManagers.clist),
    AccountSuggestionsProvider
{
    override val userIdTitle = "login"
    override val urlHomePage = CListApi.urls.main

    override fun emptyInfo() = CListUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String, flags: Int): CListUserInfo {
        try {
            val s = CListApi.getUserPage(login = data)
            val accounts = mutableMapOf<String, Pair<String, String>>()

            val body = Jsoup.parse(s).body()
            //rated table
            body.getElementById("table-accounts")
                ?.select("tr")
                ?.forEach { row ->
                    val cols = row.select("td")
                    val href = cols[0].getElementsByAttribute("href").attr("href")
                    val resource = href.removePrefix("/resource/").removeSuffix("/")
                    val link = cols[3].getElementsByAttribute("href").attr("href")
                    val userName = cols[2].select("span").map { it.text() }.firstOrNull { it.isNotBlank() } ?: return@forEach
                    accounts[resource] = userName to link
                }

            //buttons
            body.getElementsByClass("account btn-group btn-group-sm").forEach { button ->
                val a = button.select("a")
                if (a.isEmpty()) return@forEach
                val href = a[0].attr("href")
                val resource = href.removePrefix("/resource/").removeSuffix("/")
                val link = button.getElementsByClass("fas fa-external-link-alt").first()?.parent()?.attr("href") ?: ""
                val span = a[1].selectFirst("span") ?: return@forEach
                val userName = span.run {
                    val attr = "title"
                    val withAttr = getElementsByAttribute(attr).first()
                    if (withAttr == null) text()
                    else withAttr.attr(attr)
                }
                accounts[resource] = userName to link
            }

            return CListUserInfo(
                status = STATUS.OK,
                login = data,
                accounts = accounts
            )
        } catch (e: Throwable) {
            if (e is ClientRequestException && e.response.status == HttpStatusCode.NotFound) {
                return CListUserInfo(status = STATUS.NOT_FOUND, login = data)
            }
            return CListUserInfo(status = STATUS.FAILED, login = data)
        }
    }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion> {
        val s = CListApi.getUsersSearchPage(str)
        return Jsoup.parse(s).select("td.username").map {
            AccountSuggestion(userId = it.text())
        }
    }

    @Composable
    override fun makeOKInfoSpan(userInfo: CListUserInfo) = with(userInfo) {
        AnnotatedString("$login (${accounts.size})")
    }

    override fun getDataStore(): AccountDataStore<CListUserInfo> {
        throw IllegalAccessException("CList account manager can not provide data store")
    }

}