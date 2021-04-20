package com.example.test3.news.codeforces

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.test3.*
import com.example.test3.news.codeforces.adapters.CodeforcesBlogEntriesAdapter
import com.example.test3.ui.settingsUI
import com.example.test3.utils.LoadingState
import com.example.test3.utils.ignoreFirst
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

abstract class CodeforcesNewsFragment: Fragment() {

    abstract val title: CodeforcesTitle

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_cf_news_page, container, false)
    }

    protected val swipeRefreshLayout: SwipeRefreshLayout get() = requireView().findViewById(R.id.cf_news_page_swipe_refresh_layout)
    protected val recyclerView: RecyclerView get() = swipeRefreshLayout.findViewById(R.id.cf_news_page_recyclerview)


    protected val newsFragment get() = requireParentFragment() as NewsFragment
    protected fun callReload() {
        lifecycleScope.launch { newsFragment.reloadFragment(this@CodeforcesNewsFragment) }
    }


    open fun onPageSelected(tab: TabLayout.Tab) { }
    open fun onPageUnselected(tab: TabLayout.Tab) { }


    protected fun saveBlogEntriesAsViewed(blogEntriesAdapter: CodeforcesBlogEntriesAdapter) {
        val toSave = blogEntriesAdapter.getBlogIDs()
        lifecycleScope.launch {
            newsFragment.viewedDataStore.setBlogsViewed(title, toSave)
        }
    }

    protected fun subscribeNewEntries(blogEntriesAdapter: CodeforcesBlogEntriesAdapter) {
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED){
            blogEntriesAdapter.getNewEntriesSizeFlow().collect { count ->
                val tab = newsFragment.getTab(title) ?: return@collect
                if (count == 0) {
                    tab.badge?.apply {
                        isVisible = false
                        clearNumber()
                    }
                } else {
                    tab.badge?.apply {
                        number = count
                        isVisible = true
                    }
                    if (tab.isSelected && isVisible) saveBlogEntriesAsViewed(blogEntriesAdapter)
                }
            }
        }
    }

    protected fun subscribeRefreshOnRealColor(refresh: suspend (Boolean) -> Unit) {
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED) {
            newsFragment.mainActivity.settingsUI.getUseRealColorsFlow().ignoreFirst().collect(refresh)
        }
    }

    protected fun subscribeLoadingState(loadingState: Flow<LoadingState>, swipeRefreshLayout: SwipeRefreshLayout) {
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED) {
            loadingState.distinctUntilChanged().collect { swipeRefreshLayout.isRefreshing = it == LoadingState.LOADING }
        }
    }

    companion object {
        fun createInstance(title: CodeforcesTitle): CodeforcesNewsFragment {
            return when (title) {
                CodeforcesTitle.MAIN -> CodeforcesNewsMainFragment()
                CodeforcesTitle.TOP -> CodeforcesNewsTopFragment()
                CodeforcesTitle.RECENT -> CodeforcesNewsRecentFragment()
                CodeforcesTitle.LOST -> CodeforcesNewsLostFragment()
            }
        }
    }
}
