package com.example.test3

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.test3.contest_watch.CodeforcesContestWatchService
import com.example.test3.job_services.JobServicesCenter
import com.example.test3.utils.CodeforcesAPI
import com.example.test3.utils.CodeforcesContest
import kotlinx.coroutines.launch


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
        val activity = requireActivity() as MainActivity


        val stuff = view.findViewById<TextView>(R.id.stuff_textview)
        val handleEditText = view.findViewById<EditText>(R.id.dev_text_editor_handle)
        val contestIDEditText = view.findViewById<EditText>(R.id.dev_text_editor_contest_id)
        val prefs = activity.getSharedPreferences("test", Context.MODE_PRIVATE)
        contestIDEditText.setText(prefs.getInt("contest_id", 0).toString())
        handleEditText.setText(prefs.getString("handle", ""))


        //monitor beta
        view.findViewById<Button>(R.id.button_watcher).setOnClickListener { button -> button as Button

            val handle = handleEditText.text.toString()
            val contestID = contestIDEditText.text.toString().toInt()
            activity.startForegroundService(
                Intent(activity, CodeforcesContestWatchService::class.java)
                    .setAction(CodeforcesContestWatchService.ACTION_START)
                    .putExtra("handle", handle)
                    .putExtra("contestID", contestID)
            )

            with(prefs.edit()){
                putInt("contest_id", contestID)
                putString("handle", handle)
                apply()
            }
        }

        view.findViewById<Button>(R.id.button_watcher_stop).setOnClickListener { button -> button as Button
            activity.startService(
                Intent(activity, CodeforcesContestWatchService::class.java)
                    .setAction(CodeforcesContestWatchService.ACTION_STOP)
            )
        }

        view.findViewById<Button>(R.id.dev_choose_contest).setOnClickListener { button -> button as Button
            button.isEnabled = false

            activity.scope.launch {
                val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.select_dialog_item)
                val contests = arrayListOf<CodeforcesContest>()
                CodeforcesAPI.getContests()?.result?.forEach {
                    if(it.phase.isFutureOrRunning()){
                        adapter.add(it.name)
                        contests.add(it)
                    }
                }

                AlertDialog.Builder(activity)
                    .setTitle("Running or Future Contests")
                    .setAdapter(adapter) { _, index ->
                        contestIDEditText.setText(contests[index].id.toString())
                    }.create().show()

                button.isEnabled = true
            }
        }

        //show running jobs
        view.findViewById<Button>(R.id.button_running_jobs).setOnClickListener {
            stuff.text = JobServicesCenter.getRunningJobServices(activity).joinToString(separator = "\n"){ info ->
                "Job " + info.id + ": " + info.service.shortClassName.removeSuffix("JobService").removePrefix(".job_services.")
            }
        }

        //test notify
        view.findViewById<Button>(R.id.button_test_notify).setOnClickListener{ button -> button as Button

        }
    }
}
