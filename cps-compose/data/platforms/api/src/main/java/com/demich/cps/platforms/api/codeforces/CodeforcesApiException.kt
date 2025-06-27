package com.demich.cps.platforms.api.codeforces

abstract class CodeforcesApiException
internal constructor(val comment: String): CodeforcesException(message = comment)

class CodeforcesApiUnspecifiedException(comment: String): CodeforcesApiException(comment)

class CodeforcesApiCallLimitExceededException(comment: String): CodeforcesApiException(comment)

class CodeforcesApiHandleNotFoundException(comment: String, val handle: String): CodeforcesApiException(comment)

class CodeforcesApiNotAllowedReadBlogException(comment: String): CodeforcesApiException(comment)

class CodeforcesApiContestRatingUnavailableException(comment: String): CodeforcesApiException(comment)

class CodeforcesApiContestNotStartedException(comment: String, val contestId: Int): CodeforcesApiException(comment)

class CodeforcesApiContestNotFoundException(comment: String, val contestId: Int): CodeforcesApiException(comment)