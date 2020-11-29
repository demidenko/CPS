package com.example.test3.utils

import android.text.Html
import android.text.Spanned
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


fun fromHTML(s: String): Spanned = Html.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY)


class SharedReloadButton(private val button: ImageButton) {
    private val current = TreeSet<String>()

    fun startReload(tag: String) {
        if(current.contains(tag)) throw Exception("$tag already started")
        if(current.isEmpty()) button.isEnabled = false
        current.add(tag);
    }

    fun stopReload(tag: String) {
        if(!current.contains(tag)) throw Exception("$tag not started to be stopped")
        current.remove(tag)
        if(current.isEmpty()) button.isEnabled = true
    }
}
