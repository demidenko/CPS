package com.example.test3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.test3.contests.Contest
import com.example.test3.contests.ContestsAdapter
import com.example.test3.contests.ContestsViewModel
import com.example.test3.ui.CPSFragment
import com.example.test3.ui.enableIff
import com.example.test3.ui.formatCPS
import com.example.test3.utils.LoadingState
import com.example.test3.utils.getColorFromResource
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
            contestViewModel.flowOfContests().map { contests ->
                val currentTimeSeconds = getCurrentTimeSeconds()
                contests.filter { contest ->
                    currentTimeSeconds - contest.startTimeSeconds < TimeUnit.HOURS.toSeconds(48)
                }.sortedWith(
                    compareBy<Contest> { it.getPhase(currentTimeSeconds) }.thenBy {
                        when(it.getPhase(currentTimeSeconds)) {
                            Contest.Phase.BEFORE -> it.startTimeSeconds
                            Contest.Phase.RUNNING -> it.endTimeSeconds
                            Contest.Phase.FINISHED -> -it.endTimeSeconds
                        }
                    }
                )
            }
        )

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.contests_list_swipe_refresh_layout).formatCPS().apply {
            setOnRefreshListener { callReload() }
        }

        val reloadButton = requireBottomPanel().findViewById<ImageButton>(R.id.navigation_contests_reload).apply {
            setOnClickListener { callReload() }
        }

        addRepeatingJob(Lifecycle.State.STARTED) {
            contestViewModel.flowOfLoadingState().collect {
                swipeRefreshLayout.isRefreshing = it == LoadingState.LOADING
                reloadButton.apply {
                    enableIff(it != LoadingState.LOADING)
                    setColorFilter(getColorFromResource(context, if(it == LoadingState.FAILED) R.color.fail else R.color.textColor))
                }
            }
        }

        view.findViewById<RecyclerView>(R.id.contests_list).formatCPS().adapter = contestAdapter

    }

    private fun callReload() {
        contestViewModel.reload()
    }
}