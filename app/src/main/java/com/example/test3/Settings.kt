package com.example.test3

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import com.example.test3.account_manager.*
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
            CodeforcesAccountManager.preferences_file_name -> CodeforcesAccountManager(this)
            AtCoderAccountManager.preferences_file_name -> AtCoderAccountManager(this)
            TopCoderAccountManager.preferences_file_name -> TopCoderAccountManager(this)
            ACMPAccountManager.preferences_file_name -> ACMPAccountManager(this)
            else -> throw Exception("Unknown type of manager")
        }

        supportActionBar?.title =  manager.PREFERENCES_FILE_NAME + " settings"

        val handleEditor: EditText = findViewById(R.id.editText)

        val saveButton: Button = findViewById(R.id.buttonSave)

        var savedInfo = manager.savedInfo

        handleEditor.setText(savedInfo.userID)

        val preview = findViewById<TextView>(R.id.textViewUserInfo).apply { text="" }
        val suggestionsView = findViewById<TextView>(R.id.textViewSuggestions).apply { text="" }

        var lastLoadedInfo: UserInfo? = null

        var jobInfo: Job? = null
        var jobSuggestions: Job? = null

        handleEditor.addTextChangedListener {
            val handle = it!!.toString()
            saveButton.text = if(handle.equals(savedInfo.userID,true)) "Saved" else "Save"

            if(lastLoadedInfo!=null && handle == lastLoadedInfo!!.userID) return@addTextChangedListener

            saveButton.isEnabled = false
            lastLoadedInfo = null

            preview.text = "..."
            jobInfo?.cancel()
            jobInfo = scope.launch {
                delay(300)
                val info = manager.loadInfo(handle)
                if(handle == it.toString()){
                    preview.text = info.makeInfoString()
                    lastLoadedInfo = info
                    saveButton.isEnabled = true
                }
            }

            if(handle.length < 3) suggestionsView.text = ""
            else{
                suggestionsView.text = "..."
                jobSuggestions?.cancel()
                jobSuggestions = scope.launch {
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
                val currentHandle = lastLoadedInfo!!.userID
                manager.savedInfo = lastLoadedInfo!!
                savedInfo = lastLoadedInfo!!
                saveButton.text = "Saved"
                handleEditor.setText(currentHandle)
                handleEditor.setSelection(currentHandle.length)
            }
        }
    }

    override fun onBackPressed(){
        println("back")
        setResult(Activity.RESULT_OK, Intent().putExtra("manager", manager.PREFERENCES_FILE_NAME))
        finish()
        super.onBackPressed()
    }

}
