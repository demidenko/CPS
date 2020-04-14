package com.example.test3

import android.app.NotificationManager
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TestFragment() : Fragment() {
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

        //monitor alpha
        view.findViewById<Button>(R.id.button_test).setOnClickListener {button -> button as Button
            button.isEnabled = false
            button.text = "running..."
            activity.scope.launch {
                val manager = activity.accountsFragment.codeforcesAccountManager
                val contestID = view.findViewById<EditText>(R.id.text_editor).text.toString().toInt()
                var contestName: String? = null
                val handle = manager.savedInfo.userID
                val address = "https://codeforces.com/api/contest.standings?contestId=$contestID&showUnofficial=false&handles=$handle"
                println(address)

                val textView: TextView = activity.findViewById(R.id.stuff_textview)
                textView.text = "???"
                var rank: Int? = null
                var points: Int? = null
                var time: Int? = null
                val tasks = arrayListOf<Pair<Int,String>>()
                var tasks_prev: ArrayList<Pair<Int, String>>? = null
                var problemNames: ArrayList<String>? = null

                while (true) {
                    var phase: String? = null
                    try {
                        withContext(Dispatchers.IO) {
                            with(JsonReaderFromURL(address) ?: return@withContext) {
                                beginObject()
                                assert(nextName() == "status")
                                if (nextString() == "FAILED") return@withContext
                                assert(nextName() == "result")
                                readObject {
                                    while (hasNext()) when (nextName()) {
                                        "contest" -> readObject {
                                            while (hasNext()) when (nextName()) {
                                                "phase" -> phase = nextString()
                                                "relativeTimeSeconds" -> time = nextInt()
                                                "name" -> contestName = nextString()
                                                else -> skipValue()
                                            }
                                        }
                                        "problems" -> if (problemNames == null) {
                                            val tmp = ArrayList<String>()
                                            readArray { tmp.add(readObjectFields("index")[0] as String) }
                                            problemNames = tmp
                                        }
                                        "rows" -> readArray {
                                            readObject {
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
                                        }
                                        else -> skipValue()
                                    }
                                }
                            }
                        }
                    }catch (e: JsonEncodingException){

                    }catch (e: JsonDataException){

                    }

                    var s = "$handle\n\n"

                    s+="$contestName\n$phase\n"

                    if(phase == "SYSTEM_TEST"){
                        withContext(Dispatchers.IO){
                            val page = readURLData("https://codeforces.com/contest/1335") ?: return@withContext
                            var i = page.indexOf("<span class=\"contest-state-regular\">")
                            if(i!=-1){
                                i = page.indexOf(">", i+1)
                                val progress = page.substring(i+1, page.indexOf("</",i+1));
                                s+=progress+"\n"
                            }
                        }
                    }

                    if(phase == "CODING") time?.let{
                        val SS = it % 60
                        val MM = it / 60 % 60
                        val HH = it /60 / 60
                        s+="${HH/10}${HH%10}:${MM/10}${MM%10}:${SS/10}${SS%10}\n"
                    }

                    if(rank!=null){
                        s+="\nrank: $rank \n"
                        s+="points: $points \n\n"
                        tasks.forEachIndexed { index, (pts,status) ->
                            val pname = problemNames!![index]
                            s+="$pname\t\t$pts\t\t[$status] \n"
                            if(phase=="SYSTEM_TEST" && tasks_prev?.elementAt(index)?.second!=status){
                                //Toast.makeText(activity, "$pname -> $pts points", Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    if(phase=="SYSTEM_TEST") tasks_prev = tasks

                    if(phase=="FINISHED"){
                        manager.loadInfo(handle)?.let { info->
                            val old = manager.savedInfo as CodeforcesAccountManager.CodeforcesUserInfo
                            if(info!=old){
                                Toast.makeText(activity, "$handle Rating: ${old.rating} -> ${info.rating}", Toast.LENGTH_LONG).show()

                                /*val builder = NotificationCompat.Builder(activity, "test_channel").apply {
                                    setContentTitle("$handle new rating: ${info.rating}")
                                    val diff = (if (info.rating < old.rating) "" else "+") + (info.rating - old.rating)
                                    setContentText("$diff, rank: $rank")
                                    setSubText("CodeForces rating changes")
                                    color = manager.getColor(info) ?: activity.defaultTextColor
                                }
                                (activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1, builder.build());*/

                                manager.savedInfo = info
                                activity.accountsFragment.codeforcesPanel.show()

                                return@launch
                            }
                        }
                    }

                    textView.text = s

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

