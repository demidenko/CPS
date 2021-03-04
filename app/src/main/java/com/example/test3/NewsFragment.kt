package com.example.test3

import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.color
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.test3.news.ManageCodeforcesFollowListFragment
import com.example.test3.news.SettingsNewsFragment
import com.example.test3.news.settingsNews
import com.example.test3.room.getLostBlogsDao
import com.example.test3.utils.*
import com.example.test3.workers.CodeforcesNewsFollowWorker
import com.example.test3.workers.CodeforcesNewsLostRecentWorker
import com.google.android.material.snackbar.Snackbar
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
            tab?.run{
                val fragment = codeforcesNewsAdapter.fragments[position]
                if(fragment.isManagesNewEntries) badge?.run{
                    if(hasNumber()){
                        isVisible = false
                        clearNumber()
                    }
                }

                codeforcesNewsAdapter.makeGONE(fragment.title)
            }
        }

        override fun onTabSelected(tab: TabLayout.Tab?) {
            tab?.run{
                val fragment = codeforcesNewsAdapter.fragments[position]
                if(fragment.isManagesNewEntries) badge?.run{
                    if(hasNumber()){
                        lifecycleScope.launch {
                            fragment.saveEntries()
                        }
                    }
                }

                codeforcesNewsAdapter.makeVISIBLE(fragment.title)

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
        setBottomPanelId(R.id.support_navigation_news)

        val badgeColor = getColorFromResource(mainActivity, android.R.color.holo_green_light)

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
    private val failColor by lazy { getColorFromResource(mainActivity, R.color.reload_fail) }
    private suspend fun reloadFragment(
        fragment: CodeforcesNewsFragment,
        tab: TabLayout.Tab,
        lang: String
    ) {
        sharedReloadButton.startReload(fragment.title.name)
        tab.text = "..."
        fragment.startReload()
        if(fragment.reload(lang)) {
            tab.text = fragment.title.name
            fragment.afterReload(tab)
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


    private val updateLostInfoButton by lazy { requireBottomPanel().findViewById<ImageButton>(R.id.navigation_news_lost_update_info) }

    private val recentSwitchButton by lazy { requireBottomPanel().findViewById<ImageButton>(R.id.navigation_news_recent_swap) }
    private val recentShowBackButton by lazy { requireBottomPanel().findViewById<ImageButton>(R.id.navigation_news_recent_show_blog_back) }

    private fun updateLostInfo() {
        lifecycleScope.launch {
            updateLostInfoButton.isEnabled = false
            CodeforcesNewsLostRecentWorker.updateInfo(mainActivity, BottomProgressInfo("update info of lost", mainActivity))
            updateLostInfoButton.isEnabled = true

            codeforcesNewsAdapter.indexOf(CodeforcesTitle.LOST).takeIf { it!=-1 }
                ?.let { index ->
                    val tab = tabLayout.getTabAt(index) ?: return@let
                    val fragment = codeforcesNewsAdapter.fragments[index]
                    reloadFragment(fragment, tab, "")
                }
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

    class CodeforcesNewsViewedBlogsDataStore(context: Context)
        : SettingsDataStore(context, "data_news_fragment_cf_viewed"){

        private fun makeKey(title: CodeforcesTitle) = stringSetPreferencesKey("blogs_viewed_${title.name}")

        suspend fun getBlogsViewed(title: CodeforcesTitle) = dataStore.data.first()[makeKey(title)] ?: emptySet()
        suspend fun setBlogsViewed(title: CodeforcesTitle, blogIDs: Set<String>) {
            dataStore.edit { it[makeKey(title)] = blogIDs }
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

    fun makeGONE(title: CodeforcesTitle) {
        tabButtons[title]?.forEach { button ->
            buttonVisibility[button.id] = button.visibility
            button.visibility = View.GONE
        }
    }

    fun makeVISIBLE(title: CodeforcesTitle) {
        tabButtons[title]?.forEach { button ->
            button.visibility = buttonVisibility[button.id]!!
        }
    }
}

class CodeforcesNewsFragment: Fragment() {

    val title: CodeforcesTitle by lazy { CodeforcesTitle.valueOf(requireArguments().getString(keyTitle)!!) }
    val pageName: String by lazy { requireArguments().getString(keyPageName)!! }
    val isManagesNewEntries: Boolean by lazy { requireArguments().getBoolean(keyEntries) }
    val viewAdapter: CodeforcesNewsItemsAdapter by lazy { CodeforcesNewsItemsAdapter.getFromType(requireArguments().getInt(keyAdapter)) }

    companion object {
        private const val keyTitle = "cf_news_title"
        private const val keyPageName = "cf_news_page_name"
        private const val keyEntries = "cf_news_entries"
        private const val keyAdapter = "cf_news_adapter"
        fun createInstance(title: CodeforcesTitle): CodeforcesNewsFragment {
            val (pageName, isManagesNewEntries, viewAdapterType) =
            when (title) {
                CodeforcesTitle.MAIN -> Triple("/", true, CodeforcesNewsItemsAdapter.typeClassic)
                CodeforcesTitle.TOP -> Triple("/top", false, CodeforcesNewsItemsAdapter.typeClassic)
                CodeforcesTitle.RECENT -> Triple("/recent-actions", false, CodeforcesNewsItemsAdapter.typeRecent)
                CodeforcesTitle.LOST -> Triple("", true, CodeforcesNewsItemsAdapter.typeLost)
            }
            return CodeforcesNewsFragment().apply {
                arguments = Bundle().apply {
                    putString(keyTitle, title.name)
                    putString(keyPageName, pageName)
                    putBoolean(keyEntries, isManagesNewEntries)
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
        swipeRefreshLayout.isRefreshing = true
    }

    fun stopReload() {
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

        swipeRefreshLayout.apply {
            setOnRefreshListener { callReload() }
            setProgressBackgroundColorSchemeResource(R.color.textColor)
            setColorSchemeResources(R.color.colorAccent)
        }

        callReload()
    }

    private fun showItems() = viewAdapter.notifyDataSetChanged()

    suspend fun reload(lang: String): Boolean {
        val source =
            if(pageName.startsWith('/')) CodeforcesAPI.getPageSource(pageName.substring(1), lang) ?: return false
            else ""
        if(!viewAdapter.parseData(source)) return false

        showItems()

        return true
    }

    private var newBlogs = hashSetOf<String>()

    suspend fun afterReload(tab: TabLayout.Tab){
        if(!isManagesNewEntries) return

        val savedBlogs = newsFragment.viewedDataStore.getBlogsViewed(title)
        newBlogs = viewAdapter.getBlogIDs().filter { !savedBlogs.contains(it) }.toHashSet()

        if (newBlogs.size == 0) {
            tab.badge?.apply {
                isVisible = false
                clearNumber()
            }
        } else {
            tab.badge?.apply {
                number = newBlogs.size
                isVisible = true
            }
            if (tab.isSelected) saveEntries()
        }
    }

    suspend fun saveEntries() {
        if(!isManagesNewEntries) return
        val toSave = viewAdapter.getBlogIDs().toSet()
        newsFragment.viewedDataStore.setBlogsViewed(title, toSave)
        (viewAdapter as? CodeforcesNewsItemsClassicAdapter)?.markNewEntries(newBlogs)
    }

    fun refresh(){
        viewAdapter.refresh()
    }

}


///---------------data adapters--------------------

abstract class CodeforcesNewsItemsAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    abstract suspend fun parseData(s: String): Boolean
    abstract fun getBlogIDs(): List<String>

    protected lateinit var activity: MainActivity
    protected lateinit var recyclerView: RecyclerView
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        activity = recyclerView.context as MainActivity
        this.recyclerView = recyclerView
    }

    open fun refresh(){
        notifyDataSetChanged()
    }

    companion object {
        const val typeClassic = 0
        const val typeRecent = 1
        const val typeLost = 2
        fun getFromType(type: Int): CodeforcesNewsItemsAdapter {
            return when(type) {
                typeClassic -> CodeforcesNewsItemsClassicAdapter()
                typeRecent -> CodeforcesNewsItemsRecentAdapter()
                typeLost -> CodeforcesNewsItemsLostRecentAdapter()
                else -> throw Exception("Unknown type of CodeforcesNewsItemsAdapter: $type")
            }
        }
    }
}

open class CodeforcesNewsItemsClassicAdapter: CodeforcesNewsItemsAdapter(){

    data class Info(
        val blogId: Int,
        val title: String,
        val author: String,
        val authorColorTag: CodeforcesUtils.ColorTag,
        val time: String,
        val comments: String,
        val rating: String,
        var isNew: Boolean = false
    )

    override suspend fun parseData(s: String): Boolean {
        val res = arrayListOf<Info>()
        var i = 0
        while (true) {
            i = s.indexOf("<div class=\"topic\"", i + 1)
            if (i == -1) break

            val title = fromHTML(s.substring(s.indexOf("<p>", i) + 3, s.indexOf("</p>", i))).toString()

            i = s.indexOf("entry/", i)
            val id = s.substring(i+6, s.indexOf('"',i)).toInt()

            i = s.indexOf("<div class=\"info\"", i)
            i = s.indexOf("/profile/", i)
            val author = s.substring(i+9,s.indexOf('"',i))

            i = s.indexOf("rated-user user-",i)
            val authorColorTag = CodeforcesUtils.ColorTag.fromString(
                s.substring(s.indexOf(' ',i)+1, s.indexOf('"',i))
            )

            i = s.indexOf("<span class=\"format-humantime\"", i)
            val time = s.substring(s.indexOf('>',i)+1, s.indexOf("</span>",i))

            i = s.indexOf("<div class=\"roundbox meta\"", i)
            i = s.indexOf("</span>", i)
            val rating = s.substring(s.lastIndexOf('>',i-1)+1,i)

            i = s.indexOf("<div class=\"right-meta\">", i)
            i = s.indexOf("</ul>", i)
            i = s.lastIndexOf("</a>", i)
            val comments = s.substring(s.lastIndexOf('>',i-1)+1,i).trim()

            res.add(Info(id,title,author,authorColorTag,time,comments,rating))
        }

        if(res.isNotEmpty()){
            rows = res.toTypedArray()
            return true
        }

        return false
    }


    class CodeforcesNewsItemViewHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view){
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        val time: TextView = view.findViewById(R.id.news_item_time)
        val rating: TextView = view.findViewById(R.id.news_item_rating)
        val comments: TextView = view.findViewById(R.id.news_item_comments)
        val commentsIcon: ImageView = view.findViewById(R.id.news_item_comment_icon)
        val newEntryIndicator: View = view.findViewById(R.id.news_item_dot_new)
    }

    protected var rows: Array<Info> = emptyArray()

    override fun getItemCount() = rows.size


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeforcesNewsItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_item, parent, false) as ConstraintLayout
        return CodeforcesNewsItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        with(holder as CodeforcesNewsItemViewHolder){
            val info = rows[position]

            view.setOnClickListener {
                activity.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.blog(info.blogId)))
                if(info.isNew){
                    info.isNew = false
                    notifyItemChanged(position)
                }
            }

            view.isLongClickable = true
            view.setOnLongClickListener {
                runBlocking { CodeforcesNewsFollowWorker.isEnabled(activity) }.apply {
                    if(this) addToFollowListWithSnackBar(this@with)
                }
            }

            title.text = info.title

            author.text = activity.accountsFragment.codeforcesAccountManager.makeSpan(info.author, info.authorColorTag)

            time.text = timeRUtoEN(info.time)

            newEntryIndicator.visibility = if(info.isNew) View.VISIBLE else View.GONE

            comments.text = info.comments
            commentsIcon.visibility = if(info.comments.isEmpty()) View.INVISIBLE else View.VISIBLE

            rating.apply{
                text = info.rating
                setTextColor(getColorFromResource(activity,
                    if(info.rating.startsWith('+')) R.color.blog_rating_positive
                    else R.color.blog_rating_negative
                ))
            }
        }
    }

    override fun getBlogIDs(): List<String> = rows.map { it.blogId.toString() }

    fun markNewEntries(newBlogs: HashSet<String>){
        rows.forEachIndexed { index, info ->
            val isNew = info.blogId.toString() in newBlogs
            if(rows[index].isNew != isNew){
                rows[index].isNew = isNew
                notifyItemChanged(index)
            }
        }
    }

    private fun addToFollowListWithSnackBar(holder: CodeforcesNewsItemViewHolder){
        activity.newsFragment.lifecycleScope.launch {
            val connector = CodeforcesNewsFollowWorker.FollowDataConnector(activity)
            val handle = holder.author.text
            when(connector.add(handle.toString())){
                true -> {
                    Snackbar.make(holder.view, SpannableStringBuilder("You now followed ").append(handle), Snackbar.LENGTH_LONG).apply {
                        setAction("Manage"){
                            activity.newsFragment.showCodeforcesFollowListManager()
                        }
                    }
                }
                false -> {
                    Snackbar.make(holder.view, SpannableStringBuilder("You already followed ").append(handle), Snackbar.LENGTH_LONG)
                }
            }.setAnchorView(activity.navigation).show()
        }
    }
}


class CodeforcesNewsItemsRecentAdapter: CodeforcesNewsItemsAdapter(){

    class BlogInfo(
        val blogId: Int,
        val title: String,
        val author: String,
        val authorColorTag: CodeforcesUtils.ColorTag,
        val lastCommentId: Long,
        var commentators: List<Spannable>
    )

    private var rows: Array<BlogInfo> = emptyArray()
    private var rowsComments: Array<CodeforcesRecentAction> = emptyArray()
    private val blogComments = mutableMapOf<Int, MutableList<CodeforcesComment>>()

    private fun calculateCommentatorsSpans(blogID: Int): List<Spannable> {
        return runBlocking {
            blogComments[blogID]
                ?.distinctBy { it.commentatorHandle }
                ?.map { comment ->
                    activity.accountsFragment.codeforcesAccountManager.makeSpan(
                        comment.commentatorHandle,
                        comment.commentatorHandleColorTag
                    )
                } ?: emptyList()
        }
    }

    override fun refresh() {
        showHeader()
        rows.forEachIndexed { index, blogInfo ->
            rows[index].commentators = calculateCommentatorsSpans(blogInfo.blogId)
        }
        super.refresh()
    }

    override suspend fun parseData(s: String): Boolean {
        val (blogs, comments) = CodeforcesUtils.parseRecentActionsPage(s)

        blogComments.clear()
        comments.forEach { recentAction ->
            blogComments.getOrPut(recentAction.blogEntry!!.id){ mutableListOf() }.add(recentAction.comment!!)
        }

        val res = blogs.map { blog ->
            BlogInfo(blog.id, blog.title, blog.authorHandle, blog.authorColorTag,
                lastCommentId = blogComments[blog.id]?.first()?.id ?: -1,
                commentators = calculateCommentatorsSpans(blog.id)
            )
        }

        if(res.isNotEmpty()){
            rows = res.toTypedArray()
            rowsComments = comments.toTypedArray()
            return true
        }
        return false
    }

    class CodeforcesNewsBlogItemViewHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view){
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        val comments: TextView = view.findViewById(R.id.news_item_comments)
        val commentsIcon: ImageView = view.findViewById(R.id.news_item_comment_icon)
    }

    class CodeforcesNewsCommentItemViewHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view){
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        val time: TextView = view.findViewById(R.id.news_item_time)
        val rating: TextView = view.findViewById(R.id.news_item_rating)
        val comment: TextView = view.findViewById(R.id.news_item_comment_content)
    }

    override fun getItemCount(): Int {
        return headerBlog?.run {
            rowsComments.count { recentAction ->
                recentAction.blogEntry!!.id == blogId
            }
        } ?: if (modeGrouped) rows.size else rowsComments.size
    }

    private var headerBlog: BlogInfo? = null
    private lateinit var header: View
    private val switchButton: ImageButton by lazy { activity.navigation.findViewById(R.id.navigation_news_recent_swap) }
    private val showBackButton: ImageButton by lazy { activity.navigation.findViewById(R.id.navigation_news_recent_show_blog_back) }
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        with(recyclerView.parent.parent as ConstraintLayout){
            header = findViewById(R.id.cf_news_page_header)
        }
    }

    private fun showHeader(){
        if(headerBlog!=null) header.apply {
            val info = headerBlog!!
            findViewById<TextView>(R.id.news_item_title).text = info.title
            findViewById<TextView>(R.id.news_item_author).text = activity.accountsFragment.codeforcesAccountManager.makeSpan(info.author, info.authorColorTag)
            visibility = View.VISIBLE
        } else {
            header.visibility = View.GONE
        }
    }

    fun showFromBlog(info: BlogInfo){
        switchButton.visibility = View.GONE
        showBackButton.visibility = View.VISIBLE
        headerBlog = info
        showHeader()
        notifyDataSetChanged()
        recyclerView.scrollToPosition(0)
    }
    fun closeShowFromBlog(){
        showBackButton.visibility = View.GONE
        switchButton.visibility = View.VISIBLE
        headerBlog = null
        showHeader()
        notifyDataSetChanged()
    }

    private var modeGrouped = true
    fun switchMode(){
        modeGrouped = !modeGrouped
        if(modeGrouped) switchButton.setImageResource(R.drawable.ic_recent_mode_comments)
        else switchButton.setImageResource(R.drawable.ic_recent_mode_grouped)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (headerBlog!=null || !modeGrouped) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_recent_comment, parent, false) as ConstraintLayout
            return CodeforcesNewsCommentItemViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_recent_item, parent, false) as ConstraintLayout
            return CodeforcesNewsBlogItemViewHolder(view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (headerBlog!=null || !modeGrouped) 1 else 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder is CodeforcesNewsBlogItemViewHolder){
            onBindViewBlogHolder(holder, position)
        }else
        if(holder is CodeforcesNewsCommentItemViewHolder){
            onBindViewCommentHolder(holder, position)
        }
    }

    private fun onBindViewBlogHolder(holder: CodeforcesNewsBlogItemViewHolder, position: Int){
        val info = rows[position]

        holder.view.setOnClickListener {
            if(info.commentators.isEmpty()){
                activity.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.blog(info.blogId)))
                return@setOnClickListener
            }

            PopupMenu(activity, holder.title, Gravity.CENTER_HORIZONTAL).apply {
                inflate(R.menu.cf_recent_item_open_variants)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    setForceShowIcon(true)
                }
                menu.findItem(R.id.cf_news_recent_item_menu_open_last_comment).let { item ->
                    item.title = SpannableStringBuilder(item.title)
                        .append(" [")
                        .append(info.commentators.first())
                        .append("]")
                }

                setOnMenuItemClickListener {
                    when(it.itemId){
                        R.id.cf_news_recent_item_menu_open_blog -> {
                            activity.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.blog(info.blogId)))
                            true
                        }
                        R.id.cf_news_recent_item_menu_open_last_comment -> {
                            activity.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.comment(info.blogId,info.lastCommentId)))
                            true
                        }
                        R.id.cf_news_recent_item_menu_show_comments -> {
                            showFromBlog(info)
                            true
                        }
                        else -> false
                    }
                }

                show()
            }
        }

        holder.title.text =  info.title

        holder.author.text = activity.accountsFragment.codeforcesAccountManager.makeSpan(info.author, info.authorColorTag)

        holder.comments.text = info.commentators.joinTo(SpannableStringBuilder())

        holder.commentsIcon.visibility = if(info.commentators.isEmpty()) View.INVISIBLE else View.VISIBLE
    }

    private fun onBindViewCommentHolder(holder: CodeforcesNewsCommentItemViewHolder, position: Int){
        val recentAction =
            headerBlog?.run { rowsComments.filter { it.blogEntry!!.id == blogId }[position] }
                ?: rowsComments[position]

        val blogEntry = recentAction.blogEntry!!
        val comment = recentAction.comment!!

        holder.title.text = blogEntry.title

        holder.author.text = activity.accountsFragment.codeforcesAccountManager.makeSpan(comment.commentatorHandle, comment.commentatorHandleColorTag)

        holder.rating.apply{
            if(comment.rating == 0) visibility = View.GONE
            else {
                visibility = View.VISIBLE
                text = signedToString(comment.rating)
                if (comment.rating > 0) {
                    setTextColor(getColorFromResource(activity, R.color.blog_rating_positive))
                } else {
                    setTextColor(getColorFromResource(activity, R.color.blog_rating_negative))
                }
            }
        }

        holder.comment.text = CodeforcesUtils.fromCodeforcesHTML(comment.text)

        holder.time.text = timeDifference(comment.creationTimeSeconds, getCurrentTimeSeconds())

        holder.view.setOnClickListener {
            activity.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.comment(blogEntry.id,comment.id)))
        }

        if(headerBlog!=null){
            holder.title.visibility = View.GONE
            holder.view.findViewById<TextView>(R.id.recent_arrow).visibility = View.GONE
        }else{
            holder.title.visibility = View.VISIBLE
            holder.view.findViewById<TextView>(R.id.recent_arrow).visibility = View.VISIBLE
        }
    }

    override fun getBlogIDs(): List<String> = rows.map { it.blogId.toString() }

}

class CodeforcesNewsItemsLostRecentAdapter : CodeforcesNewsItemsClassicAdapter() {
    override suspend fun parseData(s: String): Boolean {
        val blogs = getLostBlogsDao(activity).getLost()

        if(blogs.isNotEmpty()){
            val currentTimeSeconds = getCurrentTimeSeconds()
            rows = blogs
                .sortedByDescending { it.timeStamp }
                .map {
                    Info(
                        blogId = it.id,
                        title = it.title,
                        author = it.authorHandle,
                        authorColorTag = it.authorColorTag,
                        time = timeDifference(it.creationTimeSeconds, currentTimeSeconds),
                        comments = "",
                        rating = ""
                    )
                }.toTypedArray()
        }

        return true
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
