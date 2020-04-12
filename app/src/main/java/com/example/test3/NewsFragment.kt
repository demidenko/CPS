package com.example.test3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.launch


class NewsFragment() : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = getActivity()!! as MainActivity //:)

        view.findViewById<Button>(R.id.button_reload_cf).apply {
            setOnClickListener {
                val textView: TextView = view.findViewById(R.id.codeforces_news)
                (it as Button).text = "..."
                it.isEnabled = false
                activity.scope.launch {
                    readURLData("https://codeforces.com/top?locale=ru")?.let { s ->
                        var res: String = ""
                        var i = 0
                        while (true) {
                            i = s.indexOf("<div class=\"topic\"", i + 1)
                            if (i == -1) break
                            val title = s.substring(s.indexOf("<p>", i) + 3, s.indexOf("</p>", i))
                            res += title + "\n"
                        }
                        textView.text = res
                        it.text = "RELOAD"
                        it.isEnabled = true
                    }

                }
            }
            callOnClick()
        }

    }
}