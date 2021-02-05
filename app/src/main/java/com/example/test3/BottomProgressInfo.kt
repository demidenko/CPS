package com.example.test3

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BottomProgressInfo(val title: String, private val activity: MainActivity) {

    private lateinit var view: View
    private val progressBar by lazy { view.findViewById<ProgressBar>(R.id.progress_bottom_info_bar) }
    private var size = 0
    private fun createView() {
        if(size<1) return
        view = activity.layoutInflater.inflate(R.layout.progress_bottom_info, null).apply {
            findViewById<TextView>(R.id.progress_bottom_info_title).text = title
        }
        progressBar.apply {
            max = size
            progress = 0
        }
        activity.progress_bar_holder.addView(view)
    }

    fun start(size: Int): BottomProgressInfo {
        this.size = size
        createView()
        return this
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