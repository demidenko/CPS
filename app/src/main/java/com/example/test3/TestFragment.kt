package com.example.test3

import android.app.IntentService
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.get
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*

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



        val watcher_layout = layoutInflater.inflate(R.layout.watcher_panel, view.findViewById(R.id.watcher_panel)) as RelativeLayout
        val problems_layout = watcher_layout.findViewById<LinearLayout>(R.id.watcher_problems)


        //monitor alpha
        var watcher: CodeforcesContestWatcher? = null
        view.findViewById<Button>(R.id.button_watcher).setOnClickListener { button -> button as Button

            if(button.text == "running..."){
                watcher?.stop()
                watcher = null
                button.text = "run"
                return@setOnClickListener
            }

            val manager = activity.accountsFragment.codeforcesAccountManager
            val handle = manager.savedInfo.userID
            val contestID = view.findViewById<EditText>(R.id.text_editor).text.toString().toInt()

            watcher_layout.findViewById<TextView>(R.id.watcher_handle).text = handle

            button.text = "running..."
            watcher = CodeforcesContestWatcher(handle, contestID, activity.scope).apply {
                addCodeforcesContestWatchListener(object : CodeforcesContestWatchListener() {
                    override fun onSetContestName(contestName: String) {
                        watcher_layout.findViewById<TextView>(R.id.watcher_contest_name).text = contestName
                    }

                    lateinit var problems: Array<String>
                    override suspend fun onSetProblemNames(problemNames: Array<String>) {
                        problems = problemNames
                        withContext(Dispatchers.Main) {
                            problems_layout.removeAllViews()
                            problems.forEach { s ->
                                val t = TextView(activity)
                                t.text = "$s\n0"
                                problems_layout.addView(t)
                            }
                        }
                    }

                    var phase = ""
                    var progress = ""
                    fun showPhaseAndProgress(){
                        watcher_layout.findViewById<TextView>(R.id.watcher_phase).text = "$phase $progress"
                    }

                    override fun onSetContestPhase(phaseCodeforces: CodeforcesContestPhase) {
                        phase = phaseCodeforces.name
                        showPhaseAndProgress()
                    }

                    override fun onSetRemainingTime(time: String) {
                        progress = time
                        showPhaseAndProgress()
                    }

                    override fun onSetSysTestProgress(percents: Int) {
                        progress = "$percents%"
                        showPhaseAndProgress()
                    }

                    var _rank = ""
                    var _points = ""
                    fun showRankAndPoints(){
                        watcher_layout.findViewById<TextView>(R.id.watcher_contestant_info).text = "rank: $_rank | score: $_points"
                    }

                    override fun onSetContestantRank(rank: Int) {
                        _rank = rank.toString()
                        showRankAndPoints()
                    }

                    override fun onSetContestantPoints(points: Int) {
                        _points = points.toString()
                        showRankAndPoints()
                    }


                    override fun onSetProblemStatus(problem: String, status: String, points: Int) {
                        val index = problems.indexOf(problem)
                        val t = problems_layout[index] as TextView
                        t.text = "${problems[index]}\n$points"
                    }

                })
                start()
            }
        }

    }
}


class CodeforcesContestWatchService : IntentService("cf-contest-watch"){
    val scope = CoroutineScope(Job() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        Toast.makeText(this@CodeforcesContestWatchService, "created", Toast.LENGTH_LONG).show()
    }

    override fun onHandleIntent(intent: Intent?) {
        scope.launch {
            delay(10000)
            Toast.makeText(this@CodeforcesContestWatchService, "finished " + (intent?.extras?.getString("id") ?: ""), Toast.LENGTH_LONG).show()
        }
    }
}