package com.example.test3.ui

import android.view.View
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.test3.R
import com.google.android.material.switchmaterial.SwitchMaterial

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

fun ImageButton.on() {
    animate().alpha(1f).setDuration(500).start()
}

fun ImageButton.off() {
    animate().alpha(.5f).setDuration(500).start()
}

fun ImageButton.onIff(condition: Boolean) {
    if(condition) on() else off()
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

fun ImageButton.enableIff(condition: Boolean) {
    if(condition) enable() else disable()
}