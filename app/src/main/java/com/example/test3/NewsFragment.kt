package com.example.test3

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.color
import androidx.fragment.app.Fragment
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.test3.job_services.CodeforcesNewsLostRecentJobService
import com.example.test3.job_services.JobServiceIDs
import com.example.test3.job_services.JobServicesCenter
import com.example.test3.utils.CodeforcesAPI
import com.example.test3.utils.CodeforcesAPIStatus
import com.example.test3.utils.fromHTML
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_cf_news_page.view.*
import kotlinx.android.synthetic.main.navigation_news.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit


class NewsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    private val codeforcesNewsAdapter: CodeforcesNewsAdapter by lazy {
        val activity = requireActivity() as MainActivity
        val fragments = mutableListOf(
            CodeforcesNewsFragment("MAIN", "/", true, CodeforcesNewsItemsClassicAdapter(activity)),
            CodeforcesNewsFragment("TOP", "/top", false, CodeforcesNewsItemsClassicAdapter(activity)),
            CodeforcesNewsFragment("RECENT", "/recent-actions", false, CodeforcesNewsItemsRecentAdapter(activity))
        )
        if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(getString(R.string.news_codeforces_lost_enabled), false)){
            fragments.add(CodeforcesNewsFragment("LOST", "", true, CodeforcesNewsItemsLostRecentAdapter(activity)))
        }
        CodeforcesNewsAdapter(this, fragments)
    }
    private lateinit var tabLayout: TabLayout
    private lateinit var codeforcesNewsViewPager: ViewPager2

    fun refresh(){
        try {
            codeforcesNewsAdapter.fragments.forEach { it.refresh() }
        }catch (e: UninitializedPropertyAccessException){

        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        codeforcesNewsViewPager = view.findViewById<ViewPager2>(R.id.cf_news_pager).apply {
            adapter = codeforcesNewsAdapter
            offscreenPageLimit = codeforcesNewsAdapter.fragments.size
        }

        tabLayout = view.findViewById(R.id.cf_news_tab_layout)
        TabLayoutMediator(tabLayout, codeforcesNewsViewPager) { tab, position ->
            val fragment = codeforcesNewsAdapter.fragments[position]
            tab.text = "CF ${fragment.title}"
            if(fragment.isManagesNewEntries){
                tab.orCreateBadge.apply {
                    backgroundColor = resources.getColor(android.R.color.holo_green_light, null)
                    isVisible = false
                }
            }
        }.attach()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
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

                    if(fragment.title == "LOST"){
                        updateLostInfoButton.visibility = View.GONE
                    }
                }
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.run{
                    val fragment = codeforcesNewsAdapter.fragments[position]
                    if(fragment.isManagesNewEntries) badge?.run{
                        if(hasNumber()){
                            fragment.save()
                        }
                    }

                    if(fragment.title == "LOST"){
                        updateLostInfoButton.visibility = View.VISIBLE
                    }

                    val subtitle = "::news.codeforces.${fragment.title.toLowerCase()}"
                    setFragmentSubTitle(this@NewsFragment, subtitle)
                    (requireActivity() as MainActivity).setActionBarSubTitle(subtitle)
                }
            }
        })

    }


    private val reloadButton by lazy { requireActivity().navigation_news_reload }
    fun reloadTabs() {
        reloadButton.isEnabled = false
        (requireActivity() as MainActivity).scope.launch {
            val lang = if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(getString(R.string.news_codeforces_ru), true)) "ru" else "en"
            val currentTime = System.currentTimeMillis()
            codeforcesNewsAdapter.fragments.mapIndexed { index, fragment ->
                val tab = tabLayout.getTabAt(index)!!
                launch {
                    if(tab.isSelected || currentTime - fragment.lastReloadTime > TimeUnit.MINUTES.toMillis(1)) {
                        tab.text = "..."
                        if(fragment.reload(lang)) {
                            tab.text = "CF ${fragment.title}"
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
                                    if (tab.isSelected) fragment.save()
                                }
                            }
                        }else{
                            tab.text = SpannableStringBuilder().apply {
                                color(resources.getColor(R.color.reload_fail,null)) {
                                    append(fragment.title)
                                }
                            }
                        }
                    }
                }
            }.joinAll()
            reloadButton.isEnabled = true
        }
    }


    private val updateLostInfoButton by lazy { requireActivity().navigation_news_lost_update_info }
    fun updateLostInfo() {
        val activity = requireActivity() as MainActivity

        val index = codeforcesNewsAdapter.fragments.indexOfFirst{ it.title == "LOST" }
        if(index == -1) return
        val tab = tabLayout.getTabAt(index) ?: return
        val fragment = codeforcesNewsAdapter.fragments[index]

        updateLostInfoButton.isEnabled = false
        tab.text = "..."

        activity.scope.launch {
            val blogEntries = CodeforcesNewsLostRecentJobService.getSavedBlogs(activity, CodeforcesNewsLostRecentJobService.CF_LOST)
                .toTypedArray()

            CodeforcesAPI.getUsers(blogEntries.map { it.authorHandle })?.result?.let { users ->
                for(i in blogEntries.indices) {
                    val blogEntry = blogEntries[i]
                    users.find { it.handle == blogEntry.authorHandle }?.let { user ->
                        blogEntries[i] = blogEntry.copy(
                            authorColorTag = activity.accountsFragment.codeforcesAccountManager.getTagByRating(user.rating)
                        )
                    }
                }
            }

            val blogIDsToRemove = mutableSetOf<Int>()
            blogEntries.forEachIndexed { index, blogEntry ->
                CodeforcesAPI.getBlogEntry(blogEntry.id)?.let { response ->
                    if(response.status == CodeforcesAPIStatus.FAILED && response.comment == "blogEntryId: Blog entry with id ${blogEntry.id} not found"){
                        blogIDsToRemove.add(blogEntry.id)
                    } else {
                        if(response.status == CodeforcesAPIStatus.OK) response.result?.let { freshBlogEntry ->
                            blogEntries[index] = blogEntry.copy(title = fromHTML(freshBlogEntry.title))
                        } else {
                            //god bless kotlin
                        }
                    }
                }
            }

            CodeforcesNewsLostRecentJobService.saveBlogs(
                activity,
                CodeforcesNewsLostRecentJobService.CF_LOST,
                blogEntries.filterNot { blogIDsToRemove.contains(it.id) }
            )
            fragment.reload("")

            tab.text = "CF LOST"
            updateLostInfoButton.isEnabled = true
        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val defaultTab =
            PreferenceManager.getDefaultSharedPreferences(context).getString(getString(R.string.news_codeforces_default_tab),null)
                ?: CodeforcesNewsAdapter.titles[1]

        var index = codeforcesNewsAdapter.fragments.indexOfFirst { it.title == defaultTab }
        if(index == -1) index = 1
        tabLayout.selectTab(tabLayout.getTabAt(index))
        codeforcesNewsViewPager.setCurrentItem(index, false)

        reloadTabs()
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

    companion object{
        val titles = arrayOf(
            "MAIN",
            "TOP",
            "RECENT",
            "LOST"
        )
    }
}

class CodeforcesNewsFragment(
    val title: String,
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.cf_news_page_recyclerview.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }
    }

    var lastReloadTime = 0L
    var newBlogs = hashSetOf<String>()

    suspend fun reload(lang: String): Boolean {
        val source =
            if(pageName.startsWith('/')) CodeforcesAPI.getPageSource(pageName.substring(1), lang) ?: return false
            else ""
        if(!viewAdapter.parseData(source)) return false
        lastReloadTime = System.currentTimeMillis()

        if(isManagesNewEntries){
            val savedBlogs = prefs.getStringSet(prefs_key, null) ?: emptySet()
            newBlogs = viewAdapter.getBlogIDs().filter { !savedBlogs.contains(it) }.toHashSet()
        }

        return true
    }

    fun save() {
        if(!isManagesNewEntries) return
        with(prefs.edit()) {
            val toSave = viewAdapter.getBlogIDs().toSet()
            putStringSet(prefs_key, toSave)
            apply()
        }
        (viewAdapter as? CodeforcesNewsItemsClassicAdapter)?.markNewEntries(newBlogs)
    }

    fun refresh(){
        viewAdapter.notifyDataSetChanged()
    }

    companion object {
        const val CODEFORCES_NEWS_VIEWED = "codeforces_news_viewed"
    }

    private val prefs: SharedPreferences by lazy { requireActivity().getSharedPreferences(CODEFORCES_NEWS_VIEWED, Context.MODE_PRIVATE) }
    private val prefs_key: String = this::class.java.simpleName + " " + title
}


///---------------data adapters--------------------

abstract class CodeforcesNewsItemsAdapter(val activity: MainActivity): RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    abstract fun parseData(s: String): Boolean
    abstract fun getBlogIDs(): List<String>
}

open class CodeforcesNewsItemsClassicAdapter(activity: MainActivity): CodeforcesNewsItemsAdapter(activity){

    data class Info(
        val blogID: String,
        val title: String,
        val author: String,
        val authorColorTag: String,
        val time: String,
        val comments: String,
        val rating: String,
        var isNew: Boolean = false
    )

    override fun parseData(s: String): Boolean {
        val res = arrayListOf<Info>()
        var i = 0
        while (true) {
            i = s.indexOf("<div class=\"topic\"", i + 1)
            if (i == -1) break

            val title = fromHTML(s.substring(s.indexOf("<p>", i) + 3, s.indexOf("</p>", i)))

            i = s.indexOf("entry/", i)
            val id = s.substring(i+6, s.indexOf('"',i))

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
            notifyDataSetChanged()
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
        val newDot: View = view.findViewById(R.id.news_item_dot_new)
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
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://codeforces.com/blog/entry/${info.blogID}")))
                if(info.isNew){
                    info.isNew = false
                    notifyItemChanged(position)
                }
            }

            title.text = info.title

            author.text = activity.accountsFragment.codeforcesAccountManager.makeSpan(info.author, info.authorColorTag)

            time.text = timeRUtoEN(info.time)

            newDot.visibility = if(info.isNew) View.VISIBLE else View.GONE

            comments.text = info.comments
            commentsIcon.visibility = if(info.comments.isEmpty()) View.INVISIBLE else View.VISIBLE

            rating.apply{
                text = info.rating
                setTextColor(activity.resources.getColor(
                    if(info.rating.startsWith('+')) R.color.blog_rating_positive
                    else R.color.blog_rating_negative, null)
                )
            }
        }
    }

    override fun getBlogIDs(): List<String> = rows.map { it.blogID }

    fun markNewEntries(newBlogs: HashSet<String>){
        rows.forEachIndexed { index, info ->
            val isNew = info.blogID in newBlogs
            if(rows[index].isNew != isNew){
                rows[index].isNew = isNew
                notifyItemChanged(index)
            }
        }
    }
}


class CodeforcesNewsItemsRecentAdapter(activity: MainActivity): CodeforcesNewsItemsAdapter(activity){

    companion object {
        fun parsePage(s: String): ArrayList<Info> {
            val commentators = mutableMapOf<String,MutableList<String>>()
            val commentatorsColors = mutableMapOf<String,String>()

            var i = 0
            while(true){
                i = s.indexOf("<table class=\"comment-table\">", i+1)
                if(i==-1) break

                i = s.indexOf("class=\"rated-user", i)
                val handleColor = s.substring(s.indexOf(' ',i)+1, s.indexOf('"',i+10))

                i = s.lastIndexOf("/profile/",i)
                val handle = s.substring(s.indexOf('/',i+1)+1, s.indexOf('"',i))

                i = s.indexOf("#comment-", i)
                val commentID = s.substring(s.indexOf('-',i)+1, s.indexOf('"',i))

                val blogID = s.substring(s.lastIndexOf('/',i)+1, i)

                commentators.getOrPut(blogID) { mutableListOf(commentID) }.add(handle)
                commentatorsColors[handle] = handleColor
            }

            val res = arrayListOf<Info>()
            i = s.indexOf("<div class=\"recent-actions\">")
            if(i==-1) return res

            while(true){
                i = s.indexOf("<div style=\"font-size:0.9em;padding:0.5em 0;\">", i+1)
                if(i==-1) break

                i = s.indexOf("/profile/", i)
                val author = s.substring(i+9,s.indexOf('"',i))

                i = s.indexOf("rated-user user-",i)
                val authorColor = s.substring(s.indexOf(' ',i)+1, s.indexOf('"',i))

                i = s.indexOf("entry/", i)
                val id = s.substring(i+6, s.indexOf('"',i))

                val title = fromHTML(s.substring(s.indexOf(">", i) + 1, s.indexOf("</a", i)))

                val comments = mutableListOf<Pair<String,String>>()
                var lastCommentId = ""

                commentators[id]?.distinct()?.mapIndexed{ index, handle ->
                    if(index==0) lastCommentId = handle
                    else comments.add(Pair(handle,commentatorsColors[handle]!!))
                }

                res.add(Info(id,title,author,authorColor,lastCommentId,comments.toTypedArray()))
            }

            return res
        }
    }

    data class Info(
        val blogID: String,
        val title: String,
        val author: String,
        val authorColorTag: String,
        val lastCommentId: String,
        val comments: Array<Pair<String,String>>
    )

    override fun parseData(s: String): Boolean {
        val res = parsePage(s)
        if(res.isNotEmpty()){
            rows = res.toTypedArray()
            notifyDataSetChanged()
            return true
        }
        return false
    }

    class CodeforcesNewsItemViewHolder(val view: RelativeLayout) : RecyclerView.ViewHolder(view){
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        val comments: TextView = view.findViewById(R.id.news_item_comments)
    }

    private var rows: Array<Info> = emptyArray()

    override fun getItemCount() = rows.size


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeforcesNewsItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_recent_item, parent, false) as RelativeLayout
        return CodeforcesNewsItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        with(holder as CodeforcesNewsItemViewHolder){
            val info = rows[position]

            view.setOnClickListener {
                var suf = info.blogID
                if(info.lastCommentId.isNotBlank()) suf+="#comment-${info.lastCommentId}"
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://codeforces.com/blog/entry/$suf")))
            }

            title.text =  info.title

            val codeforcesAccountManager = activity.accountsFragment.codeforcesAccountManager

            author.text = codeforcesAccountManager.makeSpan(info.author, info.authorColorTag)

            comments.text = SpannableStringBuilder().apply {
                var flag = false
                info.comments.forEach {(handle, colorTag) ->
                    if(flag) append(", ")
                    append(codeforcesAccountManager.makeSpan(handle,colorTag))
                    flag = true
                }
            }
        }
    }

    override fun getBlogIDs(): List<String> = rows.map { it.blogID }

}

class CodeforcesNewsItemsLostRecentAdapter(activity: MainActivity) : CodeforcesNewsItemsClassicAdapter(activity) {
    override fun parseData(s: String): Boolean {
        val blogs = CodeforcesNewsLostRecentJobService.getSavedBlogs(activity, CodeforcesNewsLostRecentJobService.CF_LOST)

        if(blogs.isNotEmpty()){
            val currentTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
            rows = blogs
                .sortedByDescending { it.creationTimeSeconds }
                .map {
                Info(
                    blogID = it.id.toString(),
                    title = it.title,
                    author = it.authorHandle,
                    authorColorTag = it.authorColorTag,
                    time = timeDifference(it.creationTimeSeconds, currentTimeSeconds),
                    comments = "",
                    rating = ""
                )
            }.toTypedArray()

            notifyDataSetChanged()
        }

        return true
    }
}

fun timeDifference(fromTimeSeconds: Long, toTimeSeconds: Long): String {
    val t = toTimeSeconds - fromTimeSeconds
    return when {
        t <= TimeUnit.MINUTES.toSeconds(2) -> "$t seconds"
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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.news_preferences)

        findPreference<DropDownPreference>(getString(R.string.news_codeforces_default_tab))?.run {
            entries = CodeforcesNewsAdapter.titles
            entryValues = CodeforcesNewsAdapter.titles
            setDefaultValue(CodeforcesNewsAdapter.titles[1])
        }

        PreferenceManager.getDefaultSharedPreferences(requireContext()).registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if(key == getString(R.string.news_codeforces_lost_enabled)){
            //spam possible
            when(sharedPreferences.getBoolean(key, false)){
                true -> JobServicesCenter.startCodeforcesNewsLostRecentJobService(requireContext())
                false -> JobServicesCenter.stopJobService(requireContext(), JobServiceIDs.codeforces_lost_recent_news)
            }
        }
    }

    override fun onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(requireContext()).unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }
}
