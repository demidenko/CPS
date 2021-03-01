package com.example.test3

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.view.isVisible
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
            findViewById<TextView>(R.id.about_title)?.text = """
                   Competitive
                   Programming
                && Solving
            """.trimIndent()

            val versionTextView = findViewById<TextView>(R.id.about_version).apply {
                text = "   version = ${BuildConfig.VERSION_NAME}"
            }

            fun makeDevModeText(enabled: Boolean) = "   dev_mode = $enabled"

            val devTextView = findViewById<TextView>(R.id.about_dev_mode)

            val devCheckBox = findViewById<CheckBox>(R.id.about_dev_checkbox).apply {
                context.settingsDev.getDevEnabled().let { devEnabled ->
                    visibility = if(devEnabled) View.VISIBLE else View.GONE
                    devTextView.visibility = if(devEnabled) View.VISIBLE else View.GONE
                    devTextView.text = makeDevModeText(devEnabled)
                    isChecked = devEnabled
                }
                setOnCheckedChangeListener { buttonView, isChecked ->
                    context.settingsDev.setDevEnabled(isChecked)
                    devTextView.text = makeDevModeText(isChecked)
                }
            }

            //apply dev mode
            val timeClicks = LongArray(6)
            var clicks = 0
            setOnClickListener {
                val cur = System.currentTimeMillis()
                if(clicks>0 && cur - timeClicks[(clicks-1)%6] > 1000) clicks = 0
                timeClicks[clicks%6] = cur
                ++clicks
                if(clicks >= 6 && !devCheckBox.isVisible){
                    val t = (0..4).map { i ->
                        timeClicks[(clicks-1-i)%6] - timeClicks[(clicks-2-i)%6]
                    }
                    if(minOf(t[1],t[3]) > maxOf(t[0],t[2],t[4])){
                        devTextView.visibility = View.VISIBLE
                        devCheckBox.visibility = View.VISIBLE
                        devCheckBox.isChecked = true
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