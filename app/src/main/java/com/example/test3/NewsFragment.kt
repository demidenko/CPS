package com.example.test3

import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.*
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.color
import androidx.core.view.isVisible
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.test3.news.*
import com.example.test3.news.codeforces.CodeforcesNewsFragment
import com.example.test3.news.codeforces.CodeforcesNewsViewModel
import com.example.test3.ui.CPSFragment
import com.example.test3.ui.enableIff
import com.example.test3.ui.subscribeProgressBar
import com.example.test3.utils.*
import com.example.test3.workers.CodeforcesNewsFollowWorker
import com.example.test3.workers.CodeforcesNewsLostRecentWorker
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


enum class CodeforcesTitle {
    MAIN, TOP, RECENT, LOST
}

enum class CodeforcesLocale {
    EN, RU;

    override fun toString(): String {
        return when(this){
            EN -> "en"
            RU -> "ru"
        }
    }
}

class NewsFragment : CPSFragment() {

    companion object {
        private const val keySelectedTab = "selected_tab"

        suspend fun getCodeforcesContentLanguage(context: Context) = if(context.settingsNews.russianContentEnabled()) CodeforcesLocale.RU else CodeforcesLocale.EN
    }

    val newsViewModel: CodeforcesNewsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    private fun createLostFragment(): CodeforcesNewsFragment {
        return findOrCreateCodeforcesNewsFragment(CodeforcesTitle.LOST)
    }

    private fun findOrCreateCodeforcesNewsFragment(title: CodeforcesTitle): CodeforcesNewsFragment {
        return childFragmentManager.fragments.filterIsInstance<CodeforcesNewsFragment>()
            .find { it.title == title }
            ?: CodeforcesNewsFragment.createInstance(title)
    }

    private val codeforcesNewsAdapter: CodeforcesNewsAdapter by lazy {
        val fragments = mutableListOf(
            findOrCreateCodeforcesNewsFragment(CodeforcesTitle.MAIN),
            findOrCreateCodeforcesNewsFragment(CodeforcesTitle.TOP),
            findOrCreateCodeforcesNewsFragment(CodeforcesTitle.RECENT)
        )
        runBlocking {
            if(CodeforcesNewsLostRecentWorker.isEnabled(mainActivity)){
                fragments.add(createLostFragment())
            }
        }
        CodeforcesNewsAdapter(this, fragments).apply {
            setButtons(CodeforcesTitle.RECENT, R.id.support_navigation_news_recent, this@NewsFragment)
            setButtons(CodeforcesTitle.TOP, R.id.support_navigation_news_top, this@NewsFragment)
            setButtons(CodeforcesTitle.LOST, R.id.support_navigation_news_lost, this@NewsFragment)
        }
    }
    private val tabLayout by lazy { requireView().findViewById<TabLayout>(R.id.cf_news_tab_layout) }
    private val tabSelectionListener = object : TabLayout.OnTabSelectedListener{
        override fun onTabReselected(tab: TabLayout.Tab) {}
        override fun onTabUnselected(tab: TabLayout.Tab) = codeforcesNewsAdapter.onPageUnselected(tab)
        override fun onTabSelected(tab: TabLayout.Tab) = codeforcesNewsAdapter.onPageSelected(tab, this@NewsFragment)
    }

    private val codeforcesNewsViewPager by lazy {
        requireView().findViewById<ViewPager2>(R.id.cf_news_pager).apply {
            adapter = codeforcesNewsAdapter
            offscreenPageLimit = CodeforcesTitle.values().size
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cpsTitle = "::news"
        setBottomPanelId(R.id.support_navigation_news, R.layout.navigation_news)

        val badgeColor = getColorFromResource(mainActivity, R.color.newEntryColor)

        TabLayoutMediator(tabLayout, codeforcesNewsViewPager) { tab, position ->
            val fragment = codeforcesNewsAdapter.createFragment(position)
            tab.text = fragment.title.name
            tab.orCreateBadge.apply {
                backgroundColor = badgeColor
                isVisible = false
            }
        }.attach()

        tabLayout.addOnTabSelectedListener(tabSelectionListener)

        setHasOptionsMenu(true)

        reloadButton.setOnClickListener { reloadFragments() }

        updateLostInfoButton.setOnClickListener { newsViewModel.updateLostInfo(requireContext()) }
        mainActivity.subscribeProgressBar("update info of lost", newsViewModel.getUpdateLostInfoProgress())

        launchAndRepeatWithViewLifecycle {
            val titles = listOf(
                CodeforcesTitle.MAIN,
                CodeforcesTitle.TOP,
                CodeforcesTitle.RECENT
            ).onEach {
                launch {
                    subscribeReloading(it)
                }
            }

            LoadingState.combineLoadingStateFlows(titles.map { newsViewModel.flowOfPageLoadingState(it) })
                .distinctUntilChanged()
                .onEach { loadingState ->
                    reloadButton.enableIff(loadingState != LoadingState.LOADING)
                }.launchIn(this)

            newsViewModel.getUpdateLostInfoProgress().onEach { progress ->
                updateLostInfoButton.enableIff(progress == null)
            }.launchIn(this)

            mainActivity.settingsDev.devEnabled.flow.onEach { use ->
                suspectsLostButton.isVisible = use
            }.launchIn(this)
        }

        selectPage(savedInstanceState)

    }

    private suspend fun subscribeReloading(title: CodeforcesTitle) {
        newsViewModel.flowOfPageLoadingState(title).collect { loadingState ->
            val tab = tabLayout.getTabAt(codeforcesNewsAdapter.indexOf(title)) ?: return@collect
            tab.text = when(loadingState){
                LoadingState.PENDING -> title.name
                LoadingState.LOADING -> "..."
                LoadingState.FAILED -> SpannableStringBuilder().color(failColor) { append(title.name) }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            menu.setGroupDividerEnabled(true)
        }
        inflater.inflate(R.menu.menu_news, menu)
        menu.findItem(R.id.menu_news_follow_list).isVisible = runBlocking { CodeforcesNewsFollowWorker.isEnabled(mainActivity) }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_news_settings_button -> showNewsSettings()
            R.id.menu_news_follow_list -> showCodeforcesFollowListManager()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun reloadFragments() {
        lifecycleScope.launch {
            val lang = getCodeforcesContentLanguage(mainActivity)
            codeforcesNewsAdapter.forEach { fragment ->
                reloadFragment(fragment, lang)
            }
        }
    }

    private val reloadButton: ImageButton get() = requireBottomPanel().findViewById(R.id.navigation_news_reload)
    private val failColor by lazy { getColorFromResource(mainActivity, R.color.fail) }

    private fun reloadFragment(fragment: CodeforcesNewsFragment, locale: CodeforcesLocale) {
        newsViewModel.reload(fragment.title, locale)
    }

    suspend fun reloadFragment(fragment: CodeforcesNewsFragment) {
        reloadFragment(fragment, getCodeforcesContentLanguage(mainActivity))
    }

    fun getTab(title: CodeforcesTitle): TabLayout.Tab? {
        val index = codeforcesNewsAdapter.indexOf(title)
        return tabLayout.getTabAt(index)
    }


    val recentSwitchButton: ImageButton by lazy { requireBottomPanel().findViewById(R.id.navigation_news_recent_swap) }
    val topCommentsButton: ImageButton by lazy { requireBottomPanel().findViewById(R.id.navigation_news_top_comments) }
    private val updateLostInfoButton: ImageButton by lazy { requireBottomPanel().findViewById(R.id.navigation_news_lost_update_info) }
    val suspectsLostButton: ImageButton by lazy { requireBottomPanel().findViewById(R.id.navigation_news_lost_suspects) }

    fun showNewsSettings(){
        mainActivity.cpsFragmentManager.pushBack(NewsSettingsFragment())
    }

    fun showCodeforcesFollowListManager(){
        mainActivity.cpsFragmentManager.pushBack(ManageCodeforcesFollowListFragment())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(keySelectedTab, tabLayout.selectedTabPosition)
    }

    private fun selectPage(savedInstanceState: Bundle?) {

        val index = savedInstanceState?.getInt(keySelectedTab)
            ?: with(codeforcesNewsAdapter){
                val defaultTab = runBlocking {
                    mainActivity.settingsNews.defaultTab()
                }
                indexOf(defaultTab).takeIf { it!=-1 } ?: indexOf(CodeforcesTitle.TOP)
            }

        tabLayout.getTabAt(index)?.let{
            if(it.isSelected) tabSelectionListener.onTabSelected(it)
            else tabLayout.selectTab(tabLayout.getTabAt(index))
        }

        codeforcesNewsViewPager.setCurrentItem(index, false)
    }

    fun addLostTab(){
        with(codeforcesNewsAdapter){
            if(indexOf(CodeforcesTitle.LOST) != -1) return
            val index = indexOf(CodeforcesTitle.RECENT)
            add(index+1, createLostFragment())
        }
    }

    fun removeLostTab(){
        val index = codeforcesNewsAdapter.indexOf(CodeforcesTitle.LOST)
        if(index == -1) return
        tabLayout.getTabAt(index)?.let { tabLost ->
            if(tabLost.isSelected){
                tabLayout.selectTab(tabLayout.getTabAt(codeforcesNewsAdapter.indexOf(CodeforcesTitle.RECENT)))
            }
        }
        codeforcesNewsAdapter.remove(index)
    }

    val viewedDataStore by lazy { CodeforcesNewsViewedBlogEntriesDataStore(mainActivity) }

    class CodeforcesNewsViewedBlogEntriesDataStore(context: Context): CPSDataStore(context.cf_viewed_blog_entries_dataStore){
        companion object {
            private val Context.cf_viewed_blog_entries_dataStore by preferencesDataStore("data_news_fragment_cf_viewed")
        }

        private val itemsByTitle = mutableMapOf<CodeforcesTitle, ItemStringConvertible<Set<Int>>>()
        private fun itemByTitle(title: CodeforcesTitle) = itemsByTitle.getOrPut(title) {
            itemJsonConvertible(jsonCPS, "blog_entries_${title.name}", emptySet())
        }

        fun flowOfViewedBlogEntries(title: CodeforcesTitle): Flow<Set<Int>> = itemByTitle(title).flow

        suspend fun setViewedBlogEntries(title: CodeforcesTitle, blogEntriesIds: Collection<Int>) =
            itemByTitle(title)(blogEntriesIds.toSet())
    }
}


class CodeforcesNewsAdapter(
    parentFragment: Fragment,
    private val fragments: MutableList<CodeforcesNewsFragment>
) : FragmentStateAdapter(parentFragment) {

    override fun createFragment(position: Int) = fragments[position]
    override fun getItemCount() = fragments.size
    override fun getItemId(position: Int) = fragments[position].title.ordinal.toLong()

    fun indexOf(title: CodeforcesTitle) = fragments.indexOfFirst { it.title == title }
    fun forEach(action: (CodeforcesNewsFragment) -> Unit) = fragments.forEach(action)

    fun remove(index: Int){
        fragments.removeAt(index)
        notifyItemRemoved(index)
    }

    fun add(index: Int, fragment: CodeforcesNewsFragment){
        fragments.add(index, fragment)
        notifyItemInserted(index)
    }

    
    fun onPageSelected(tab: TabLayout.Tab, newsFragment: NewsFragment) {
        val fragment = fragments[tab.position]
        fragment.onPageSelected(tab)
        showSupportButtons(fragment.title)

        newsFragment.cpsTitle = "::news.codeforces.${fragment.title.name.lowercase()}"
    }
    
    fun onPageUnselected(tab: TabLayout.Tab) {
        val fragment = fragments[tab.position]
        fragment.onPageUnselected(tab)
        hideSupportButtons(fragment.title)
    }

    private val buttonsByTitle = mutableMapOf<CodeforcesTitle, ConstraintLayout>()
    fun setButtons(title: CodeforcesTitle, resid: Int, newsFragment: NewsFragment){
        with(newsFragment.requireBottomPanel()){
            buttonsByTitle[title] = findViewById(resid)
        }
    }

    private fun hideSupportButtons(title: CodeforcesTitle) {
        buttonsByTitle[title]?.isVisible = false
    }

    private fun showSupportButtons(title: CodeforcesTitle) {
        buttonsByTitle[title]?.isVisible = true
    }
}


fun timeDifference(fromTimeSeconds: Long, toTimeSeconds: Long): String {
    val t: Duration = (toTimeSeconds - fromTimeSeconds).seconds
    return when {
        t < 2.minutes -> "minute"
        t < 2.hours -> "${t.inWholeMinutes} minutes"
        t < 24.hours * 2 -> "${t.inWholeHours} hours"
        t < 7.days * 2 -> "${t.inWholeDays} days"
        t < 31.days * 2 -> "${t.inWholeDays / 7} weeks"
        t < 365.days * 2 -> "${t.inWholeDays / 31} months"
        else -> "${t.inWholeDays / 365} years"
    }
}

fun timeAgo(fromTimeSeconds: Long, toTimeSeconds: Long) = timeDifference(fromTimeSeconds, toTimeSeconds) + " ago"
fun timeAgo(fromTime: Instant, toTime: Instant) = timeDifference(fromTime.epochSeconds, toTime.epochSeconds) + " ago"

fun timeDifference2(fromTimeSeconds: Long, toTimeSeconds: Long): String {
    val t: Duration = (toTimeSeconds - fromTimeSeconds).seconds
    if(t < 24.hours * 2) return durationHHMMSS(t)
    return timeDifference(fromTimeSeconds, toTimeSeconds)
}

