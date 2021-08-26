package com.example.test3.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.test3.account_manager.CodeforcesAccountManager

class CodeforcesUpsolvingSuggestionsWorker(private val context: Context, params: WorkerParameters): CoroutineWorker(context, params) {

    companion object {
        suspend fun isEnabled(context: Context): Boolean = CodeforcesAccountManager(context).getSettings().upsolvingSuggestionsEnabled()
    }

    override suspend fun doWork(): Result {
        if(!isEnabled(context)) {
            WorkersCenter.stopWorker(context, WorkersNames.codeforces_upsolving_suggestions)
            return Result.success()
        }

        return Result.success()
    }

}