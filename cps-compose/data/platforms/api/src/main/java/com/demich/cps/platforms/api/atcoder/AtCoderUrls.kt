package com.demich.cps.platforms.api.atcoder

object AtCoderUrls {
    const val main = "https://atcoder.jp"
    fun user(handle: String) = "$main/users/$handle"
    fun userContestResult(handle: String, contestId: String) = "$main/users/$handle/history/share/$contestId"
    fun contest(id: String) = "$main/contests/$id"
    fun post(id: String) = "$main/posts/$id"
}