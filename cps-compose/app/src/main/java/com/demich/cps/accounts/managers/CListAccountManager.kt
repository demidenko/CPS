package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.buildAnnotatedString
import com.demich.cps.utils.CListAPI
import org.jsoup.Jsoup


data class CListUserInfo(
    override val status: STATUS,
    val login: String,
    val accounts: Map<String, Pair<String, String>> = emptyMap()
): UserInfo() {
    override val userId: String
        get() = login

    override fun link(): String = CListAPI.URLFactory.user(login)
}

class CListAccountManager(context: Context):
    AccountManager<CListUserInfo>(context, AccountManagers.clist),
    AccountSuggestionsProvider
{
    override val userIdTitle = "login"
    override val urlHomePage = CListAPI.URLFactory.main

    override fun emptyInfo() = CListUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String, flags: Int): CListUserInfo {
        try {
            val s = CListAPI.getUserPage(login = data)
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
            //TODO: parse buttons

            return CListUserInfo(
                status = STATUS.OK,
                login = data,
                accounts = accounts
            )
        } catch (e: Throwable) {
            return CListUserInfo(status = STATUS.FAILED, login = data)
        }
    }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion>? {
        try {
            val s = CListAPI.getUsersSearchPage(str)
            return buildList {
                var i = 0
                while (true) {
                    i = s.indexOf("<td class=\"username\">", i+1)
                    if (i == -1) break
                    var j = s.indexOf("<span", i)
                    j = s.indexOf("<a href=", j)
                    val login = s.substring(s.indexOf("\">",j)+2, s.indexOf("</a",j))
                    add(AccountSuggestion(login,"",login))
                }
            }
        } catch (e: Throwable) {
            return null
        }
    }

    @Composable
    override fun makeOKInfoSpan(userInfo: CListUserInfo) = with(userInfo) {
        buildAnnotatedString { append("$login (${accounts.size})") }
    }

    override fun getDataStore(): AccountDataStore<CListUserInfo> {
        throw IllegalAccessException("CList account manager can not provide data store")
    }

}