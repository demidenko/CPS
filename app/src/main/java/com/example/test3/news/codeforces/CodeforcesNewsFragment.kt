package com.example.test3.news.codeforces

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.test3.*
import com.example.test3.ui.settingsUI
import com.example.test3.utils.LoadingState
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

abstract class CodeforcesNewsFragment: Fragment() {

    abstract val title: CodeforcesTitle
    abstract val isManagesNewEntries: Boolean
    abstract val isAutoUpdatable: Boolean
    abstract val viewAdapter: CodeforcesNewsItemsAdapter<out RecyclerView.ViewHolder>

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_cf_news_page, container, false)
    }

    private val swipeRefreshLayout: SwipeRefreshLayout get() = requireView().findViewById(R.id.cf_news_page_swipe_refresh_layout)
    private val recyclerView: RecyclerView get() = swipeRefreshLayout.findViewById(R.id.cf_news_page_recyclerview)


    protected val newsFragment by lazy { requireParentFragment() as NewsFragment }
    private fun callReload() {
        lifecycleScope.launch { newsFragment.reloadFragment(this@CodeforcesNewsFragment) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView.apply {
            adapter = viewAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }

        if(isAutoUpdatable){
            swipeRefreshLayout.isEnabled = false
        } else {
            swipeRefreshLayout.apply {
                setOnRefreshListener { callReload() }
                setProgressBackgroundColorSchemeResource(R.color.backgroundAdditional)
                setColorSchemeResources(R.color.colorAccent)
            }
            newsFragment.newsViewModel.getPageLoadingStateLiveData(title).observe(viewLifecycleOwner){ loadingState ->
                swipeRefreshLayout.isRefreshing = loadingState == LoadingState.LOADING
            }
        }

        subscribeNewEntries()
        subscribeRefreshOnRealColor()

        if(savedInstanceState == null) callReload()
    }

    open fun onPageSelected(tab: TabLayout.Tab) { }
    open fun onPageUnselected(tab: TabLayout.Tab) { }


    protected fun saveEntries() {
        if(!isManagesNewEntries) return
        val toSave = (viewAdapter as CodeforcesBlogEntriesAdapter).getBlogIDs()
        lifecycleScope.launch {
            newsFragment.viewedDataStore.setBlogsViewed(title, toSave)
        }
    }

    private fun subscribeNewEntries() {
        if(!isManagesNewEntries) return
        (viewAdapter as CodeforcesBlogEntriesAdapter).getNewEntriesSize().observe(viewLifecycleOwner){ count ->
            val tab = newsFragment.getTab(title) ?: return@observe
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
                if (tab.isSelected && isVisible) saveEntries()
            }
        }
    }

    private fun subscribeRefreshOnRealColor() {
        newsFragment.mainActivity.settingsUI
            .useRealColorsLiveData
            .distinctUntilChanged()
            .observe(viewLifecycleOwner){ use ->
                viewAdapter.refresh()
            }
    }
}