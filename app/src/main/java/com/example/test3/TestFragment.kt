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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.bold
import androidx.core.text.color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.asFlow
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import com.example.test3.account_manager.AtCoderAccountManager
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.HandleColor
import com.example.test3.account_manager.TopCoderAccountManager
import com.example.test3.ui.*
import com.example.test3.utils.CPSDataStore
import com.example.test3.workers.WorkersCenter
import kotlinx.coroutines.flow.distinctUntilChanged
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
        view.findViewById<RecyclerView>(R.id.workers).formatCPS().flowAdapter = WorkersInfoAdapter(this@TestFragment)

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

            val context = requireContext()
            for(handleColor in HandleColor.values()){
                val row = arrayListOf<CharSequence>()

                row.add(
                    SpannableStringBuilder().bold {
                        color(handleColor.getARGB(CodeforcesAccountManager(context), false)) { append(handleColor.name) }
                    }
                )

                arrayOf(
                    CodeforcesAccountManager(context),
                    AtCoderAccountManager(context),
                    TopCoderAccountManager(context),
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

    private class WorkersInfoAdapter(
        fragment: TestFragment
    ): FlowItemsAdapter<WorkersInfoAdapter.WorkerInfoViewHolder, List<WorkInfo>>(
        fragment,
        WorkersCenter.getWorksLiveData(fragment.requireContext()).asFlow()
    ) {

        private var items: Array<WorkInfo> = emptyArray()
        override fun getItemCount() = items.size

        private fun workerName(info: WorkInfo) = info.tags.find { it!=WorkersCenter.commonTag }!!
            .split(".")
            .last()
            .removeSuffix("Worker")

        class WorkerInfoViewHolder(view: View): RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.worker_name)
            val state: TextView = view.findViewById(R.id.worker_state)
        }

        override suspend fun applyData(data: List<WorkInfo>): DiffUtil.DiffResult? {
            items = data.sortedBy { workerName(it) }.toTypedArray()
            return null
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerInfoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.dev_worker_info, parent, false) as ConstraintLayout
            return WorkerInfoViewHolder(view)
        }

        override fun onBindViewHolder(holder: WorkerInfoViewHolder, position: Int) {
            val info = items[position]
            holder.name.text = workerName(info)
            holder.state.text = when(info.state){
                WorkInfo.State.ENQUEUED -> "\uD83D\uDE34"
                WorkInfo.State.RUNNING -> "\uD83E\uDD2A"
                WorkInfo.State.SUCCEEDED -> "\uD83D\uDE0F"
                WorkInfo.State.FAILED -> "\uD83D\uDE14"
                WorkInfo.State.BLOCKED -> "\uD83E\uDD7A"
                WorkInfo.State.CANCELLED -> "\uD83D\uDE10"
            }
        }


    }
}

val Context.settingsDev by CPSDataStoreDelegate { SettingsDev(it) }

class SettingsDev(context: Context) : CPSDataStore(context.settings_dev_dataStore) {
    companion object {
        private val Context.settings_dev_dataStore by preferencesDataStore("settings_develop")
    }

    private val KEY_DEV = booleanPreferencesKey("develop_enabled")

    private val devEnabledFlow = dataStore.data.map { it[KEY_DEV] ?: false }
    fun getDevEnabled() = runBlocking { devEnabledFlow.first() }
    fun flowOfDevEnabled() = devEnabledFlow.distinctUntilChanged()
    fun setDevEnabled(flag: Boolean) {
        runBlocking { dataStore.edit { it[KEY_DEV] = flag } }
    }
}