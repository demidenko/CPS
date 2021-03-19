package com.example.test3.contest_watch

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CodeforcesContestWatchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        TODO("Not yet implemented")
    }

}