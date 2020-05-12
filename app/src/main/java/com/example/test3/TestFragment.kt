package com.example.test3

import android.app.IntentService
import android.app.NotificationManager
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.example.test3.account_manager.CodeforcesAccountManager
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.*

class TestFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_test, container, false)
    }

    lateinit var textView: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity

        textView = activity.findViewById(R.id.stuff_textview)



        //monitor alpha
        var job: Job? = null
        view.findViewById<Button>(R.id.button_watcher).setOnClickListener { button -> button as Button

            //activity.stopService(Intent(activity, CodeforcesContestWatchService::class.java))

            //activity.startService(Intent(activity, CodeforcesContestWatchService::class.java).putExtra("id", view.findViewById<EditText>(R.id.text_editor).text.toString()))

            //return@setOnClickListener


            if(button.text == "running..."){
                job?.cancel()
                job = null
                button.text = "run"
                return@setOnClickListener
            }

            button.text = "running..."
            job = activity.scope.launch {
                val manager = activity.accountsFragment.codeforcesAccountManager
                val contestID = view.findViewById<EditText>(R.id.text_editor).text.toString().toInt()
                var contestName: String? = null
                val handle = manager.savedInfo.userID
                val address = "https://codeforces.com/api/contest.standings?contestId=$contestID&showUnofficial=false&handles=$handle"
                println(address)

                textView.text = "???"
                var rank: Int? = null
                var points: Int? = null
                var currentTime: Int? = null
                var tasksPrevious: ArrayList<Pair<Int, String>>? = null
                var problemNames: ArrayList<String>? = null

                while (true) {
                    var phase: String? = null
                    val tasks = arrayListOf<Pair<Int,String>>()
                    try {
                        withContext(Dispatchers.IO) {
                            with(JsonReaderFromURL(address) ?: return@withContext ) {
                                beginObject()
                                if (nextString("status") == "FAILED") return@withContext
                                nextName()
                                readObject {
                                    while (hasNext()) when (nextName()) {
                                        "contest" -> readObject {
                                            while (hasNext()) when (nextName()) {
                                                "phase" -> phase = nextString()
                                                "relativeTimeSeconds" -> currentTime = nextInt()
                                                "name" -> contestName = nextString()
                                                else -> skipValue()
                                            }
                                        }
                                        "problems" -> {
                                            if (problemNames == null) {
                                                val tmp = ArrayList<String>()
                                                readArray { tmp.add(readObjectFields("index")[0] as String) }
                                                problemNames = tmp
                                            } else skipValue()
                                        }
                                        "rows" -> readArrayOfObjects {
                                            while (hasNext()) when (nextName()) {
                                                "rank" -> rank = nextInt()
                                                "points" -> points = nextInt()
                                                "problemResults" -> readArray {
                                                    val arr = readObjectFields("points", "type")
                                                    val pts = (arr[0] as Double).toInt()
                                                    val status = arr[1] as String
                                                    tasks.add(Pair(pts, status))
                                                }
                                                else -> skipValue()
                                            }
                                        }
                                        else -> skipValue()
                                    }
                                }
                                this.close()
                            }
                        }
                    }catch (e: JsonEncodingException){

                    }catch (e: JsonDataException){

                    }

                    var str = "$handle\n\n"

                    str+="$contestName\n$phase\n"

                    if(phase == "SYSTEM_TEST"){
                        withContext(Dispatchers.IO){
                            val page = readURLData("https://codeforces.com/contest/$contestID") ?: return@withContext
                            var i = page.indexOf("<span class=\"contest-state-regular\">")
                            if(i!=-1){
                                i = page.indexOf(">", i+1)
                                val progress = page.substring(i+1, page.indexOf("</",i+1))
                                str+=progress+"\n"
                            }
                        }
                    }

                    if(phase == "CODING") currentTime?.let{
                        val SS = it % 60
                        val MM = it / 60 % 60
                        val HH = it / 60 / 60
                        str+=String.format("%02d:%02d:%02d\n", HH, MM, SS)
                    }

                    str+="\nrank: $rank \n"
                    rank?.let{
                        str+="points: $points \n\n"
                        println("tasks ${tasks.size}")
                        tasks.forEachIndexed { index, (pts,status) ->
                            val problemName = problemNames!![index]
                            str+="$problemName\t\t$pts\t\t[$status] \n"
                            if(phase=="SYSTEM_TEST"){
                                tasksPrevious?.let {
                                    if(it.elementAt(index).second!=status){
                                        //Toast.makeText(activity, "$problemName -> $pts points", Toast.LENGTH_LONG).show()
                                        val builder = NotificationCompat.Builder(activity, "test").apply {
                                            setContentTitle("problems")
                                            setContentText("$problemName -> $pts points")
                                            setSmallIcon(R.drawable.ic_news)
                                        }
                                        (activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(2 + index, builder.build());
                                    }
                                }
                            }
                        }
                        tasksPrevious = tasks
                    }

                    if(phase=="FINISHED"){
                        val info = manager.loadInfo(handle) as CodeforcesAccountManager.CodeforcesUserInfo
                        val old = manager.savedInfo as CodeforcesAccountManager.CodeforcesUserInfo
                        if(info.rating!=old.rating){
                            val builder = NotificationCompat.Builder(activity, "test").apply {
                                val difference = info.rating - old.rating
                                setContentTitle("$handle new rating: ${info.rating}")
                                val diff_str = (if (info.rating < old.rating) "" else "+") + difference
                                setContentText("$diff_str, rank: $rank")
                                setSubText("CodeForces rating changes")
                                setSmallIcon(if(difference<0) R.drawable.ic_rating_down else R.drawable.ic_rating_up)
                                //color = manager.getColor(info) ?: activity.defaultTextColor
                            }
                            (activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1, builder.build());

                            manager.savedInfo = info
                            activity.accountsFragment.codeforcesPanel.show()

                            button.text = "run"
                            return@launch
                        }
                    }

                    textView.text = str

                    when(phase){
                        "CODING", "SYSTEM_TEST" -> delay(1_000)
                        "FINISHED" -> delay(30_000)
                        else -> delay(60_000)
                    }
                }
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