package com.example.test3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.test3.contests.ContestsAdapter
import com.example.test3.contests.ContestsSettingsFragment
import com.example.test3.contests.ContestsViewModel
import com.example.test3.contests.settingsContests
import com.example.test3.room.getContestsListDao
import com.example.test3.ui.CPSFragment
import com.example.test3.ui.enableIff
import com.example.test3.ui.flowAdapter
import com.example.test3.ui.formatCPS
import com.example.test3.utils.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

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
            getContestsListDao(mainActivity).flowOfContests().map { contests ->
                val currentTimeSeconds = getCurrentTimeSeconds()
                contests.filter { contest ->
                    currentTimeSeconds - contest.endTimeSeconds < TimeUnit.DAYS.toSeconds(7)
                }
            },
            previewType = 2
        )

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.contests_list_swipe_refresh_layout).formatCPS().apply {
            setOnRefreshListener { callReload() }
        }

        val reloadButton = requireBottomPanel().findViewById<ImageButton>(R.id.navigation_contests_reload).apply {
            setOnClickListener { callReload() }
        }

        val settingsButton = requireBottomPanel().findViewById<ImageButton>(R.id.navigation_contests_settings).apply {
            setOnClickListener { showContestsSettings() }
        }

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
        }

        view.findViewById<RecyclerView>(R.id.contests_list).formatCPS().flowAdapter = contestAdapter
    }

    private fun callReload() {
        contestViewModel.reloadEnabledPlatforms(requireContext())
    }

    private fun showContestsSettings() {
        mainActivity.cpsFragmentManager.pushBack(ContestsSettingsFragment())
    }
}