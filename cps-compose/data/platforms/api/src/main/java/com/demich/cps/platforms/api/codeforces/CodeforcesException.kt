package com.demich.cps.platforms.api.codeforces

abstract class CodeforcesException
internal constructor(message: String): Throwable(message = message)

class CodeforcesTemporarilyUnavailableException: CodeforcesException("Codeforces Temporarily Unavailable")