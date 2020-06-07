package com.example.test3

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.example.test3.contest_watch.CodeforcesContestWatchService

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


        //monitor alpha
        view.findViewById<Button>(R.id.button_watcher).setOnClickListener { button -> button as Button

            val manager = activity.accountsFragment.codeforcesAccountManager
            val handle = manager.savedInfo.userID
            val contestID = view.findViewById<EditText>(R.id.text_editor).text.toString().toInt()
            activity.startForegroundService(
                Intent(activity, CodeforcesContestWatchService::class.java)
                    .putExtra("handle", handle)
                    .putExtra("contestID", contestID)
            )
        }

    }
}
