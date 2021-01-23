package com.example.test3.news

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.test3.CodeforcesTitle
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.job_services.JobServiceIDs
import com.example.test3.job_services.JobServicesCenter
import com.example.test3.utils.setupSelect
import com.example.test3.utils.setupSwitch
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class SettingsNewsFragment: Fragment(){

    private val newsFragment by lazy { (requireActivity() as MainActivity).newsFragment }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(requireActivity() as MainActivity){
            setActionBarSubTitle("::news.settings")
            navigation.visibility = View.GONE
        }
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        runBlocking {

            val tabOptions = listOf(
                CodeforcesTitle.MAIN,
                CodeforcesTitle.TOP,
                //CodeforcesTitle.RECENT TODO: crash on init
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

            val switchLost = view.findViewById<ConstraintLayout>(R.id.news_settings_lost)
            setupSwitch(
                switchLost,
                "Lost recent blogs",
                getSettings(requireContext()).getLostEnabled(),
                "TODO"
            ){ buttonView, isChecked ->
                lifecycleScope.launch {
                    buttonView.isEnabled = false
                    val context = requireContext()
                    getSettings(context).setLostEnabled(isChecked)
                    if (isChecked) {
                        JobServicesCenter.startCodeforcesNewsLostRecentJobService(context)
                        newsFragment.addLostTab()
                    } else {
                        newsFragment.removeLostTab()
                        JobServicesCenter.stopJobService(context, JobServiceIDs.codeforces_news_lost_recent)
                    }
                    buttonView.isEnabled = true
                }
            }

            val switchFollow = view.findViewById<ConstraintLayout>(R.id.news_settings_follow)
            setupSwitch(
                switchFollow,
                "Follow",
                getSettings(requireContext()).getFollowEnabled(),
                "TODO"
            ){ buttonView, isChecked ->
                lifecycleScope.launch {
                    buttonView.isEnabled = false
                    val activity = requireActivity()
                    getSettings(activity).setFollowEnabled(isChecked)
                    if (isChecked) {
                        JobServicesCenter.startCodeforcesNewsFollowJobService(activity)
                    } else {
                        JobServicesCenter.stopJobService(activity, JobServiceIDs.codeforces_news_follow)
                    }
                    activity.invalidateOptionsMenu()
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

    class NewsSettingsDataStore(context: Context){
        private val dataStore by lazy { context.createDataStore(name = "news_settings") }

        companion object {
            private val KEY_TAB = preferencesKey<String>("default_tab")
            private val KEY_RU = preferencesKey<Boolean>("ru_lang")
            private val KEY_LOST = preferencesKey<Boolean>("lost")
            private val KEY_FOLLOW = preferencesKey<Boolean>("follow")
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

        suspend fun getFollowEnabled() = dataStore.data.first()[KEY_FOLLOW] ?: false
        suspend fun setFollowEnabled(flag: Boolean){
            dataStore.edit { it[KEY_FOLLOW] = flag }
        }
    }
}