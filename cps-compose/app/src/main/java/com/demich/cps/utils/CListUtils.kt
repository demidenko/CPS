package com.demich.cps.utils

import com.demich.cps.accounts.managers.*
import io.ktor.client.request.*

object CListUtils {
    fun getManager(resource: String, userName: String, link: String): Pair<AccountManagers, String>? {
        return when(resource){
            "codeforces.com" -> AccountManagers.codeforces to userName
            "atcoder.jp" -> AccountManagers.atcoder to userName
            "codechef.com" -> AccountManagers.codechef to userName
            "dmoj.ca" -> AccountManagers.dmoj to userName
            "acm.timus.ru", "timus.online" -> {
                val userId = link.substring(link.lastIndexOf('=')+1)
                AccountManagers.timus to userId
            }
            else -> null
        }
    }
}

object CListAPI {
    private val client = cpsHttpClient {  }

    suspend fun getUserPage(login: String): String {
        return client.get(URLFactory.user(login))
    }

    suspend fun getUsersSearchPage(str: String): String {
        return client.get(URLFactory.main + "/coders") {
            parameter("search", str)
        }
    }

    object URLFactory {
        const val main = "https://clist.by"
        fun user(login: String) = "$main/coder/$login"

        val apiHelp get() = "$main/api/v2/doc/"
    }
}