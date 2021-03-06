package com.example.test3.ui

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.test3.MainActivity
import com.example.test3.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BottomProgressInfo(title: String, private val mainActivity: MainActivity): ConstraintLayout(mainActivity) {

    private val progressBar: ProgressBar by lazy { findViewById(R.id.progress_bottom_info_bar) }
    private var size = 0

    init {
        inflate(mainActivity, R.layout.progress_bottom_info, this)
        findViewById<TextView>(R.id.progress_bottom_info_title).text = title
        visibility = View.GONE
        mainActivity.progressBarHolder.addView(this)
    }

    fun start(size: Int): BottomProgressInfo {
        if(size > 0) {
            this.size = size
            progressBar.apply {
                max = size
                progress = 0
            }
            visibility = View.VISIBLE
        }
        return this
    }

    fun finish(){
        if(size>0){
            mainActivity.lifecycleScope.launch {
                delay(1000)
                mainActivity.progressBarHolder.removeView(this@BottomProgressInfo)
            }
        }
    }

    fun increment(){
        progressBar.incrementProgressBy(1)
    }

}