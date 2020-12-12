package com.example.test3

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.text.bold
import androidx.core.text.color
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.test3.account_manager.HandleColor
import com.example.test3.account_manager.RatedAccountManager
import com.example.test3.account_manager.useRealColors
import com.example.test3.contest_watch.CodeforcesContestWatchService
import com.example.test3.job_services.JobServicesCenter
import com.example.test3.utils.CodeforcesAPI
import com.example.test3.utils.CodeforcesAPIStatus
import com.example.test3.utils.CodeforcesContest
import kotlinx.coroutines.launch
import java.util.*


class TestFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = requireActivity() as MainActivity


        val stuff = view.findViewById<TextView>(R.id.stuff_textview)
        val handleEditText = view.findViewById<EditText>(R.id.dev_text_editor_handle)
        val contestIDEditText = view.findViewById<EditText>(R.id.dev_text_editor_contest_id)
        val prefs = mainActivity.getSharedPreferences("test", Context.MODE_PRIVATE)
        contestIDEditText.setText(prefs.getInt("contest_id", 0).toString())
        handleEditText.setText(prefs.getString("handle", ""))


        //monitor beta
        view.findViewById<Button>(R.id.button_watcher).setOnClickListener { button -> button as Button

            val handle = handleEditText.text.toString()
            val contestID = contestIDEditText.text.toString().toInt()

            lifecycleScope.launch {
                CodeforcesAPI.getUser(handle)?.let { userInfo ->
                    if(userInfo.status == CodeforcesAPIStatus.OK){
                        val intent = Intent(mainActivity, CodeforcesContestWatchService::class.java)
                            .setAction(CodeforcesContestWatchService.ACTION_START)
                            .putExtra("handle", userInfo.result!!.handle)
                            .putExtra("contestID", contestID)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            mainActivity.startForegroundService(intent)
                        } else {
                            mainActivity.startService(intent)
                        }
                    }else{
                        Toast.makeText(mainActivity, userInfo.comment, Toast.LENGTH_LONG).show()
                    }
                }
            }


            with(prefs.edit()){
                putInt("contest_id", contestID)
                putString("handle", handle)
                apply()
            }
        }

        view.findViewById<Button>(R.id.button_watcher_stop).setOnClickListener { button -> button as Button
            mainActivity.startService(CodeforcesContestWatchService.makeStopIntent(mainActivity))
        }

        view.findViewById<Button>(R.id.dev_choose_contest).setOnClickListener { button -> button as Button
            button.isEnabled = false

            lifecycleScope.launch {
                val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.select_dialog_item)
                val contests = arrayListOf<CodeforcesContest>()
                CodeforcesAPI.getContests()?.result?.forEach {
                    if(it.phase.isFutureOrRunning()){
                        adapter.add(it.name)
                        contests.add(it)
                    }
                }

                AlertDialog.Builder(mainActivity)
                    .setTitle("Running or Future Contests")
                    .setAdapter(adapter) { _, index ->
                        contestIDEditText.setText(contests[index].id.toString())
                    }.create().show()

                button.isEnabled = true
            }
        }

        view.findViewById<Button>(R.id.dev_choose_handle).setOnClickListener { button -> button as Button
            button.isEnabled = false

            lifecycleScope.launch {
                mainActivity.chooseUserID(mainActivity.accountsFragment.codeforcesAccountManager)?.let {
                    handleEditText.setText(it.userID)
                }
                button.isEnabled = true
            }
        }

        //show running jobs
        view.findViewById<Button>(R.id.button_running_jobs).setOnClickListener {
            stuff.text = JobServicesCenter.getRunningJobServices(mainActivity).joinToString(separator = "\n"){ info ->
                "Job " + info.id + ": " + info.service.shortClassName.removeSuffix("JobService").removePrefix(".job_services.")
            }
        }

        //colors
        view.findViewById<Button>(R.id.button_test_handle_colors).setOnClickListener{ button -> button as Button
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

            val backup = useRealColors
            for(handleColor in HandleColor.values()){
                val row = arrayListOf<CharSequence>()

                useRealColors = false

                row.add(
                    SpannableStringBuilder().bold {
                        color(handleColor.getARGB(mainActivity.accountsFragment.codeforcesAccountManager)) { append(handleColor.name) }
                    }
                )
                useRealColors = true
                arrayOf<RatedAccountManager>(
                    mainActivity.accountsFragment.codeforcesAccountManager,
                    mainActivity.accountsFragment.atcoderAccountManager,
                    mainActivity.accountsFragment.topcoderAccountManager,
                ).forEach {
                    val s = SpannableStringBuilder().bold {
                        try {
                            color(handleColor.getARGB(it)) { append(handleColor.name) }
                        } catch (e: HandleColor.UnknownHandleColorException) {
                            append("")
                        }
                    }
                    row.add(s)
                }
                addRow(row)
            }

            useRealColors = backup
        }

    }
}
