package com.example.test3.utils

import android.text.Html
import android.widget.ImageButton
import androidx.core.text.HtmlCompat
import okhttp3.OkHttpClient
import java.util.*
import java.util.concurrent.TimeUnit


val httpClient = OkHttpClient
    .Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()


fun fromHTML(s: String): String = Html.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()


class SharedReloadButton(private val button: ImageButton) {
    private val current = TreeSet<String>()

    fun toggle(str: String) {
        if(current.contains(str)){
            current.remove(str)
            if(current.isEmpty()){
                button.isEnabled = true
            }
        }else{
            if(current.isEmpty()){
                button.isEnabled = false
            }
            current.add(str)
        }
    }
}

interface ProgressListener {
    fun onStart(max: Int)
    fun onIncrement()
    fun onFinish()
}