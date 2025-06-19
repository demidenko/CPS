package com.demich.cps.platforms.api.codeforces

open class CodeforcesApiException
internal constructor(comment: String): Throwable(message = comment)

class CodeforcesApiCallLimitExceededException
internal constructor(comment: String): CodeforcesApiException(comment)

class CodeforcesApiHandleNotFoundException
internal constructor(comment: String, val handle: String): CodeforcesApiException(comment)

class CodeforcesApiNotAllowedReadBlogException
internal constructor(comment: String): CodeforcesApiException(comment)

class CodeforcesApiContestRatingUnavailableException
internal constructor(comment: String): CodeforcesApiException(comment)

class CodeforcesApiContestNotStartedException
internal constructor(comment: String, val contestId: Int): CodeforcesApiException(comment)

class CodeforcesApiContestNotFoundException
internal constructor(comment: String, val contestId: Int): CodeforcesApiException(comment)