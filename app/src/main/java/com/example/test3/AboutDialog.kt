package com.example.test3

import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.example.test3.utils.getCurrentTime
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

class AboutDialog: DialogFragment() {

    companion object {
        fun showDialog(activity: MainActivity) {
            AboutDialog().show(activity.supportFragmentManager, "about_dialog")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val context = requireContext()

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_about, null).apply {

            val title = """
                   Competitive
                   Programming
                && Solving
            """.trimIndent()

            val spannedTitle = SpannableString(title)
            title.forEachIndexed { index, c ->
                if(c.isUpperCase()) spannedTitle.setSpan(StyleSpan(Typeface.BOLD), index, index+1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            }

            findViewById<TextView>(R.id.about_title)?.text = spannedTitle

            val versionTextView = findViewById<TextView>(R.id.about_version).apply {
                text = "   version = ${BuildConfig.VERSION_NAME}"
            }

            fun makeDevModeText(enabled: Boolean) = "   dev_mode = $enabled"

            val devTextView = findViewById<TextView>(R.id.about_dev_mode)

            val devCheckBox = findViewById<CheckBox>(R.id.about_dev_checkbox).apply {
                runBlocking { context.settingsDev.devEnabled() }.let { devEnabled ->
                    isVisible = devEnabled
                    devTextView.isVisible = devEnabled
                    devTextView.text = makeDevModeText(devEnabled)
                    isChecked = devEnabled
                }
                setOnCheckedChangeListener { buttonView, isChecked ->
                    runBlocking { context.settingsDev.devEnabled(isChecked) }
                    devTextView.text = makeDevModeText(isChecked)
                }
            }

            //apply dev mode
            setOnClickListener(PatternClickListener("._.._..."){
                if(devCheckBox.isVisible) return@PatternClickListener
                devTextView.isVisible = true
                devCheckBox.isVisible = true
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

            n = pushes
            shortIndices = shorts.toIntArray()
            longIndices = longs.toIntArray()
        }

        private val timeClicks = Array(n) { Instant.DISTANT_PAST }
        private var clicks = 0

        override fun onClick(v: View?) {
            getCurrentTime().let { cur ->
                if(clicks > 0 && cur - timeClicks[(clicks-1)%n] > 1.seconds) clicks = 0
                timeClicks[clicks%n] = cur
            }
            ++clicks
            if(clicks >= n) {
                val t = (n-2 downTo 0).map { i ->
                    timeClicks[(clicks-1-i)%n] - timeClicks[(clicks-2-i)%n]
                }

                val x: Duration = longIndices.minOfOrNull { t[it] } ?: INFINITE
                val y: Duration = shortIndices.maxOfOrNull { t[it] } ?: ZERO

                if(x > y) {
                    clicks = 0
                    callback()
                }
            }
        }

    }

}