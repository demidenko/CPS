package com.example.test3

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.test3.account_manager.AccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.UserInfo
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AccountEditFragment(

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

        activity.setActionBarSubTitle("::accounts.${manager.PREFERENCES_FILE_NAME}.edit")
        activity.navigation.visibility = View.GONE


        val handleEditor: EditText = view.findViewById(R.id.account_create_handle_edit)

        handleEditor.addTextChangedListener(
            UserIDChangeWatcher(
                this,
                manager,
                handleEditor,
                view.findViewById(R.id.account_create_save_button),
                view.findViewById(R.id.account_create_user_info),
                view.findViewById(R.id.account_create_suggestions)
            )
        )

    }

    class UserIDChangeWatcher(
        val fragment: Fragment,
        val manager: AccountManager,
        val handleEditor: EditText,
        val saveButton: Button,
        val preview: TextView,
        val suggestionsView: TextView
    ) : TextWatcher {

        val activity = fragment.requireActivity() as MainActivity

        var jobInfo: Job? = null
        var jobSuggestions: Job? = null

        var savedInfo = manager.savedInfo
        var lastLoadedInfo: UserInfo? = null

        init {
            handleEditor.setText(savedInfo.userID)
            preview.text = ""
            suggestionsView.text = ""

            saveButton.setOnClickListener {
                if(saveButton.text == "Save"){
                    with(lastLoadedInfo ?: return@setOnClickListener){
                        val currentUserId = userID
                        manager.savedInfo = this
                        savedInfo = this
                        saveButton.text = "Saved"
                        handleEditor.setText(currentUserId)
                        handleEditor.setSelection(currentUserId.length)
                        activity.accountsFragment.panels.find { panel ->
                            panel.manager.PREFERENCES_FILE_NAME == manager.PREFERENCES_FILE_NAME
                        }?.show()
                    }
                }
            }
        }

        private val successColor = fragment.resources.getColor(R.color.success, null)
        private val failColor = fragment.resources.getColor(R.color.fail, null)

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(editable: Editable?) {
            val usedId = editable?.toString() ?: return

            saveButton.text = if(usedId.equals(savedInfo.userID,true)) "Saved" else "Save"

            if(usedId == lastLoadedInfo?.userID) return

            saveButton.isEnabled = false
            lastLoadedInfo = null

            preview.text = "..."
            jobInfo?.cancel()
            jobInfo = activity.scope.launch {
                delay(300)
                val info = manager.loadInfo(usedId)
                if(usedId == editable.toString()){
                    preview.text = info.makeInfoString()
                    lastLoadedInfo = info
                    saveButton.isEnabled = true
                    handleEditor.backgroundTintList = ColorStateList.valueOf(
                        if(info.status==STATUS.OK) successColor
                        else failColor
                    )
                }
            }

            if(usedId.length < 3) suggestionsView.text = ""
            else{
                suggestionsView.text = "..."
                jobSuggestions?.cancel()
                jobSuggestions = activity.scope.launch {
                    delay(300)
                    val suggestions = manager.loadSuggestions(usedId)
                    if(usedId == editable.toString()){
                        val str = buildString {
                            suggestions?.forEach { pair -> appendLine(pair.first) }
                        }
                        suggestionsView.text = str
                    }
                }
            }
        }

    }
}