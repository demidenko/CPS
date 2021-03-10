package com.example.test3.news.codeforces

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.test3.*
import com.example.test3.utils.CodeforcesAPI
import kotlinx.coroutines.launch

class CodeforcesNewsFragment: Fragment() {

    val title: CodeforcesTitle by lazy { CodeforcesTitle.valueOf(requireArguments().getString(keyTitle)!!) }
    val pageName: String by lazy { requireArguments().getString(keyPageName)!! }
    val isManagesNewEntries: Boolean by lazy { requireArguments().getBoolean(keyEntries) }
    val isAutoUpdatable: Boolean by lazy { requireArguments().getBoolean(keyAutoUpdate) }
    val viewAdapter: CodeforcesNewsItemsAdapter by lazy { CodeforcesNewsItemsAdapter.getFromType(requireArguments().getInt(keyAdapter)) }

    companion object {
        private const val keyTitle = "cf_news_title"
        private const val keyPageName = "cf_news_page_name"
        private const val keyEntries = "cf_news_entries"
        private const val keyAutoUpdate = "cf_news_auto_update"
        private const val keyAdapter = "cf_news_adapter"
        fun createInstance(title: CodeforcesTitle): CodeforcesNewsFragment {
            return when (title) {
                CodeforcesTitle.MAIN -> createInstance(title, "/", CodeforcesNewsItemsAdapter.typeClassic, isManagesNewEntries = true, isAutoUpdatable = false)
                CodeforcesTitle.TOP -> createInstance(title, "/top", CodeforcesNewsItemsAdapter.typeClassic, isManagesNewEntries = false, isAutoUpdatable = false)
                CodeforcesTitle.RECENT -> createInstance(title, "/recent-actions", CodeforcesNewsItemsAdapter.typeRecent, isManagesNewEntries = false, isAutoUpdatable = false)
                CodeforcesTitle.LOST -> createInstance(title, "", CodeforcesNewsItemsAdapter.typeLost, isManagesNewEntries = true, isAutoUpdatable = true)
            }
        }
        private fun createInstance(
            title: CodeforcesTitle,
            pageName: String,
            viewAdapterType: Int,
            isManagesNewEntries: Boolean,
            isAutoUpdatable: Boolean
        ): CodeforcesNewsFragment {
            return CodeforcesNewsFragment().apply {
                arguments = Bundle().apply {
                    putString(keyTitle, title.name)
                    putString(keyPageName, pageName)
                    putBoolean(keyEntries, isManagesNewEntries)
                    putBoolean(keyAutoUpdate, isAutoUpdatable)
                    putInt(keyAdapter, viewAdapterType)
                }
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

    private val swipeRefreshLayout: SwipeRefreshLayout by lazy { requireView().findViewById(R.id.cf_news_page_swipe_refresh_layout) }

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
        swipeRefreshLayout.findViewById<RecyclerView>(R.id.cf_news_page_recyclerview).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }

        if(isAutoUpdatable){
            swipeRefreshLayout.isEnabled = false
            (viewAdapter as? CodeforcesNewsItemsLostRecentAdapter)?.subscribeLiveData(this){
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

        callReload()
    }

    suspend fun reload(lang: String): Boolean {
        val source =
            if(pageName.startsWith('/')) CodeforcesAPI.getPageSource(pageName.substring(1), lang) ?: return false
            else ""

        if(!viewAdapter.parseData(source)) return false

        (viewAdapter as? CodeforcesNewsItemsClassicAdapter)?.clearNewEntries()

        showItems()

        return true
    }

    private suspend fun showItems() {
        manageNewEntries()
        viewAdapter.notifyDataSetChanged()
    }

    private suspend fun manageNewEntries() {
        if(!isManagesNewEntries) return
        val savedBlogs = newsFragment.viewedDataStore.getBlogsViewed(title)
        val newBlogs = viewAdapter.getBlogIDs().filter { !savedBlogs.contains(it) }.map { it.toInt() }
        (viewAdapter as? CodeforcesNewsItemsClassicAdapter)?.addNewEntries(newBlogs)
    }

    suspend fun saveEntries() {
        if(!isManagesNewEntries) return
        val toSave = viewAdapter.getBlogIDs().toSet()
        newsFragment.viewedDataStore.setBlogsViewed(title, toSave)
    }

    private fun subscribeNewEntries() {
        if(!isManagesNewEntries) return
        (viewAdapter as? CodeforcesNewsItemsClassicAdapter)?.getNewEntriesCountLiveData()?.observe(viewLifecycleOwner){ count ->
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
                if (tab.isSelected) lifecycleScope.launch { saveEntries() }
            }
        }
    }

    fun refresh() = viewAdapter.refresh()

}