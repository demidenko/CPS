package com.example.test3.news

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import com.example.test3.CPSFragment
import com.example.test3.CodeforcesTitle
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.utils.*
import com.example.test3.workers.WorkersCenter
import com.example.test3.workers.WorkersNames
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class SettingsNewsFragment: CPSFragment(){

    private val mainActivity by lazy { requireActivity() as MainActivity }
    private val newsFragment by lazy { mainActivity.newsFragment }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //TODO bad restore
        super.onViewCreated(view, savedInstanceState)

        setCPSTitle("::news.settings")

        setHasOptionsMenu(true)

        runBlocking {

            val tabOptions = listOf(
                CodeforcesTitle.MAIN,
                CodeforcesTitle.TOP,
                CodeforcesTitle.RECENT
            )

            val selectDefaultTab = view.findViewById<ConstraintLayout>(R.id.news_settings_default_tab)
            setupSelect(
                selectDefaultTab,
                "Default tab",
                tabOptions.map { it.name }.toTypedArray(),
                tabOptions.indexOf(getSettings(requireContext()).getDefaultTab()),
            ){ buttonView, optionSelected ->
                lifecycleScope.launch {
                    buttonView.isEnabled = false
                    getSettings(requireContext()).setDefaultTab(tabOptions[optionSelected])
                    buttonView.isEnabled = true
                }
            }

            val switchRuLang = view.findViewById<ConstraintLayout>(R.id.news_settings_ru_lang)
            setupSwitch(
                switchRuLang,
                "Russian content",
                getSettings(requireContext()).getRussianContentEnabled()
            ){ buttonView, isChecked ->
                lifecycleScope.launch {
                    buttonView.isEnabled = false
                    getSettings(requireContext()).setRussianContentEnabled(isChecked)
                    buttonView.isEnabled = true
                }
            }

            val isLostEnabled = getSettings(requireContext()).getLostEnabled()
            val switchLost = view.findViewById<ConstraintLayout>(R.id.news_settings_lost)
            val selectLostRating = view.findViewById<ConstraintLayout>(R.id.news_settings_lost_min_rating).apply {
                visibility = if(isLostEnabled) View.VISIBLE else View.GONE
                findViewById<TextView>(R.id.settings_select_button).setTextColor(mainActivity.defaultTextColor)
            }
            setupSwitch(
                switchLost,
                "Lost recent blogs",
                isLostEnabled,
                getString(R.string.news_settings_cf_lost_description)
            ){ buttonView, isChecked ->
                lifecycleScope.launch {
                    buttonView.isEnabled = false
                    val context = requireContext()
                    getSettings(context).setLostEnabled(isChecked)
                    if (isChecked) {
                        WorkersCenter.startCodeforcesNewsLostRecentWorker(context)
                        newsFragment.addLostTab()
                        selectLostRating.visibility = View.VISIBLE
                    } else {
                        newsFragment.removeLostTab()
                        WorkersCenter.stopWorker(context, WorkersNames.codeforces_news_lost_recent)
                        selectLostRating.visibility = View.GONE
                    }
                    buttonView.isEnabled = true
                }
            }

            val ratingOptions = CodeforcesUtils.ColorTag.values().filter { it != CodeforcesUtils.ColorTag.ADMIN }

            val ratingNames = ratingOptions.map { tag ->
                when(tag){
                    CodeforcesUtils.ColorTag.BLACK -> "Exists"
                    CodeforcesUtils.ColorTag.GRAY -> "Newbie"
                    CodeforcesUtils.ColorTag.GREEN -> "Pupil"
                    CodeforcesUtils.ColorTag.CYAN -> "Specialist"
                    CodeforcesUtils.ColorTag.BLUE -> "Expert"
                    CodeforcesUtils.ColorTag.VIOLET -> "Candidate Master"
                    CodeforcesUtils.ColorTag.ORANGE -> "Master"
                    CodeforcesUtils.ColorTag.RED -> "Grandmaster"
                    CodeforcesUtils.ColorTag.LEGENDARY -> "LGM"
                    else -> ""
                }.let {
                    mainActivity.accountsFragment.codeforcesAccountManager.makeSpan(it, tag)
                }
            }

            setupSelect(
                selectLostRating,
                "Author at least",
                ratingNames.toTypedArray(),
                ratingOptions.indexOf(getSettings(requireContext()).getLostMinRating())
            ){ buttonView, optionSelected ->
                lifecycleScope.launch {
                    buttonView.isEnabled = false
                    getSettings(requireContext()).setLostMinRating(ratingOptions[optionSelected])
                    buttonView.isEnabled = true
                }
            }

            val switchFollow = view.findViewById<ConstraintLayout>(R.id.news_settings_follow)
            setupSwitch(
                switchFollow,
                "Follow",
                getSettings(requireContext()).getFollowEnabled(),
                getString(R.string.news_settings_cf_follow_description)
            ){ buttonView, isChecked ->
                lifecycleScope.launch {
                    buttonView.isEnabled = false
                    val activity = requireActivity()
                    getSettings(activity).setFollowEnabled(isChecked)
                    if (isChecked) {
                        WorkersCenter.startCodeforcesNewsFollowWorker(activity)
                    } else {
                        WorkersCenter.stopWorker(activity, WorkersNames.codeforces_news_follow)
                    }
                    activity.invalidateOptionsMenu()
                    buttonView.isEnabled = true
                }
            }

            val multiSelectNewsFeeds = view.findViewById<ConstraintLayout>(R.id.news_settings_news_feeds)

            val newsFeedsNames = NewsFeed.values().map { newsFeed ->
                when(newsFeed){
                    NewsFeed.PROJECT_EULER_RECENT -> "projecteuler.net/recent"
                    NewsFeed.PROJECT_EULER_NEWS -> "projecteuler.net"
                    NewsFeed.ACMP_NEWS -> "acmp.ru"
                    NewsFeed.ZAOCH_NEWS -> "olympiads.ru/zaoch"
                }
            }

            val newsFeedsEnabled = with(getSettings(requireContext())){
                NewsFeed.values().map { newsFeed -> getNewsFeedEnabled(newsFeed) }
                    .toBooleanArray()
            }

            setupMultiSelect(
                multiSelectNewsFeeds,
                "Subscribes",
                newsFeedsNames.toTypedArray(),
                newsFeedsEnabled
            ){ buttonView, optionsSelected ->
                lifecycleScope.launch {
                    buttonView.isEnabled = false
                    val changed = mutableListOf<Pair<NewsFeed,Boolean>>()
                    with(getSettings(requireContext())){
                        NewsFeed.values().mapIndexed { index, newsFeed ->
                            val current = optionsSelected[index]
                            if(newsFeedsEnabled[index] != current){
                                setNewsFeedEnabled(newsFeed, current)
                                newsFeedsEnabled[index] = current
                                changed.add(Pair(newsFeed,current))
                            }
                        }
                    }
                    changed.forEach { (newsFeed, enabled) ->
                        when(newsFeed){
                            NewsFeed.PROJECT_EULER_RECENT -> {
                                if(enabled) WorkersCenter.startProjectEulerRecentProblemsWorker(requireContext())
                                else WorkersCenter.stopWorker(requireContext(), WorkersNames.project_euler_recent_problems)
                            }
                            NewsFeed.PROJECT_EULER_NEWS,
                            NewsFeed.ACMP_NEWS,
                            NewsFeed.ZAOCH_NEWS -> {
                                if(enabled) WorkersCenter.startNewsWorker(requireContext())
                            }
                        }
                    }
                    buttonView.isEnabled = true
                }
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
        super.onPrepareOptionsMenu(menu)
    }

    companion object {
        fun getSettings(context: Context) = NewsSettingsDataStore(context)
    }

    enum class NewsFeed {
        PROJECT_EULER_RECENT,
        PROJECT_EULER_NEWS,
        ACMP_NEWS,
        ZAOCH_NEWS
    }

    class NewsSettingsDataStore(context: Context): SettingsDataStore(context, "news_settings"){

        companion object {
            private val KEY_TAB = stringPreferencesKey("default_tab")
            private val KEY_RU = booleanPreferencesKey("ru_lang")
            private val KEY_LOST = booleanPreferencesKey("lost")
            private val KEY_LOST_RATING = stringPreferencesKey("lost_min_rating")
            private val KEY_FOLLOW = booleanPreferencesKey("follow")

            private val KEY_FEED_PE = booleanPreferencesKey("news_feeds_project_euler_news")
            private val KEY_FEED_PE_RECENT = booleanPreferencesKey("news_feeds_project_euler_recent")
            private val KEY_FEED_ACMP = booleanPreferencesKey("news_feeds_acmp_news")
            private val KEY_FEED_ZAOCH = booleanPreferencesKey("news_feeds_zaoch_news")
        }

        suspend fun getDefaultTab(): CodeforcesTitle {
            return dataStore.data.first()[KEY_TAB]?.let {
                CodeforcesTitle.valueOf(it)
            } ?: CodeforcesTitle.TOP
        }
        suspend fun setDefaultTab(title: CodeforcesTitle) {
            dataStore.edit { it[KEY_TAB] = title.name }
        }

        suspend fun getRussianContentEnabled() = dataStore.data.first()[KEY_RU] ?: true
        suspend fun setRussianContentEnabled(flag: Boolean){
            dataStore.edit { it[KEY_RU] = flag }
        }

        suspend fun getLostEnabled() = dataStore.data.first()[KEY_LOST] ?: false
        suspend fun setLostEnabled(flag: Boolean){
            dataStore.edit { it[KEY_LOST] = flag }
        }

        suspend fun getLostMinRating(): CodeforcesUtils.ColorTag {
            return dataStore.data.first()[KEY_LOST_RATING]?.let {
                CodeforcesUtils.ColorTag.valueOf(it)
            } ?: CodeforcesUtils.ColorTag.ORANGE
        }
        suspend fun setLostMinRating(tag: CodeforcesUtils.ColorTag) {
            dataStore.edit { it[KEY_LOST_RATING] = tag.name }
        }

        suspend fun getFollowEnabled() = dataStore.data.first()[KEY_FOLLOW] ?: false
        suspend fun setFollowEnabled(flag: Boolean){
            dataStore.edit { it[KEY_FOLLOW] = flag }
        }

        private fun getKey(newsFeed: NewsFeed): Preferences.Key<Boolean> {
            return when(newsFeed){
                NewsFeed.PROJECT_EULER_RECENT -> KEY_FEED_PE_RECENT
                NewsFeed.PROJECT_EULER_NEWS -> KEY_FEED_PE
                NewsFeed.ACMP_NEWS -> KEY_FEED_ACMP
                NewsFeed.ZAOCH_NEWS -> KEY_FEED_ZAOCH
            }
        }

        suspend fun getNewsFeedEnabled(newsFeed: NewsFeed) = dataStore.data.first()[getKey(newsFeed)] ?: false
        suspend fun setNewsFeedEnabled(newsFeed: NewsFeed, flag: Boolean){
            dataStore.edit { it[getKey(newsFeed)] = flag }
        }

    }
}