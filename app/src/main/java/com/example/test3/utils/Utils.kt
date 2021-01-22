package com.example.test3.utils

import android.text.Html
import android.text.Spanned
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.view.get
import com.example.test3.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import java.util.*
import java.util.concurrent.TimeUnit


val httpClient = OkHttpClient
    .Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

val jsonCPS = Json{ ignoreUnknownKeys = true }
val jsonConverterFactory = jsonCPS.asConverterFactory(MediaType.get("application/json"))

fun fromHTML(s: String): Spanned {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        Html.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY)
    } else {
        Html.fromHtml(s)
    }
}

fun signedToString(x: Int): String = if(x>0) "+$x" else "$x"


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

fun createAndAddSwitch(
    view: LinearLayout,
    title: String,
    checked: Boolean,
    description: String = "",
    onChangeCallback: (buttonView: CompoundButton, isChecked: Boolean) -> Unit
): View {
    return view[view.childCount-1].apply {
        findViewById<TextView>(R.id.account_settings_switcher_title).text = title
        findViewById<SwitchMaterial>(R.id.account_settings_switcher_button).apply {
            isChecked = checked
            this.setOnCheckedChangeListener { buttonView, isChecked -> onChangeCallback(buttonView,isChecked) }
        }
        if(description.isNotBlank()){
            findViewById<TextView>(R.id.account_settings_switcher_description).apply {
                text = description
                visibility = View.VISIBLE
            }
        }
    }
}
