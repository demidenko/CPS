package com.example.test3

import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.*
import android.widget.ImageButton
import androidx.core.text.color
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.test3.news.*
import com.example.test3.news.codeforces.CodeforcesNewsFragment
import com.example.test3.news.codeforces.CodeforcesNewsItemsRecentAdapter
import com.example.test3.ui.BottomProgressInfo
import com.example.test3.ui.CPSFragment
import com.example.test3.ui.observeUpdates
import com.example.test3.ui.settingsUI
import com.example.test3.utils.*
import com.example.test3.workers.CodeforcesNewsFollowWorker
import com.example.test3.workers.CodeforcesNewsLostRecentWorker
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
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
            setButtons(CodeforcesTitle.RECENT, recentSwitchButton to View.VISIBLE, recentShowBackButton to View.GONE)
            setButtons(CodeforcesTitle.LOST, updateLostInfoButton to View.VISIBLE)
        }
    }
    private val tabLayout by lazy { requireView().findViewById<TabLayout>(R.id.cf_news_tab_layout) }
    private val tabSelectionListener = object : TabLayout.OnTabSelectedListener{

        override fun onTabReselected(tab: TabLayout.Tab?) {}

        override fun onTabUnselected(tab: TabLayout.Tab?) {
            tab?.run {
                val fragment = codeforcesNewsAdapter.fragments[position]
                if(fragment.isManagesNewEntries) badge?.run {
                    if(hasNumber()) isVisible = false
                }

                codeforcesNewsAdapter.hideSupportButtons(fragment.title)
            }
        }

        override fun onTabSelected(tab: TabLayout.Tab?) {
            tab?.run {
                val fragment = codeforcesNewsAdapter.fragments[position]
                if(fragment.isManagesNewEntries) badge?.run {
                    if(hasNumber()){
                        isVisible = true
                        lifecycleScope.launch {
                            fragment.saveEntries()
                        }
                    }
                }

                codeforcesNewsAdapter.showSupportButtons(fragment.title)

                val subtitle = "::news.codeforces.${fragment.title.name.toLowerCase(Locale.ENGLISH)}"
                cpsTitle = subtitle
                if(this@NewsFragment.isVisible) mainActivity.setActionBarSubTitle(subtitle)
            }
        }
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
            val fragment = codeforcesNewsAdapter.fragments[position]
            tab.text = fragment.title.name
            if(fragment.isManagesNewEntries){
                tab.orCreateBadge.apply {
                    backgroundColor = badgeColor
                    isVisible = false
                }
            }
        }.attach()

        tabLayout.addOnTabSelectedListener(tabSelectionListener)

        setHasOptionsMenu(true)

        with(mainActivity){
            reloadButton.setOnClickListener { reloadTabs() }
            updateLostInfoButton.setOnClickListener { updateLostInfo() }
            recentSwitchButton.setOnClickListener {
                val fragment = codeforcesNewsAdapter.getFragment(CodeforcesTitle.RECENT) ?: return@setOnClickListener
                (fragment.viewAdapter as CodeforcesNewsItemsRecentAdapter).switchMode()
            }
            recentShowBackButton.setOnClickListener {
                val fragment = codeforcesNewsAdapter.getFragment(CodeforcesTitle.RECENT) ?: return@setOnClickListener
                (fragment.viewAdapter as CodeforcesNewsItemsRecentAdapter).closeShowFromBlog()
            }

            settingsUI.useRealColorsLiveData.observeUpdates(viewLifecycleOwner){ use ->
                codeforcesNewsAdapter.fragments.forEach { it.refresh() }
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

    private fun reloadTabs() {
        lifecycleScope.launch {
            val lang = getCodeforcesContentLanguage(mainActivity)
            codeforcesNewsAdapter.fragments.mapIndexed { index, fragment ->
                val tab = tabLayout.getTabAt(index)!!
                launch { reloadFragment(fragment, tab, lang) }
            }.joinAll()
        }
    }

    private val reloadButton by lazy { requireBottomPanel().findViewById<ImageButton>(R.id.navigation_news_reload) }
    private val sharedReloadButton by lazy { SharedReloadButton(reloadButton) }
    private val failColor by lazy { getColorFromResource(mainActivity, R.color.fail) }
    private suspend fun reloadFragment(
        fragment: CodeforcesNewsFragment,
        tab: TabLayout.Tab,
        lang: String
    ) {
        if(fragment.isAutoUpdatable) return
        sharedReloadButton.startReload(fragment.title.name)
        tab.text = "..."
        fragment.startReload()
        if(fragment.reload(lang)) {
            tab.text = fragment.title.name
        }else{
            tab.text = SpannableStringBuilder().color(failColor) { append(fragment.title.name) }
        }
        fragment.stopReload()
        sharedReloadButton.stopReload(fragment.title.name)
    }

    suspend fun reloadFragment(fragment: CodeforcesNewsFragment) {
        val index = codeforcesNewsAdapter.fragments.indexOf(fragment)
        val tab = tabLayout.getTabAt(index) ?: return
        reloadFragment(fragment, tab, getCodeforcesContentLanguage(mainActivity))
    }

    fun getTab(title: CodeforcesTitle): TabLayout.Tab? {
        val index = codeforcesNewsAdapter.indexOf(title)
        return tabLayout.getTabAt(index)
    }


    private val updateLostInfoButton by lazy { requireBottomPanel().findViewById<ImageButton>(R.id.navigation_news_lost_update_info) }

    private val recentSwitchButton by lazy { requireBottomPanel().findViewById<ImageButton>(R.id.navigation_news_recent_swap) }
    private val recentShowBackButton by lazy { requireBottomPanel().findViewById<ImageButton>(R.id.navigation_news_recent_show_blog_back) }

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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

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

        suspend fun getBlogsViewed(title: CodeforcesTitle): Set<Int> = dataStore.data.first()[makeKey(title)]?.mapTo(mutableSetOf()){ it.toInt() } ?: emptySet()
        suspend fun setBlogsViewed(title: CodeforcesTitle, blogIDs: Collection<Int>) {
            dataStore.edit { it[makeKey(title)] = blogIDs.mapTo(mutableSetOf()){ id -> id.toString() } }
        }
    }
}


class CodeforcesNewsAdapter(
    parentFragment: Fragment,
    val fragments: MutableList<CodeforcesNewsFragment>
) : FragmentStateAdapter(parentFragment) {

    override fun createFragment(position: Int) = fragments[position]
    override fun getItemCount() = fragments.size

    fun indexOf(title: CodeforcesTitle) = fragments.indexOfFirst { it.title == title }
    fun getFragment(title: CodeforcesTitle) = fragments.find { it.title == title }

    fun remove(index: Int){
        fragments.removeAt(index)
        notifyItemRemoved(index)
    }

    fun add(index: Int, fragment: CodeforcesNewsFragment){
        fragments.add(index, fragment)
        notifyItemInserted(index)
    }

    override fun getItemId(position: Int) = fragments[position].title.ordinal.toLong()


    private val tabButtons = mutableMapOf<CodeforcesTitle, List<ImageButton>>()
    private val buttonVisibility = mutableMapOf<Int, Int>()
    fun setButtons(title: CodeforcesTitle, vararg buttons: Pair<ImageButton,Int>){
        tabButtons[title] = buttons.unzip().first
        buttons.forEach { (button, visibility) ->
            buttonVisibility[button.id] = visibility
        }
    }

    fun hideSupportButtons(title: CodeforcesTitle) {
        tabButtons[title]?.forEach { button ->
            buttonVisibility[button.id] = button.visibility
            button.visibility = View.GONE
        }
    }

    fun showSupportButtons(title: CodeforcesTitle) {
        tabButtons[title]?.forEach { button ->
            button.visibility = buttonVisibility[button.id]!!
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
