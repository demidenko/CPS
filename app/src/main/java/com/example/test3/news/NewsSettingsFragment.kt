package com.example.test3.news

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.example.test3.CodeforcesTitle
import com.example.test3.R
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.ui.*
import com.example.test3.utils.CPSDataStore
import com.example.test3.utils.CodeforcesUtils
import com.example.test3.workers.WorkersCenter
import com.example.test3.workers.WorkersNames
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class NewsSettingsFragment: CPSFragment(){

    private val newsFragment by lazy { mainActivity.newsFragment }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cpsTitle = "::news.settings"

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
                tabOptions.indexOf(mainActivity.settingsNews.getDefaultTab()),
            ){ buttonView, optionSelected ->
                lifecycleScope.launch {
                    buttonView.isEnabled = false
                    mainActivity.settingsNews.setDefaultTab(tabOptions[optionSelected])
                    buttonView.isEnabled = true
                }
            }

            val switchRuLang = view.findViewById<ConstraintLayout>(R.id.news_settings_ru_lang)
            setupSwitch(
                switchRuLang,
                "Russian content",
                mainActivity.settingsNews.russianContentEnabled()
            ){ buttonView, isChecked ->
                lifecycleScope.launch {
                    buttonView.isEnabled = false
                    mainActivity.settingsNews.russianContentEnabled(isChecked)
                    buttonView.isEnabled = true
                }
            }

            val isLostEnabled = mainActivity.settingsNews.lostEnabled()
            val switchLost = view.findViewById<ConstraintLayout>(R.id.news_settings_lost)
            val selectLostRating = view.findViewById<ConstraintLayout>(R.id.news_settings_lost_min_rating).apply {
                isVisible = isLostEnabled
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
                    mainActivity.settingsNews.lostEnabled(isChecked)
                    if (isChecked) {
                        WorkersCenter.startCodeforcesNewsLostRecentWorker(mainActivity)
                        newsFragment.addLostTab()
                        selectLostRating.isVisible = true
                    } else {
                        newsFragment.removeLostTab()
                        WorkersCenter.stopWorker(mainActivity, WorkersNames.codeforces_news_lost_recent)
                        selectLostRating.isVisible = false
                    }
                    buttonView.isEnabled = true
                }
            }

            val ratingOptions = CodeforcesUtils.ColorTag.values().filter { it != CodeforcesUtils.ColorTag.ADMIN }

            val codeforcesAccountManager = CodeforcesAccountManager(requireContext())
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
                    codeforcesAccountManager.makeSpan(it, tag)
                }
            }

            setupSelect(
                selectLostRating,
                "Author at least",
                ratingNames.toTypedArray(),
                ratingOptions.indexOf(mainActivity.settingsNews.getLostMinRating())
            ){ buttonView, optionSelected ->
                lifecycleScope.launch {
                    buttonView.isEnabled = false
                    mainActivity.settingsNews.setLostMinRating(ratingOptions[optionSelected])
                    buttonView.isEnabled = true
                }
            }

            val switchFollow = view.findViewById<ConstraintLayout>(R.id.news_settings_follow)
            setupSwitch(
                switchFollow,
                "Follow",
                mainActivity.settingsNews.followEnabled(),
                getString(R.string.news_settings_cf_follow_description)
            ){ buttonView, isChecked ->
                lifecycleScope.launch {
                    buttonView.isEnabled = false
                    mainActivity.settingsNews.followEnabled(isChecked)
                    if (isChecked) {
                        WorkersCenter.startCodeforcesNewsFollowWorker(mainActivity)
                    } else {
                        WorkersCenter.stopWorker(mainActivity, WorkersNames.codeforces_news_follow)
                    }
                    mainActivity.invalidateOptionsMenu()
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

            val newsFeedsEnabled = with(mainActivity.settingsNews){
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
                    with(mainActivity.settingsNews){
                        NewsFeed.values().forEachIndexed { index, newsFeed ->
                            val current = optionsSelected[index]
                            if(newsFeedsEnabled[index] != current){
                                setNewsFeedEnabled(newsFeed, current)
                                newsFeedsEnabled[index] = current
                                changed.add(Pair(newsFeed,current))
                            }
                        }
                    }

                    changed.find { it.first == NewsFeed.PROJECT_EULER_RECENT }
                        ?.let { (_, enabled) ->
                            if(enabled) WorkersCenter.startProjectEulerRecentProblemsWorker(mainActivity)
                            else WorkersCenter.stopWorker(mainActivity, WorkersNames.project_euler_recent_problems)
                        }

                    changed.find { (newsFeed, enabled) ->
                        enabled &&
                        newsFeed in listOf(NewsFeed.PROJECT_EULER_NEWS, NewsFeed.ACMP_NEWS, NewsFeed.ZAOCH_NEWS)
                    } ?.run { WorkersCenter.startNewsWorker(mainActivity) }

                    buttonView.isEnabled = true
                }
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
        super.onPrepareOptionsMenu(menu)
    }

}

val Context.settingsNews by CPSDataStoreDelegate { NewsSettingsDataStore(it) }

class NewsSettingsDataStore(context: Context): CPSDataStore(context.news_settings_dataStore){

    companion object {
        private val Context.news_settings_dataStore by preferencesDataStore("news_settings")
    }

    private val KEY_TAB = stringPreferencesKey("default_tab")
    val russianContentEnabled = Item(booleanPreferencesKey("ru_lang"), true)
    val lostEnabled = Item(booleanPreferencesKey("lost"), false)
    private val KEY_LOST_RATING = stringPreferencesKey("lost_min_rating")
    val followEnabled = Item(booleanPreferencesKey("follow"), false)

    private val itemsByFeed = mapOf(
        NewsFeed.PROJECT_EULER_NEWS to Item(booleanPreferencesKey("news_feeds_project_euler_news"), false),
        NewsFeed.PROJECT_EULER_RECENT to Item(booleanPreferencesKey("news_feeds_project_euler_recent"), false),
        NewsFeed.ACMP_NEWS to Item(booleanPreferencesKey("news_feeds_acmp_news"), false),
        NewsFeed.ZAOCH_NEWS to Item(booleanPreferencesKey("news_feeds_zaoch_news"), false)
    )

    suspend fun getNewsFeedEnabled(newsFeed: NewsFeed) = itemsByFeed[newsFeed]!!()
    suspend fun setNewsFeedEnabled(newsFeed: NewsFeed, flag: Boolean) = itemsByFeed[newsFeed]!!(flag)

    suspend fun getDefaultTab(): CodeforcesTitle {
        return dataStore.data.first()[KEY_TAB]?.let {
            CodeforcesTitle.valueOf(it)
        } ?: CodeforcesTitle.TOP
    }
    suspend fun setDefaultTab(title: CodeforcesTitle) {
        dataStore.edit { it[KEY_TAB] = title.name }
    }

    suspend fun getLostMinRating(): CodeforcesUtils.ColorTag {
        return dataStore.data.first()[KEY_LOST_RATING]?.let {
            CodeforcesUtils.ColorTag.valueOf(it)
        } ?: CodeforcesUtils.ColorTag.ORANGE
    }
    suspend fun setLostMinRating(tag: CodeforcesUtils.ColorTag) {
        dataStore.edit { it[KEY_LOST_RATING] = tag.name }
    }

}

enum class NewsFeed {
    PROJECT_EULER_RECENT,
    PROJECT_EULER_NEWS,
    ACMP_NEWS,
    ZAOCH_NEWS
}
