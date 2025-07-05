package com.demich.cps.contests.monitors

import android.content.Context
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.widget.RemoteViews
import androidx.core.text.bold
import androidx.core.text.color
import com.demich.cps.R
import com.demich.cps.notifications.NotificationBuilder
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContestPhase
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContestType
import com.demich.cps.platforms.api.codeforces.models.CodeforcesParticipationType
import com.demich.cps.utils.getCurrentTime
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration.Companion.milliseconds

class CodeforcesMonitorNotifier(
    val context: Context,
    val notificationBuilder: NotificationBuilder,
    val handle: String
) {
    private var changed: Boolean = false

    private inline fun <T> delegateWithOld(initialValue: T, crossinline block: (T, T) -> Unit) =
        object : ObservableProperty<T>(initialValue = initialValue) {
            override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean {
                return newValue != oldValue
            }

            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
                changed = true
                block(oldValue, newValue)
            }
        }

    private inline fun <T> delegate(initialValue: T, crossinline block: (T) -> Unit) =
        delegateWithOld(initialValue = initialValue) { oldValue, newValue -> block(newValue) }

    private val viewSmall = RemoteViews(context.packageName, R.layout.cf_monitor_view_small)
    private val viewBig = RemoteViews(context.packageName, R.layout.cf_monitor_view_big)
    private val views = arrayOf(viewSmall, viewBig)
    private val problemColumns = mutableMapOf<String, RemoteViews>()

    fun apply(contestData: CodeforcesMonitorData) {
        changed = false
        contestantRank = contestData.contestantRank
        contestName = contestData.contestInfo.name
        if (setContestPhase(contestData.contestPhase)) changed = true
        if (setProblemNames(contestData.problems.map { it.name })) changed = true
        contestData.problems.forEach { //don't confuse with .any{} !!
            if (setProblemResult(
                problemName = it.name,
                problemResult = it.result,
                contestType = contestData.contestInfo.type
            )) changed = true
        }
        if (changed) submitNotification()
    }

    private var contestantRank by delegate(
        initialValue = CodeforcesMonitorData.ContestRank(rank = null, participationType = CodeforcesParticipationType.NOT_PARTICIPATED)
    ) {
        val rank = buildString {
            if (it.participationType != CodeforcesParticipationType.CONTESTANT) append('*')
            append(it.rank)
        }
        viewBig.setTextViewText(R.id.cf_monitor_rank, rank)
    }

    private var contestName by delegate(initialValue = "") {
        notificationBuilder.edit { subText = "$it • $handle" }
    }

    private var _phase: CodeforcesMonitorData.ContestPhase = CodeforcesMonitorData.ContestPhase.Other(
        CodeforcesContestPhase.UNDEFINED)
    private fun setContestPhase(phase: CodeforcesMonitorData.ContestPhase): Boolean {
        if (_phase == phase) return false
        val phaseChanged = _phase.phase != phase.phase
        _phase = phase
        views.forEach { it.setTextViewText(R.id.cf_monitor_phase, phase.phase.title) }
        if (phase is CodeforcesMonitorData.ContestPhase.Coding) {
            val remaining = phase.endTime - getCurrentTime()
            val elapsed = SystemClock.elapsedRealtime().milliseconds
            views.forEach { it.setChronometer(R.id.cf_monitor_progress, (elapsed + remaining).inWholeMilliseconds, null, true) }
        } else {
            if (phaseChanged) {
                views.forEach { it.setChronometer(R.id.cf_monitor_progress, 0, null, false) }
                views.forEach { it.setTextViewText(R.id.cf_monitor_progress, "") }
            }
        }
        if (phase is CodeforcesMonitorData.ContestPhase.SystemTesting) {
            views.forEach { it.setTextViewText(R.id.cf_monitor_progress, "${phase.percentage}%") }
        }
        return true
    }

    private var _problemNames: List<String>? = null
    private fun setProblemNames(problemNames: List<String>): Boolean {
        if (_problemNames == problemNames) return false
        _problemNames = problemNames
        viewBig.removeAllViews(R.id.cf_monitor_problems_table)
        problemColumns.clear()
        problemNames.forEach { problemName ->
            val view = RemoteViews(context.packageName, R.layout.cf_monitor_table_column)
            view.setTextViewText(R.id.cf_monitor_table_column_header, problemName)
            view.setTextViewText(R.id.cf_monitor_table_column_cell, "")
            viewBig.addView(R.id.cf_monitor_problems_table, view)
            problemColumns[problemName] = view
        }
        return true
    }

    private val _problemResults = mutableMapOf<String, CodeforcesMonitorData.ProblemResult>()
    private fun setProblemResult(
        problemName: String,
        problemResult: CodeforcesMonitorData.ProblemResult,
        contestType: CodeforcesContestType
    ): Boolean {
        if (_problemResults[problemName] == problemResult) return false
        _problemResults[problemName] = problemResult
        problemColumns[problemName]?.run {
            setTextViewText(R.id.cf_monitor_table_column_cell, spanOfResult(problemResult, contestType))
        }
        return true
    }

    private val failColor = context.getColor(R.color.fail)
    private val successColor = context.getColor(R.color.success)
    private fun spanOfResult(
        problemResult: CodeforcesMonitorData.ProblemResult,
        contestType: CodeforcesContestType
    ) = SpannableStringBuilder().apply {
            when (problemResult) {
                CodeforcesMonitorData.ProblemResult.Pending -> {
                    append("?")
                }
                CodeforcesMonitorData.ProblemResult.FailedSystemTest -> {
                    color(color = failColor) {
                        append(CodeforcesMonitorData.ProblemResult.failedSystemTestSymbol)
                    }
                }
                is CodeforcesMonitorData.ProblemResult.Points -> {
                    bold {
                        val text = if (contestType == CodeforcesContestType.ICPC) "+"
                                    else problemResult.pointsToNiceString()
                        if (problemResult.isFinal) {
                            color(successColor) { append(text) }
                        } else {
                            append(text)
                        }
                    }
                }
                CodeforcesMonitorData.ProblemResult.Empty -> {}
            }
        }

    private fun submitNotification() {
        notificationBuilder.edit {
            val notParticipated = contestantRank.participationType == CodeforcesParticipationType.NOT_PARTICIPATED

            setCustomContentView(viewSmall.apply {
                setTextViewText(
                    R.id.cf_monitor_rank,
                    if (notParticipated) "not participated"
                    else "rank: ${contestantRank.rank}"
                )
            })

            setCustomBigContentView(
                if (notParticipated) null else viewBig
            )
        }

        notificationBuilder.notify()
    }
}