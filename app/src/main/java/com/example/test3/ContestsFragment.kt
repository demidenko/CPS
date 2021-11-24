package com.example.test3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.test3.contests.*
import com.example.test3.room.getContestsListDao
import com.example.test3.ui.CPSFragment
import com.example.test3.ui.enableIff
import com.example.test3.ui.flowAdapter
import com.example.test3.ui.formatCPS
import com.example.test3.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days

class ContestsFragment: CPSFragment() {

    private val contestViewModel: ContestsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_contests, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cpsTitle = "::contests"
        setBottomPanelId(R.id.support_navigation_contests, R.layout.navigation_contests)

        val contestAdapter = ContestsAdapter(
            this,
            makeContestsFlow()
        )

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.contests_list_swipe_refresh_layout).formatCPS().apply {
            setOnRefreshListener { callReload() }
        }

        val reloadButton: ImageButton
        val settingsButton: ImageButton
        val addCustomContestButton: ImageButton
        requireBottomPanel().apply {
            reloadButton = findViewById(R.id.navigation_contests_reload)
            settingsButton = findViewById(R.id.navigation_contests_settings)
            addCustomContestButton = findViewById(R.id.navigation_contests_add_custom)
        }

        reloadButton.setOnClickListener { callReload() }
        settingsButton.setOnClickListener { showContestsSettings() }
        addCustomContestButton.setOnClickListener { showCustomContestDialog() }

        launchAndRepeatWithViewLifecycle {
            contestViewModel.flowOfLoadingState().onEach {
                reloadButton.apply {
                    enableIff(it != LoadingState.LOADING)
                    if(it == LoadingState.FAILED) setColorFilter(getColorFromResource(context, R.color.fail))
                    else clearColorFilter()
                }
                swipeRefreshLayout.isRefreshing = it == LoadingState.LOADING
            }.launchIn(this)

            requireContext().settingsContests.let { dataStore ->
                val dao = getContestsListDao(requireContext())
                combine(dataStore.enabledPlatforms.flow, dataStore.lastReloadedPlatforms.flow) { a, b -> a to b }
                    .filter { (current, lastReloaded) -> current != lastReloaded }
                    .flowWithLifecycle(getHideShowLifecycleOwner().lifecycle, Lifecycle.State.RESUMED)
                    .onEach { (current, lastReloaded) ->
                        collectionsDifference(current, lastReloaded) { added, removed ->
                            removed.forEach { platform -> dao.remove(platform) }
                            dataStore.lastReloadedPlatforms(current)
                            contestViewModel.reload(added, requireContext())
                        }
                    }.launchIn(this)
            }

            requireContext().settingsDev.devEnabled.flow.onEach {
                addCustomContestButton.isVisible = it
            }.launchIn(this)
        }

        view.findViewById<RecyclerView>(R.id.contests_list).formatCPS().flowAdapter = contestAdapter
    }

    private fun callReload() {
        contestViewModel.reloadEnabledPlatforms(requireContext())
    }

    private fun showContestsSettings() {
        mainActivity.cpsFragmentManager.pushBack(ContestsSettingsFragment())
    }

    fun makeContestsFlow(): Flow<List<Contest>> {
        val contestsFlow = getContestsListDao(mainActivity).flowOfContests().map { contests ->
            val currentTime = getCurrentTime()
            contests.filter { contest ->
                currentTime - contest.endTime < 7.days
            }
        }
        val removedIdsFlow = mainActivity.settingsContests.removedContestsIds.flow
        return contestsFlow.combine(removedIdsFlow) { contests, removed ->
            contests.filter { it.getCompositeId() !in removed }
        }
    }

    private fun showCustomContestDialog() {
        val dialogView = mainActivity.layoutInflater.inflate(R.layout.dialog_add_custom_contest, null)
        val titleTextField = dialogView.findViewById<TextInputLayout>(R.id.dialog_custom_contest_title)
        val startTextField = dialogView.findViewById<TextInputLayout>(R.id.dialog_custom_contest_start)
        val endTextField = dialogView.findViewById<TextInputLayout>(R.id.dialog_custom_contest_end)

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US)
        val dateHint = "Use DD.MM.YYYY hh:mm"
        val dateIntervalError = "Start must be before End!"

        titleTextField.editText?.apply {
            doOnTextChanged { text, start, before, count ->
                titleTextField.error = if (text == null || text.isBlank()) "Blank title!" else null
            }
            setText("")
        }

        fun parseOrNull(text: String?): Date? {
            return try {
                text?.let { str ->
                    dateFormat.parse(str)?.let { date ->
                        if (dateFormat.format(date) == str) date
                        else null
                    }
                }
            } catch (e: ParseException) {
                null
            }
        }

        fun getDates(): Pair<Date?, Date?> {
            val startText = startTextField.editText?.getStringNotBlank()
            val endText = endTextField.editText?.getStringNotBlank()
            return parseOrNull(startText) to parseOrNull(endText)
        }

        fun onChange() {
            val (startDate, endDate) = getDates()
            if (startDate!=null && endDate!=null) {
                if (startDate.time < endDate.time) {
                    startTextField.error = null
                    endTextField.error = null
                } else {
                    startTextField.error = dateIntervalError
                    endTextField.error = dateIntervalError
                }
            } else {
                startTextField.error = if (startDate==null) dateHint else null
                endTextField.error = if (endDate==null) dateHint else null
            }
        }

        listOf(startTextField, endTextField).forEach { textField ->
            textField.editText?.apply {
                textField.helperText = dateHint
                doOnTextChanged { text, start, before, count ->
                    onChange()
                }
                setText("")
            }
        }

        val dialog = MaterialAlertDialogBuilder(mainActivity)
            .setView(dialogView)
            .setPositiveButton("add") { _, _ -> }
            .create()

        dialog.show()

        val fields = listOf(
            titleTextField,
            startTextField,
            endTextField
        )

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (fields.any { it.error != null }) return@setOnClickListener

            val (startDate, endDate) = getDates()

            val contest = Contest(
                platform = Contest.Platform.unknown,
                id = getCurrentTime().toString(),
                title = titleTextField.editText?.getStringNotBlank()!!,
                startTime = Instant.fromEpochMilliseconds(startDate!!.time),
                durationSeconds = TimeUnit.MILLISECONDS.toSeconds(endDate!!.time - startDate!!.time),
            )
            contestViewModel.addCustomContest(contest, requireContext())

            dialog.dismiss()
        }

    }
}