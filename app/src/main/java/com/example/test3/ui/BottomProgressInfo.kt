package com.example.test3.ui

import android.content.Context
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.test3.MainActivity
import com.example.test3.R
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class BottomProgressInfo(title: String, context: Context): ConstraintLayout(context) {

    private val progressBar: LinearProgressIndicator by lazy { findViewById(R.id.progress_bottom_info_bar) }

    init {
        inflate(context, R.layout.progress_bottom_info, this)
        findViewById<TextView>(R.id.progress_bottom_info_title).text = title
        isGone = true
    }

    fun setState(current: Int, full: Int){
        progressBar.apply {
            max = full
            progress = current
        }
        isVisible = full > 0
    }
}

fun MainActivity.subscribeProgressBar(
    title: String,
    flow: StateFlow<Pair<Int,Int>?>
){
    var progressBar: BottomProgressInfo? = null
    lifecycleScope.launch {
        flow.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect { progress ->
            if(progress == null) {
                progressBar?.let {
                    progressBarHolder.removeView(it)
                }
                progressBar = null
            } else {
                val bar = progressBar ?: BottomProgressInfo(title, this@subscribeProgressBar).also {
                    progressBar = it
                    progressBarHolder.addView(it)
                }
                val (cur, max) = progress
                bar.setState(cur, max)
            }
        }
    }
}