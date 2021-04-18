package com.example.test3

import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.bold
import androidx.core.text.color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.test3.account_manager.HandleColor
import com.example.test3.account_manager.RatedAccountManager
import com.example.test3.ui.CPSDataStoreDelegate
import com.example.test3.ui.CPSFragment
import com.example.test3.utils.CPSDataStore
import com.example.test3.workers.WorkersCenter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.*


class TestFragment : CPSFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)

        cpsTitle = "::develop"
        setBottomPanelId(R.id.support_navigation_develop, R.layout.navigation_dev)

        //show running jobs
        val workersTextView = view.findViewById<TextView>(R.id.workers)
        fun concat(a: String, b: String) = a + ".".repeat(42 - a.length - b.length) + b
        WorkersCenter.getWorksLiveData(mainActivity).observe(mainActivity){ infos ->
            val rows = infos.map { info ->
                val str = info.tags.find { it!=WorkersCenter.commonTag }!!
                    .split(".")
                    .last()
                    .removeSuffix("Worker")
                str to info.state
            }
            workersTextView.text =
                rows.sortedBy { it.first }.joinToString(separator = "\n"){ (str, state) -> concat(str, state.name) }
        }

        val stuff = view.findViewById<TextView>(R.id.stuff_textview)

        //colors
        requireBottomPanel().findViewById<ImageButton>(R.id.navigation_dev_colors).setOnClickListener{
            val table = view.findViewById<LinearLayout>(R.id.table_handle_colors)
            table.removeAllViews()

            fun addRow(row: ArrayList<CharSequence>) {
                val l = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                row.forEach { s->
                    l.addView(
                        TextView(context).apply{ text = s },
                        LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT).apply { weight = 1f }
                    )
                }
                table.addView(l)
            }

            addRow(arrayListOf(
                "app",
                "Codeforces",
                "AtCoder",
                "Topcoder"
            ))

            for(handleColor in HandleColor.values()){
                val row = arrayListOf<CharSequence>()

                row.add(
                    SpannableStringBuilder().bold {
                        color(handleColor.getARGB(mainActivity.accountsFragment.codeforcesAccountManager, false)) { append(handleColor.name) }
                    }
                )

                arrayOf<RatedAccountManager>(
                    mainActivity.accountsFragment.codeforcesAccountManager,
                    mainActivity.accountsFragment.atcoderAccountManager,
                    mainActivity.accountsFragment.topcoderAccountManager,
                ).forEach { manager ->
                    val s = SpannableStringBuilder().bold {
                        try {
                            color(handleColor.getARGB(manager, true)) { append(handleColor.name) }
                        } catch (e: HandleColor.UnknownHandleColorException) {
                            append("")
                        }
                    }
                    row.add(s)
                }
                addRow(row)
            }
        }

    }
}

val Context.settingsDev by CPSDataStoreDelegate { SettingsDev(it) }

class SettingsDev(context: Context) : CPSDataStore(context.settings_dev_dataStore) {
    companion object {
        private val Context.settings_dev_dataStore by preferencesDataStore("settings_develop")

        private val KEY_DEV = booleanPreferencesKey("develop_enabled")
    }

    private val devEnabledFlow = dataStore.data.map { it[KEY_DEV] ?: false }
    fun getDevEnabled() = runBlocking { devEnabledFlow.first() }
    fun getDevEnabledFlow() = devEnabledFlow
    fun setDevEnabled(flag: Boolean) {
        runBlocking { dataStore.edit { it[KEY_DEV] = flag } }
    }
}