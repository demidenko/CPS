package com.example.test3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.test3.contests.ContestsAdapter
import com.example.test3.contests.ContestsViewModel
import com.example.test3.ui.CPSFragment
import com.example.test3.ui.formatCPS
import com.example.test3.utils.CodeforcesContest
import com.example.test3.utils.LoadingState
import com.example.test3.utils.getCurrentTimeSeconds
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
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
            contestViewModel.flowOfCodeforcesContests().map { contests ->
                val currentTimeSeconds = getCurrentTimeSeconds()
                contests.filter { contest ->
                    currentTimeSeconds - contest.startTimeSeconds < TimeUnit.HOURS.toSeconds(48)
                }.sortedWith(
                    compareBy<CodeforcesContest> { it.phase }.thenBy { it.startTimeSeconds }
                )
            }
        )
        val recyclerView = view.findViewById<RecyclerView>(R.id.contests_list).formatCPS().apply {
            adapter = contestAdapter
        }

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.contests_list_swipe_refresh_layout).formatCPS().apply {
            setOnRefreshListener { callReload() }
        }

        addRepeatingJob(Lifecycle.State.STARTED) {
            contestViewModel.flowOfLoadingState().collect {
                swipeRefreshLayout.isRefreshing = it == LoadingState.LOADING
            }
        }

        callReload()
    }

    private fun callReload() {
        contestViewModel.reload()
    }
}