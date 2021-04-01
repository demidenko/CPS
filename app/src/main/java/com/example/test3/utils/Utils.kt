package com.example.test3.utils

import android.text.Html
import android.text.Spanned
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.test3.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

fun getCurrentTimeSeconds() = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())

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


fun ImageButton.on() {
    animate().alpha(1f).setDuration(500).start()
}
fun ImageButton.off() {
    animate().alpha(.5f).setDuration(500).start()
}
fun ImageButton.enable() {
    if(isEnabled) return
    on()
    isEnabled = true
}
fun ImageButton.disable() {
    if(!isEnabled) return
    isEnabled = false
    off()
}

class SharedReloadButton(private val button: ImageButton) {
    private val current = mutableSetOf<String>()

    fun startReload(tag: String) {
        if(current.contains(tag)) throw Exception("$tag already started")
        if(current.isEmpty()) button.disable()
        current.add(tag);
    }

    fun stopReload(tag: String) {
        //if(!current.contains(tag)) throw Exception("$tag not started to be stopped")
        current.remove(tag)
        if(current.isEmpty()) button.enable()
    }
}


class MutableSetLiveSize<T>() {
    private val s = mutableSetOf<T>()
    private val size = MutableLiveData<Int>(0)
    val sizeLiveData: LiveData<Int> get() = size

    fun values() = s.toSet()
    fun contains(element: T) = s.contains(element)
    fun add(element: T) {
        s.add(element)
        size.value = s.size
    }
    fun addAll(elements: Collection<T>) {
        s.addAll(elements)
        size.value = s.size
    }
    fun remove(element: T) {
        s.remove(element)
        size.value = s.size
    }
    fun clear() {
        s.clear()
        size.value = 0
    }
}

open class CPSDataStore(protected val dataStore: DataStore<Preferences>)

private fun setupDescription(
    view: ConstraintLayout,
    description: String
){
    if(description.isNotBlank()){
        view.findViewById<TextView>(R.id.settings_item_description).apply {
            text = description
            visibility = View.VISIBLE
        }
    }
}

fun setupSwitch(
    view: ConstraintLayout,
    title: String,
    checked: Boolean,
    description: String = "",
    onChangeCallback: (buttonView: CompoundButton, isChecked: Boolean) -> Unit
){
    view.apply {
        findViewById<TextView>(R.id.settings_item_title).text = title
        findViewById<SwitchMaterial>(R.id.settings_switcher_button).apply {
            isSaveEnabled = false
            isChecked = checked
            this.setOnCheckedChangeListener { buttonView, isChecked -> onChangeCallback(buttonView,isChecked) }
        }
        setupDescription(this, description)
    }
}

fun setupSelect(
    view: ConstraintLayout,
    title: String,
    options: Array<CharSequence>,
    initSelected: Int,
    description: String = "",
    onChangeCallback: (buttonView: View, optionSelected: Int) -> Unit
){
    view.apply {
        findViewById<TextView>(R.id.settings_item_title).text = title
        val selectedTextView = findViewById<TextView>(R.id.settings_select_button)
        selectedTextView.text = options[initSelected]
        var selected = initSelected
        setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle(title)
                .setSingleChoiceItems(options, selected){ d, index ->
                    selected = index
                    onChangeCallback(it, index)
                    selectedTextView.text = options[index]
                    d.dismiss()
                }
                .create().show()
        }
        setupDescription(this, description)
    }
}

fun setupMultiSelect(
    view: ConstraintLayout,
    title: String,
    options: Array<CharSequence>,
    initSelected: BooleanArray,
    description: String = "",
    onChangeCallback: (buttonView: View, optionsSelected: BooleanArray) -> Unit
){
    view.apply {
        findViewById<TextView>(R.id.settings_item_title).text = title
        val selectedTextView = findViewById<TextView>(R.id.settings_multiselect_button)
        selectedTextView.text = initSelected.count { it }.toString()
        var selected = initSelected.clone()
        setOnClickListener {
            val currentSelected = selected.clone()
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMultiChoiceItems(options, currentSelected){ _, _, _ -> }
                .setPositiveButton("Save"){ d, _ ->
                    selected = currentSelected
                    onChangeCallback(it, selected.clone())
                    selectedTextView.text = selected.count { it }.toString()
                    d.dismiss()
                }
                .create().show()
        }
        setupDescription(this, description)
    }
}

enum class LoadingState {
    PENDING, LOADING, FAILED;

    companion object {
        fun combineLoadingStateFlows(states: List<StateFlow<LoadingState>>): Flow<LoadingState> =
            combine(states){
                when {
                    it.contains(LOADING) -> LOADING
                    it.contains(FAILED) -> FAILED
                    else -> PENDING
                }
            }
    }
}