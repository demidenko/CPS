package com.example.test3

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import kotlinx.coroutines.*
import java.lang.Exception


class Settings: AppCompatActivity() {
    val scope = CoroutineScope(Job() + Dispatchers.Main)
    lateinit var manager: AccountManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_codeforces)

        val managerType = intent.getStringExtra("manager")!!

        manager = when(managerType){
            CodeforcesAccountManager::class.java.name ->    CodeforcesAccountManager(this)
            AtCoderAccountManager::class.java.name ->       AtCoderAccountManager(this)
            TopCoderAccountManager::class.java.name ->      TopCoderAccountManager(this)
            ACMPAccountManager::class.java.name ->          ACMPAccountManager(this)
            else -> throw Exception("Unknown type of manager")
        }

        supportActionBar?.title =  manager.preferences_file_name + " settings"

        val handleEditor: EditText = findViewById(R.id.editText)

        val saveButton: Button = findViewById(R.id.buttonSave)

        var savedInfo = manager.getSavedInfo()

        handleEditor.setText(savedInfo.usedID)

        val preview = findViewById<TextView>(R.id.textView2).also { it.text="" }
        val suggestionsView = findViewById<TextView>(R.id.textView3).also { it.text="" }

        var lastLoadedInfo: UserInfo? = null

        handleEditor.addTextChangedListener {
            val handle = it!!.toString()
            saveButton.text = if(handle.equals(savedInfo.usedID,true)) "Saved" else "Save"

            if(lastLoadedInfo!=null && handle == lastLoadedInfo!!.usedID) return@addTextChangedListener

            saveButton.isEnabled = false
            lastLoadedInfo = null

            preview.text = "..."
            scope.launch {
                delay(300)
                val info = manager.loadInfo(handle)
                if(handle == it.toString()){
                    preview.text = info?.makeInfoString() ?: "error"
                    lastLoadedInfo = info
                    saveButton.isEnabled = true
                }
            }

            if(handle.length < 3) suggestionsView.text = ""
            else{
                suggestionsView.text = "..."
                scope.launch {
                    delay(300)
                    val suggestions = manager.loadSuggestions(handle)
                    if(handle == it.toString()){
                        var str = ""
                        suggestions?.forEach { pair -> str+=pair.first+"\n" }
                        suggestionsView.text = str
                    }
                }
            }
        }

        saveButton.setOnClickListener{
            if(saveButton.text == "Save"){
                val currentHandle = lastLoadedInfo!!.usedID
                manager.saveInfo(lastLoadedInfo!!)
                savedInfo = lastLoadedInfo!!
                saveButton.text = "Saved"
                handleEditor.setText(currentHandle)
                handleEditor.setSelection(currentHandle.length)
            }
        }
    }

    override fun onBackPressed(){
        println("back")
        setResult(Activity.RESULT_OK, Intent().putExtra("manager", manager.preferences_file_name))
        finish()
        super.onBackPressed()
    }

}
