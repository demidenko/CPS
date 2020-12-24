package com.example.test3

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.*
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.color
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.test3.job_services.CodeforcesNewsFollowJobService
import com.example.test3.job_services.CodeforcesNewsLostRecentJobService
import com.example.test3.job_services.JobServiceIDs
import com.example.test3.job_services.JobServicesCenter
import com.example.test3.utils.*
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_cf_news_page.view.*
import kotlinx.android.synthetic.main.navigation_news.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.TimeUnit


enum class CodeforcesTitle {
    MAIN, TOP, RECENT, LOST
}

class NewsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    private val codeforcesNewsAdapter: CodeforcesNewsAdapter by lazy {
        val context = requireContext()
        val fragments = mutableListOf(
            CodeforcesNewsFragment(CodeforcesTitle.MAIN, "/", true, CodeforcesNewsItemsClassicAdapter()),
            CodeforcesNewsFragment(CodeforcesTitle.TOP, "/top", false, CodeforcesNewsItemsClassicAdapter()),
            CodeforcesNewsFragment(CodeforcesTitle.RECENT, "/recent-actions", false, CodeforcesNewsItemsRecentAdapter())
        )
        if(CodeforcesNewsLostRecentJobService.isEnabled(context)){
            fragments.add(CodeforcesNewsFragment(CodeforcesTitle.LOST, "", true, CodeforcesNewsItemsLostRecentAdapter()))
        }
        CodeforcesNewsAdapter(this, fragments)
    }
    private val tabLayout by lazy { requireView().findViewById<TabLayout>(R.id.cf_news_tab_layout) }
    private val tabSelectionListener = object : TabLayout.OnTabSelectedListener{

        fun changeVisibility(fragment: CodeforcesNewsFragment, type: Int){
            when(fragment.title){
                CodeforcesTitle.LOST -> updateLostInfoButton.visibility = type
                CodeforcesTitle.RECENT -> (fragment.viewAdapter as CodeforcesNewsItemsRecentAdapter).changeVisibility(type)
            }
        }

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

                changeVisibility(fragment, View.GONE)
            }
        }

        override fun onTabSelected(tab: TabLayout.Tab?) {
            tab?.run{
                val fragment = codeforcesNewsAdapter.fragments[position]
                if(fragment.isManagesNewEntries) badge?.run{
                    if(hasNumber()){
                        fragment.saveEntries()
                    }
                }

                changeVisibility(fragment, View.VISIBLE)

                val subtitle = "::news.codeforces.${fragment.title.name.toLowerCase(Locale.ENGLISH)}"
                setFragmentSubTitle(this@NewsFragment, subtitle)
                (requireActivity() as MainActivity).setActionBarSubTitle(subtitle)
            }
        }
    }

    private val codeforcesNewsViewPager by lazy {
        requireView().findViewById<ViewPager2>(R.id.cf_news_pager).apply {
            adapter = codeforcesNewsAdapter
            offscreenPageLimit = codeforcesNewsAdapter.fragments.size
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val badgeColor = getColorFromResource(requireContext(), android.R.color.holo_green_light)

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

        with(requireActivity() as MainActivity){
            navigation_news_reload.setOnClickListener { reloadTabs() }
            navigation_news_lost_update_info.setOnClickListener { updateLostInfo() }
            navigation_news_recent_swap.setOnClickListener {
                val fragment = codeforcesNewsAdapter.fragments.find { it.title == CodeforcesTitle.RECENT } ?: return@setOnClickListener
                (fragment.viewAdapter as CodeforcesNewsItemsRecentAdapter).switchMode()
            }
            navigation_news_recent_show_blog_back.setOnClickListener {
                val fragment = codeforcesNewsAdapter.fragments.find { it.title == CodeforcesTitle.RECENT } ?: return@setOnClickListener
                (fragment.viewAdapter as CodeforcesNewsItemsRecentAdapter).closeShowFromBlog()
            }

            settingsUI.userRealColorsLiveData.observeUpdates(viewLifecycleOwner){ use ->
                codeforcesNewsAdapter.fragments.forEach { it.refresh() }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            menu.setGroupDividerEnabled(true)
        }
        inflater.inflate(R.menu.menu_news, menu)
        menu.findItem(R.id.menu_news_follow_list).isVisible = CodeforcesNewsFollowJobService.isEnabled(requireContext())
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_news_settings_button -> {
                with(requireActivity() as MainActivity) {
                    supportFragmentManager.beginTransaction()
                        .hide(newsFragment)
                        .add(android.R.id.content, SettingsNewsFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }
            R.id.menu_news_follow_list -> manageCodeforcesFollowList()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getContentLanguage() = with(PreferenceManager.getDefaultSharedPreferences(context)){
        if(getBoolean(getString(R.string.news_codeforces_ru), true)) "ru" else "en"
    }

    private fun reloadTabs() {
        lifecycleScope.launch {
            val lang = getContentLanguage()
            codeforcesNewsAdapter.fragments.mapIndexed { index, fragment ->
                val tab = tabLayout.getTabAt(index)!!
                launch { reloadFragment(fragment, tab, lang) }
            }.joinAll()
        }
    }

    private val sharedReloadButton by lazy { SharedReloadButton(requireActivity().navigation_news_reload) }
    private val failColor by lazy { getColorFromResource(requireContext(), R.color.reload_fail) }
    private suspend fun reloadFragment(
        fragment: CodeforcesNewsFragment,
        tab: TabLayout.Tab,
        lang: String,
        block: suspend () -> Unit = {}
    ) {
        sharedReloadButton.startReload(fragment.title.name)
        tab.text = "..."
        fragment.swipeRefreshLayout.isRefreshing = true
        block()
        if(fragment.reload(lang)) {
            tab.text = fragment.title.name
            if (fragment.isManagesNewEntries) {
                if (fragment.newBlogs.isEmpty()) {
                    tab.badge?.apply {
                        isVisible = false
                        clearNumber()
                    }
                } else {
                    tab.badge?.apply {
                        number = fragment.newBlogs.size
                        isVisible = true
                    }
                    if (tab.isSelected) fragment.saveEntries()
                }
            }
        }else{
            tab.text = SpannableStringBuilder().color(failColor) { append(fragment.title.name) }
        }
        fragment.swipeRefreshLayout.isRefreshing = false
        sharedReloadButton.stopReload(fragment.title.name)
    }

    suspend fun reloadFragment(fragment: CodeforcesNewsFragment) {
        val index = codeforcesNewsAdapter.fragments.indexOf(fragment)
        val tab = tabLayout.getTabAt(index) ?: return
        reloadFragment(fragment, tab, getContentLanguage())
    }


    private val updateLostInfoButton by lazy { requireActivity().navigation_news_lost_update_info }

    private fun updateLostInfo() {
        val activity = requireActivity() as MainActivity

        val index = codeforcesNewsAdapter.fragments.indexOfFirst{ it.title == CodeforcesTitle.LOST }
        if(index == -1) return
        val tab = tabLayout.getTabAt(index) ?: return
        val fragment = codeforcesNewsAdapter.fragments[index]

        lifecycleScope.launch {
            reloadFragment(fragment, tab, ""){
                updateLostInfoButton.isEnabled = false
                CodeforcesNewsLostRecentJobService.updateInfo(activity)
                updateLostInfoButton.isEnabled = true
            }
        }

    }

    fun manageCodeforcesFollowList(){
        requireActivity().supportFragmentManager.beginTransaction()
            .hide(this)
            .add(android.R.id.content, ManageCodeforcesFollowListFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val index =
            with(codeforcesNewsAdapter.fragments){
                val defaultTab =
                    PreferenceManager.getDefaultSharedPreferences(context).getString(getString(R.string.news_codeforces_default_tab),null)
                        ?: CodeforcesTitle.TOP.name

                var pos = indexOfFirst { it.title.name == defaultTab }
                if(pos == -1)
                    pos = indexOfFirst { it.title == CodeforcesTitle.TOP }

                return@with pos
            }

        tabLayout.getTabAt(index)?.let{
            if(it.isSelected) tabSelectionListener.onTabSelected(it)
            else tabLayout.selectTab(tabLayout.getTabAt(index))
        }

        codeforcesNewsViewPager.setCurrentItem(index, false)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if(!hidden){
            with(requireActivity() as MainActivity){
                navigation.visibility = View.VISIBLE
            }
        }
        super.onHiddenChanged(hidden)
    }
}


class CodeforcesNewsAdapter(
    parentFragment: Fragment,
    val fragments: List<CodeforcesNewsFragment>
) : FragmentStateAdapter(parentFragment) {

    override fun createFragment(position: Int) = fragments[position]
    override fun getItemCount() = fragments.size
}

class CodeforcesNewsFragment(
    val title: CodeforcesTitle,
    val pageName: String,
    val isManagesNewEntries: Boolean,
    val viewAdapter: CodeforcesNewsItemsAdapter
): Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_cf_news_page, container, false)
    }

    val swipeRefreshLayout: SwipeRefreshLayout by lazy { requireView().cf_news_page_swipe_refresh_layout }

    private val newsFragment by lazy { (requireActivity() as MainActivity).newsFragment }
    private suspend fun callReload() = newsFragment.reloadFragment(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.cf_news_page_recyclerview.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }

        swipeRefreshLayout.apply {
            setOnRefreshListener {
                lifecycleScope.launch { callReload() }
            }
            setProgressBackgroundColorSchemeResource(R.color.textColor)
            setColorSchemeResources(R.color.colorAccent)
        }

        lifecycleScope.launchWhenCreated { callReload() }
    }

    var newBlogs = hashSetOf<String>()

    suspend fun reload(lang: String): Boolean {
        val source =
            if(pageName.startsWith('/')) CodeforcesAPI.getPageSource(pageName.substring(1), lang) ?: return false
            else ""
        if(!viewAdapter.parseData(source)) return false

        viewAdapter.notifyDataSetChanged()

        if(isManagesNewEntries){
            val savedBlogs = prefs.getStringSet(prefs_key, null) ?: emptySet()
            newBlogs = viewAdapter.getBlogIDs().filter { !savedBlogs.contains(it) }.toHashSet()
        }

        return true
    }

    fun saveEntries() {
        if(!isManagesNewEntries) return
        with(prefs.edit()) {
            val toSave = viewAdapter.getBlogIDs().toSet()
            putStringSet(prefs_key, toSave)
            apply()
        }
        (viewAdapter as? CodeforcesNewsItemsClassicAdapter)?.markNewEntries(newBlogs)
    }

    fun refresh(){
        viewAdapter.refresh()
    }

    companion object {
        const val CODEFORCES_NEWS_VIEWED = "codeforces_news_viewed"
    }

    private val prefs: SharedPreferences by lazy { requireActivity().getSharedPreferences(CODEFORCES_NEWS_VIEWED, Context.MODE_PRIVATE) }
    private val prefs_key: String = this::class.java.simpleName + " " + title
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
}

open class CodeforcesNewsItemsClassicAdapter: CodeforcesNewsItemsAdapter(){

    data class Info(
        val blogId: Int,
        val title: String,
        val author: String,
        val authorColorTag: String,
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
            val authorColor = s.substring(s.indexOf(' ',i)+1, s.indexOf('"',i))

            i = s.indexOf("<span class=\"format-humantime\"", i)
            val time = s.substring(s.indexOf('>',i)+1, s.indexOf("</span>",i))

            i = s.indexOf("<div class=\"roundbox meta\"", i)
            i = s.indexOf("</span>", i)
            val rating = s.substring(s.lastIndexOf('>',i-1)+1,i)

            i = s.indexOf("<div class=\"right-meta\">", i)
            i = s.indexOf("</ul>", i)
            i = s.lastIndexOf("</a>", i)
            val comments = s.substring(s.lastIndexOf('>',i-1)+1,i).trim()

            res.add(Info(id,title,author,authorColor,time,comments,rating))
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
            view.setOnLongClickListener{
                CodeforcesNewsFollowJobService.isEnabled(activity).apply {
                    if(this) addToFollowListWithSnackBar(activity, this@with)
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

    private fun addToFollowListWithSnackBar(mainActivity: MainActivity, holder: CodeforcesNewsItemViewHolder){
        mainActivity.newsFragment.lifecycleScope.launch {
            val connector = CodeforcesNewsFollowJobService.FollowDataConnector(mainActivity)
            val handle = holder.author.text
            when(connector.add(handle.toString())){
                true -> {
                    connector.save()
                    Snackbar.make(holder.view, SpannableStringBuilder("You now followed ").append(handle), Snackbar.LENGTH_LONG).apply {
                        setAction("Manage"){
                            mainActivity.newsFragment.manageCodeforcesFollowList()
                        }
                    }
                }
                false -> {
                    Snackbar.make(holder.view, SpannableStringBuilder("You already followed ").append(handle), Snackbar.LENGTH_LONG)
                }
            }.setAnchorView(mainActivity.navigation_main).show()
        }
    }
}


class CodeforcesNewsItemsRecentAdapter: CodeforcesNewsItemsAdapter(){

    class BlogInfo(
        val blogId: Int,
        val title: String,
        val author: String,
        val authorColorTag: String,
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
    private val switchButton by lazy { activity.navigation_news_recent_swap }
    private val showBackButton by lazy { activity.navigation_news_recent_show_blog_back }
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        with(recyclerView.parent.parent as ConstraintLayout){
            header = findViewById(R.id.cf_news_page_header)
        }
    }

    fun changeVisibility(type: Int){
        if(headerBlog == null){
            switchButton.visibility = type
        }else{
            showBackButton.visibility = type
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

        holder.time.text = timeDifference(comment.creationTimeSeconds, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()))

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
        val blogs = CodeforcesNewsLostRecentJobService.getSavedLostBlogs(activity)

        if(blogs.isNotEmpty()){
            val currentTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
            rows = blogs
                .sortedByDescending { it.creationTimeSeconds }
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


///--------------SETTINGS------------
class SettingsNewsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(requireActivity() as MainActivity){
            setActionBarSubTitle("::news.settings")
            navigation.visibility = View.GONE
        }
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
        super.onPrepareOptionsMenu(menu)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.news_preferences)

        findPreference<DropDownPreference>(getString(R.string.news_codeforces_default_tab))?.run {
            val titles = arrayOf(
                CodeforcesTitle.MAIN.name,
                CodeforcesTitle.TOP.name,
                CodeforcesTitle.RECENT.name
            )
            entries = titles
            entryValues = titles
            setDefaultValue(CodeforcesTitle.TOP.name)
        }

        PreferenceManager.getDefaultSharedPreferences(requireContext()).registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            getString(R.string.news_codeforces_lost_enabled) -> {
                //spam possible
                when(sharedPreferences.getBoolean(key, false)){
                    true -> JobServicesCenter.startCodeforcesNewsLostRecentJobService(requireContext())
                    false -> JobServicesCenter.stopJobService(requireContext(), JobServiceIDs.codeforces_news_lost_recent)
                }
            }
            getString(R.string.news_codeforces_follow_enabled) -> {
                //spam possible
                when(sharedPreferences.getBoolean(key, false)){
                    true -> JobServicesCenter.startCodeforcesNewsFollowJobService(requireContext())
                    false -> JobServicesCenter.stopJobService(requireContext(), JobServiceIDs.codeforces_news_follow)
                }
                requireActivity().invalidateOptionsMenu()
            }
            getString(R.string.news_project_euler_problems) -> {
                when(sharedPreferences.getBoolean(key, false)){
                    true -> JobServicesCenter.startProjectEulerRecentProblemsJobService(requireContext())
                    false -> JobServicesCenter.stopJobService(requireContext(), JobServiceIDs.project_euler_recent_problems)
                }
            }
            getString(R.string.news_project_euler_feed),
            getString(R.string.news_acmp_feed),
            getString(R.string.news_zaoch_feed) -> {
                if(sharedPreferences.getBoolean(key, false)) JobServicesCenter.startNewsJobService(requireContext())
            }
        }
    }

    override fun onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(requireContext()).unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }
}
