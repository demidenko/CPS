package com.example.test3.utils

import android.text.Html
import androidx.core.text.HtmlCompat
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit


val httpClient = OkHttpClient
    .Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()


fun fromHTML(s: String): String = Html.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
