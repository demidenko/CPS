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

    fun startReload() {
        if(isAutoUpdatable) return
        swipeRefreshLayout.isRefreshing = true
    }

    fun stopReload() {
        if(isAutoUpdatable) return
        swipeRefreshLayout.isRefreshing = false
    }

    private val newsFragment by lazy { requireParentFragment() as NewsFragment }
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
            (viewAdapter as CodeforcesNewsItemsAdapterAutoUpdatable).subscribeLiveData(this){
                lifecycleScope.launch { showItems() }
            }
        } else {
            swipeRefreshLayout.apply {
                setOnRefreshListener { callReload() }
                setProgressBackgroundColorSchemeResource(R.color.backgroundAdditional)
                setColorSchemeResources(R.color.colorAccent)
            }
        }

        subscribeNewEntries()
        subscribeRefreshOnRealColor()

        callReload()
    }

    abstract suspend fun parseData(lang: String): Boolean
    suspend fun reload(lang: String): Boolean {
        if(!parseData(lang)) return false

        with(viewAdapter){
            if(this is CodeforcesNewsItemsAdapterManagesNewEntries) newEntries.clear()
        }

        showItems()

        return true
    }

    fun onPageSelected(tab: TabLayout.Tab) {
        if(isManagesNewEntries) tab.badge?.run {
            if(hasNumber()){
                isVisible = true
                lifecycleScope.launch { saveEntries() }
            }
        }
    }
    fun onPageUnselected(tab: TabLayout.Tab) {
        if(isManagesNewEntries) tab.badge?.run {
            if(hasNumber()) isVisible = false
        }
    }

    private suspend fun showItems() {
        manageNewEntries()
        viewAdapter.notifyDataSetChanged()
    }

    private suspend fun manageNewEntries() {
        if(!isManagesNewEntries) return
        val savedBlogs = newsFragment.viewedDataStore.getBlogsViewed(title)
        with(viewAdapter as CodeforcesNewsItemsAdapterManagesNewEntries){
            val currentBlogs = getBlogIDs()
            for(id in newEntries.values()) if(id !in currentBlogs) newEntries.remove(id)
            val newBlogs = currentBlogs.filter { !savedBlogs.contains(it) }
            newEntries.addAll(newBlogs)
        }
    }

    private suspend fun saveEntries() {
        if(!isManagesNewEntries) return
        val toSave = (viewAdapter as CodeforcesNewsItemsAdapterManagesNewEntries).getBlogIDs()
        newsFragment.viewedDataStore.setBlogsViewed(title, toSave)
    }

    private fun subscribeNewEntries() {
        if(!isManagesNewEntries) return
        (viewAdapter as CodeforcesNewsItemsAdapterManagesNewEntries).newEntries.sizeLiveData.observe(viewLifecycleOwner){ count ->
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
                if (tab.isSelected && isVisible) lifecycleScope.launch { saveEntries() }
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