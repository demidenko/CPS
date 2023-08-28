package com.demich.cps.ui.bottomprogressbar

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.utils.sharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds


@Immutable
data class ProgressBarInfo(
    val total: Int,
    val current: Int = 0,
    val title: String = ""
) {
    val fraction: Float get() = current.toFloat() / total

    operator fun inc(): ProgressBarInfo = copy(current = current + 1)
}

@Composable
fun progressBarsViewModel(): ProgressBarsViewModel = sharedViewModel()

class ProgressBarsViewModel: ViewModel() {
    //TODO: to StateFlows
    private val progressIds = mutableStateListOf<String>()
    val progressBarsIdsList: List<String> get() = progressIds

    private val progressStates = mutableMapOf<String, MutableState<ProgressBarInfo>>()

    fun progressState(id: String): State<ProgressBarInfo> = progressStates.getValue(id)

    fun doJob(
        id: String,
        coroutineScope: CoroutineScope = viewModelScope,
        block: suspend CoroutineScope.(MutableState<ProgressBarInfo>) -> Unit
    ) {
        require(id !in progressStates) { "progress bar with id=$id is already started" }
        coroutineScope.launch {
            val progressState = progressStates.getOrPut(id) { mutableStateOf(ProgressBarInfo(total = 0)) }
            progressIds.add(id)
            block(progressState)
            if (progressState.value.total > 0) delay(1.seconds)
            progressIds.remove(id)
            progressStates.remove(id)
        }
    }

    val clistImportIsRunning: Boolean get() = clistImportId in progressIds

    companion object {
        const val clistImportId = "clist_import"
    }
}