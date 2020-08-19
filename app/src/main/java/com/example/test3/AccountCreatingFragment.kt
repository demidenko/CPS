package com.example.test3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.test3.account_manager.UserInfo
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AccountCreatingFragment(

): Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_account_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity

        val managerType = arguments?.getString("manager") ?: throw Exception("Unset type of manager")

        val manager = activity.accountsFragment.panels
            .find { it.manager.PREFERENCES_FILE_NAME == managerType }
            ?.manager
            ?: throw Exception("Unknown type of manager")

        activity.setActionBarSubTitle("::accounts.${manager.PREFERENCES_FILE_NAME}.create")
        activity.navigation.visibility = View.GONE



        var savedInfo = manager.savedInfo

        val saveButton: Button = view.findViewById(R.id.account_create_save_button)

        val handleEditor: EditText = view.findViewById(R.id.account_create_handle_edit)
        handleEditor.setText(savedInfo.userID)

        val preview = view.findViewById<TextView>(R.id.account_create_user_info).apply { text="" }
        val suggestionsView = view.findViewById<TextView>(R.id.account_create_suggestions).apply { text="" }

        var lastLoadedInfo: UserInfo? = null

        var jobInfo: Job? = null
        var jobSuggestions: Job? = null

        handleEditor.addTextChangedListener { editable ->
            val handle = editable?.toString() ?: return@addTextChangedListener
            saveButton.text = if(handle.equals(savedInfo.userID,true)) "Saved" else "Save"

            if(handle == lastLoadedInfo?.userID) return@addTextChangedListener

            saveButton.isEnabled = false
            lastLoadedInfo = null

            preview.text = "..."
            jobInfo?.cancel()
            jobInfo = activity.scope.launch {
                delay(300)
                val info = manager.loadInfo(handle)
                if(handle == editable.toString()){
                    preview.text = info.makeInfoString()
                    lastLoadedInfo = info
                    saveButton.isEnabled = true
                }
            }

            if(handle.length < 3) suggestionsView.text = ""
            else{
                suggestionsView.text = "..."
                jobSuggestions?.cancel()
                jobSuggestions = activity.scope.launch {
                    delay(300)
                    val suggestions = manager.loadSuggestions(handle)
                    if(handle == editable.toString()){
                        var str = ""
                        suggestions?.forEach { pair -> str+=pair.first+"\n" }
                        suggestionsView.text = str
                    }
                }
            }
        }

        saveButton.setOnClickListener {
            if(saveButton.text == "Save"){
                with(lastLoadedInfo ?: return@setOnClickListener){
                    val currentHandle = userID
                    manager.savedInfo = this
                    savedInfo = this
                    saveButton.text = "Saved"
                    handleEditor.setText(currentHandle)
                    handleEditor.setSelection(currentHandle.length)
                    with(requireActivity() as MainActivity){
                        accountsFragment.panels.find { panel ->
                            panel.manager.PREFERENCES_FILE_NAME == manager.PREFERENCES_FILE_NAME
                        }?.show()
                    }
                }
            }
        }
    }
}