package com.example.test3

import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.*
import android.widget.ImageButton
import androidx.core.text.color
import androidx.core.view.isVisible
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.test3.news.*
import com.example.test3.news.codeforces.CodeforcesNewsFragment
import com.example.test3.news.codeforces.CodeforcesNewsViewModel
import com.example.test3.ui.BottomProgressInfo
import com.example.test3.ui.CPSFragment
import com.example.test3.utils.*
import com.example.test3.workers.CodeforcesNewsFollowWorker
import com.example.test3.workers.CodeforcesNewsLostRecentWorker
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import java.util.*
import java.util.concurrent.TimeUnit


enum class CodeforcesTitle {
    MAIN, TOP, RECENT, LOST
}

class NewsFragment : CPSFragment() {

    companion object {
        private const val keySelectedTab = "selected_tab"

        suspend fun getCodeforcesContentLanguage(context: Context) = if(context.settingsNews.getRussianContentEnabled()) "ru" else "en"
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
            setButtons(CodeforcesTitle.RECENT, recentSwitchButton to true, recentShowBackButton to false)
            setButtons(CodeforcesTitle.LOST, updateLostInfoButton to true)
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
        updateLostInfoButton.setOnClickListener { updateLostInfo() }

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED) {
            subscribeReloading(CodeforcesTitle.MAIN)
            subscribeReloading(CodeforcesTitle.TOP)
            subscribeReloading(CodeforcesTitle.RECENT)
        }

        selectPage(savedInstanceState)

    }

    private fun subscribeReloading(title: CodeforcesTitle) {
        lifecycleScope.launch {
            newsViewModel.getPageLoadingStateFlow(title).collect { loadingState ->
                val tab = tabLayout.getTabAt(codeforcesNewsAdapter.indexOf(title)) ?: return@collect
                tab.text = when(loadingState){
                    LoadingState.PENDING -> title.name
                    LoadingState.LOADING -> "..."
                    LoadingState.FAILED -> SpannableStringBuilder().color(failColor) { append(title.name) }
                }
                if(loadingState == LoadingState.LOADING){
                    sharedReloadButton.startReload(title.name)
                }else{
                    sharedReloadButton.stopReload(title.name)
                }
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
            R.id.menu_news_settings_button -> showSettingsNews()
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

    private val reloadButton by lazy { requireBottomPanel().findViewById<ImageButton>(R.id.navigation_news_reload) }
    private val sharedReloadButton by lazy { SharedReloadButton(reloadButton) }
    private val failColor by lazy { getColorFromResource(mainActivity, R.color.fail) }

    private fun reloadFragment(fragment: CodeforcesNewsFragment, lang: String) {
        newsViewModel.reload(fragment.title, lang)
    }

    suspend fun reloadFragment(fragment: CodeforcesNewsFragment) {
        reloadFragment(fragment, getCodeforcesContentLanguage(mainActivity))
    }

    fun getTab(title: CodeforcesTitle): TabLayout.Tab? {
        val index = codeforcesNewsAdapter.indexOf(title)
        return tabLayout.getTabAt(index)
    }


    private val updateLostInfoButton: ImageButton by lazy { requireBottomPanel().findViewById(R.id.navigation_news_lost_update_info) }

    val recentSwitchButton: ImageButton by lazy { requireBottomPanel().findViewById(R.id.navigation_news_recent_swap) }
    val recentShowBackButton: ImageButton by lazy { requireBottomPanel().findViewById(R.id.navigation_news_recent_show_blog_back) }

    private fun updateLostInfo() {
        lifecycleScope.launch {
            updateLostInfoButton.disable()
            CodeforcesNewsLostRecentWorker.updateInfo(mainActivity, BottomProgressInfo("update info of lost", mainActivity))
            updateLostInfoButton.enable()
        }
    }

    fun showSettingsNews(){
        mainActivity.cpsFragmentManager.pushBack(SettingsNewsFragment())
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
                    mainActivity.settingsNews.getDefaultTab()
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

    val viewedDataStore by lazy { CodeforcesNewsViewedBlogsDataStore(mainActivity) }

    class CodeforcesNewsViewedBlogsDataStore(context: Context): CPSDataStore(context.cf_blogs_viewed_dataStore){

        companion object {
            private val Context.cf_blogs_viewed_dataStore by preferencesDataStore("data_news_fragment_cf_viewed")
        }

        private fun makeKey(title: CodeforcesTitle) = stringSetPreferencesKey("blogs_viewed_${title.name}")

        fun blogsViewedFlow(title: CodeforcesTitle): Flow<Set<Int>> = dataStore.data.map {
            it[makeKey(title)]
                ?.map { str -> str.toInt() }
                ?.toSet()
                ?: emptySet()
        }

        suspend fun setBlogsViewed(title: CodeforcesTitle, blogIDs: Collection<Int>) {
            dataStore.edit { it[makeKey(title)] = blogIDs.mapTo(mutableSetOf()){ id -> id.toString() } }
        }
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

        newsFragment.cpsTitle = "::news.codeforces.${fragment.title.name.toLowerCase(Locale.ENGLISH)}"
    }
    
    fun onPageUnselected(tab: TabLayout.Tab) {
        val fragment = fragments[tab.position]
        fragment.onPageUnselected(tab)
        hideSupportButtons(fragment.title)
    }

    private val buttonsByTitle = mutableMapOf<CodeforcesTitle, List<ImageButton>>()
    private val buttonVisibility = mutableMapOf<Int, Boolean>()
    fun setButtons(title: CodeforcesTitle, vararg buttons: Pair<ImageButton,Boolean>){
        buttonsByTitle[title] = buttons.unzip().first
        buttons.forEach { (button, visibility) ->
            buttonVisibility[button.id] = visibility
        }
    }

    private fun hideSupportButtons(title: CodeforcesTitle) {
        buttonsByTitle[title]?.forEach { button ->
            buttonVisibility[button.id] = button.isVisible
            button.isVisible = false
        }
    }

    private fun showSupportButtons(title: CodeforcesTitle) {
        buttonsByTitle[title]?.forEach { button ->
            button.isVisible = buttonVisibility[button.id]!!
        }
    }
}


fun timeDifference(fromTimeSeconds: Long, toTimeSeconds: Long): String {
    val t = toTimeSeconds - fromTimeSeconds
    return when {
        t < TimeUnit.MINUTES.toSeconds(2) -> "minute"
        t <= TimeUnit.HOURS.toSeconds(2) -> "${TimeUnit.SECONDS.toMinutes(t)} minutes"
        t <= TimeUnit.HOURS.toSeconds(24 * 2) -> "${TimeUnit.SECONDS.toHours(t)} hours"
        else -> "${TimeUnit.SECONDS.toDays(t)} days"
    } + " ago"
}

fun timeRUtoEN(time: String): String{
    val s = time.split(' ')
    if(s.size == 3 && s.last()=="назад" && s.first().toIntOrNull()!=null){
        val w = s[1]
        return s.first() + " " + when {
            w.startsWith("сек") -> "seconds"
            w.startsWith("мин") -> "minutes"
            w.startsWith("час") -> "hours"
            w.startsWith("д") -> "days"
            w.startsWith("нед") -> "weeks"
            w.startsWith("мес") -> "months"
            w=="лет" || w=="год" -> "years"
            else -> w
        } + " ago"
    }
    return time
}
