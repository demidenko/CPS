package com.demich.cps.workers

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.demich.cps.platforms.api.codeforces.CodeforcesApiException
import com.demich.cps.platforms.api.codeforces.CodeforcesTemporarilyUnavailableException
import com.demich.cps.platforms.clients.isResponseException
import com.demich.cps.ui.bottomprogressbar.ProgressBarInfo
import com.demich.cps.utils.getCurrentTime
import com.demich.cps.utils.joinAllWithProgress
import com.demich.cps.utils.jsonCPS
import com.demich.cps.utils.repeatUntilSuccessOrLast
import com.demich.cps.utils.update
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

abstract class CPSWorker(
    protected val work: CPSPeriodicWork,
    val parameters: WorkerParameters
): CoroutineWorker(work.context, parameters) {

    protected val context get() = work.context

    private val timeHolder = TimeHolder()
    protected val workerStartTime by timeHolder

    final override suspend fun doWork(): Result {
        if (!work.isEnabled()) {
            work.stop()
            return Result.success()
        }

        val workersInfo = CPSWorkersDataStore(context)

        val result = coroutineScope {
            val event = ExecutionEvent(start = workerStartTime)
            workersInfo.append(event)
            smartRunWork().also { result ->
                workersInfo.append(event.copy(end = getCurrentTime(), resultType = result.toType()))
                if (result.toType() != ResultType.SUCCESS) {
                    work.enqueueAsap()
                }
            }
        }

        return result
    }

    protected abstract suspend fun runWork(): Result
    private suspend fun smartRunWork(): Result =
        repeatUntilSuccessOrLast(
            times = 2,
            between = { delay(5.seconds) },
            isSuccess = { it.toType() == ResultType.SUCCESS }
        ) {
            setProgressInfo(ProgressBarInfo(total = 0))
            timeHolder.reset()
            runCatching { runWork() }.getOrElse {
                println("${work.name}: $it")
                when {
                    it.isResponseException -> Result.retry()
                    it is CodeforcesApiException -> Result.failure()
                    it is CodeforcesTemporarilyUnavailableException -> Result.retry()
                    else -> Result.failure()
                }
            }
        }

    protected suspend fun setProgressInfo(progressInfo: ProgressBarInfo) {
        if (progressInfo.total == 0) {
            setProgress(workDataOf())
            return
        }
        setProgress(workDataOf(
            KEY_PROGRESS to arrayOf(progressInfo.current, progressInfo.total)
        ))
    }

    protected suspend inline fun <T> List<T>.forEachWithProgress(
        action: (T) -> Unit
    ) {
        var progressInfo = ProgressBarInfo(total = size)
        setProgressInfo(progressInfo)
        forEach {
            action(it)
            progressInfo++
            setProgressInfo(progressInfo)
        }
    }

    protected suspend fun List<suspend () -> Unit>.joinAllWithProgress() {
        if (isEmpty()) return
        joinAllWithProgress(title = "") {
            setProgressInfo(it)
        }
    }

    protected suspend inline fun joinAllWithProgress(
        block: MutableList<suspend () -> Unit>.() -> Unit
    ) {
        buildList(block).joinAllWithProgress()
    }


    enum class ResultType {
        SUCCESS, RETRY, FAILURE
    }

    @SuppressLint("RestrictedApi")
    private fun Result.toType(): ResultType? {
        return when (this) {
            is Result.Success -> ResultType.SUCCESS
            is Result.Retry -> ResultType.RETRY
            is Result.Failure -> ResultType.FAILURE
            else -> null
        }
    }

    @Serializable
    data class ExecutionEvent(
        val start: Instant,
        val end: Instant? = null,
        val resultType: ResultType? = null
    ) {
        val duration: Duration? get() = end?.minus(start)
    }

    private suspend fun CPSWorkersDataStore.append(event: ExecutionEvent) {
        executions.edit {
            val dateThreshold = event.start - 24.hours
            update(work.name) { list ->
                buildList {
                    list.filterTo(this) { it.start >= dateThreshold }
                    indexOfFirst { it.start == event.start }.let { index ->
                        if (index == -1) add(event)
                        else set(index, event)
                    }
                }
            }
        }
    }

    protected val hintsDataStore: WorkersHintsDataStore
        get() = WorkersHintsDataStore(context)
}

private const val KEY_PROGRESS = "cpsworker_progress"
fun WorkInfo.getProgressInfo(): ProgressBarInfo? {
    val arr = progress.getIntArray(KEY_PROGRESS)?.takeIf { it.size == 2 } ?: return null
    return ProgressBarInfo(
        current = arr[0],
        total = arr[1]
    )
}


class CPSWorkersDataStore(context: Context): ItemizedDataStore(context.workersDataStore) {
    companion object {
        private val Context.workersDataStore by dataStoreWrapper(name = "workers_info")
    }

    val executions = jsonCPS.itemMap<String, List<CPSWorker.ExecutionEvent>>("executions")
}


private class TimeHolder {
    private var time: Instant = getCurrentTime()
    fun reset() {
        time = getCurrentTime()
    }
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = time
}