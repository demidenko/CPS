package com.demich.cps.platforms.api.codeforces

import com.demich.cps.platforms.api.codeforces.models.CodeforcesSubmission

object CodeforcesUrls {
    const val main = "https://codeforces.com"

    fun user(handle: String) = "$main/profile/$handle"

    fun blogEntry(blogEntryId: Int) = "$main/blog/entry/$blogEntryId"

    fun comment(blogEntryId: Int, commentId: Long) = blogEntry(blogEntryId) + "#comment-$commentId"

    fun contest(contestId: Int) = "$main/contest/$contestId"

    fun contestPending(contestId: Int) = "$main/contests/$contestId"

    fun contestsWith(handle: String) = "$main/contests/with/$handle"

    fun submission(submission: CodeforcesSubmission) = contest(submission.contestId) + "/submission/${submission.id}"

    fun problem(contestId: Int, problemIndex: String) = contest(contestId) + "/problem/$problemIndex"
}