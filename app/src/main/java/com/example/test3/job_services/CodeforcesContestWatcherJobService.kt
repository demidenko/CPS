package com.example.test3.job_services

import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.STATUS

class CodeforcesContestWatcherJobService: CoroutineJobService() {

    override suspend fun doJob() {
        val info = CodeforcesAccountManager(this).savedInfo
        if(info.status != STATUS.OK) return
        TODO("https://codeforces.com/api/user.status?handle=${info.userID}&count=50")
    }

}