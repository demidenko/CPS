package com.example.test3

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AboutDialog: DialogFragment() {

    companion object {
        fun showDialog(activity: MainActivity) {
            AboutDialog().show(activity.supportFragmentManager, "about_dialog")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val context = requireContext()

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_about, null).apply {
            findViewById<TextView>(R.id.about_text_view)?.text = """
                   Competitive
                   Programming
                && Solving
                
                version = ${BuildConfig.VERSION_NAME}
            """.trimIndent()

            //apply dev mode
            val timeClicks = LongArray(6)
            var clicks = 0
            setOnClickListener {
                val cur = System.currentTimeMillis()
                if(clicks>0 && cur - timeClicks[clicks-1] > 1000) clicks = 0
                timeClicks[clicks%6] = cur
                ++clicks
                if(clicks >= 6){
                    val t = (0..4).map { i ->
                        timeClicks[(clicks-1-i)%6] - timeClicks[(clicks-2-i)%6]
                    }
                    if(minOf(t[1],t[3]) > maxOf(t[0],t[2],t[4])){
                        with(requireActivity() as MainActivity){
                            SettingsDev(this).setDevEnabled(true)
                            showToast("dev enabled")
                        }
                        clicks = 0
                    }
                }
            }
        }

        val builder = MaterialAlertDialogBuilder(context)
            .setView(view)

        val dialog = builder.create()

        return dialog
    }



}