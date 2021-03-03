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
            setOnClickListener(PatternClickListener("._.._..."){
                if(devCheckBox.isVisible) return@PatternClickListener
                devTextView.visibility = View.VISIBLE
                devCheckBox.visibility = View.VISIBLE
                devCheckBox.isChecked = true
            })
        }

        val builder = MaterialAlertDialogBuilder(context)
            .setView(view)

        val dialog = builder.create()

        return dialog
    }

    class PatternClickListener(
        pattern: String,
        private val callback: () -> Unit
    ): View.OnClickListener {

        private val n: Int
        private val shortIndices: IntArray
        private val longIndices: IntArray

        init {
            fun badPattern() { throw Exception("Bad pattern: [$pattern]") }

            if(pattern[0]!='.') badPattern()

            var pushes = 1
            val shorts = mutableListOf<Int>()
            val longs = mutableListOf<Int>()
            for(i in 1 until pattern.length){
                when(pattern[i]){
                    '.' -> {
                        pushes++
                        if(pattern[i-1] == '.') shorts.add(pushes-2)
                        else longs.add(pushes-2)
                    }
                    '_' -> if(pattern[i-1] != '.') badPattern()
                    else -> badPattern()
                }
            }

            println(shorts)
            println(longs)

            n = pushes
            shortIndices = shorts.toIntArray()
            longIndices = longs.toIntArray()
        }

        private val timeClicks = LongArray(n)
        private var clicks = 0

        override fun onClick(v: View?) {
            val cur = System.currentTimeMillis()
            if(clicks>0 && cur - timeClicks[(clicks-1)%n] > 1000) clicks = 0
            timeClicks[clicks%n] = cur
            ++clicks
            if(clicks >= n){
                val t = (n-2 downTo 0).map { i ->
                    timeClicks[(clicks-1-i)%n] - timeClicks[(clicks-2-i)%n]
                }

                val x = longIndices.map { t[it] }.minOrNull() ?: Long.MAX_VALUE
                val y = shortIndices.map { t[it] }.maxOrNull() ?: Long.MIN_VALUE

                if(x > y){
                    clicks = 0
                    callback()
                }
            }
        }

    }

}