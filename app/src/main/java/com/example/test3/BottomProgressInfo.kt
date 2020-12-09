package com.example.test3

import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BottomProgressInfo(val size: Int, val title: String, private val activity: MainActivity) {
    private val view = activity.layoutInflater.inflate(R.layout.progress_bottom_info, null).apply {
        findViewById<TextView>(R.id.progress_bottom_info_title).text = title
    }

    private val progressBar = view.findViewById<ProgressBar>(R.id.progress_bottom_info_bar)

    init {
        if(size>0) {
            progressBar.max = size
            progressBar.progress = 0
            activity.progress_bar_holder.addView(view)
        }
    }

    fun finish(){
        if(size>0){
            activity.lifecycleScope.launch {
                delay(1000)
                activity.progress_bar_holder.removeView(view)
            }
        }
    }

    fun increment(){
        progressBar.incrementProgressBy(1)
    }

}