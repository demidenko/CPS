package com.demich.cps.platforms.api.codeforces

open class CodeforcesApiException(comment: String): Throwable(message = comment)

class CodeforcesApiCallLimitExceededException(comment: String): CodeforcesApiException(comment)

class CodeforcesApiHandleNotFoundException(comment: String, val handle: String): CodeforcesApiException(comment)

class CodeforcesApiNotAllowedReadBlogException(comment: String): CodeforcesApiException(comment)

class CodeforcesApiContestRatingUnavailableException(comment: String): CodeforcesApiException(comment)

class CodeforcesApiContestNotStartedException(comment: String, val contestId: Int): CodeforcesApiException(comment)

class CodeforcesApiContestNotFoundException(comment: String, val contestId: Int): CodeforcesApiException(comment)