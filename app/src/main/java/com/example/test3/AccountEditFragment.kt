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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        return inflater.inflate(R.layout.fragment_account_edit, container, false)
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


        val handleEditor: EditText = view.findViewById(R.id.account_edit_handle_edit)

        handleEditor.addTextChangedListener(
            UserIDChangeWatcher(
                this,
                manager,
                handleEditor,
                view.findViewById(R.id.account_edit_save_button),
                view.findViewById(R.id.account_edit_user_info),
                view.findViewById(R.id.account_edit_suggestions)
            )
        )

    }

    class UserIDChangeWatcher(
        val fragment: Fragment,
        val manager: AccountManager,
        val handleEditor: EditText,
        val saveButton: Button,
        val preview: TextView,
        val suggestionsView: RecyclerView
    ) : TextWatcher {

        val activity = fragment.requireActivity() as MainActivity

        var jobInfo: Job? = null
        var jobSuggestions: Job? = null

        var savedInfo = manager.savedInfo
        var lastLoadedInfo: UserInfo? = null

        val suggestionsAdapter: SuggestionsItemsAdapter

        init {
            handleEditor.setText(savedInfo.userID)
            preview.text = ""

            suggestionsAdapter = SuggestionsItemsAdapter(this)

            suggestionsView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = suggestionsAdapter
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                setHasFixedSize(true)
            }

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

        var changedByChoose = false
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


            if(changedByChoose) changedByChoose = false
            else {
                if (usedId.length < 3) suggestionsAdapter.clear()
                else {
                    suggestionsAdapter.loading()
                    jobSuggestions?.cancel()
                    jobSuggestions = activity.scope.launch {
                        delay(300)
                        val suggestions = manager.loadSuggestions(usedId)
                        if (usedId == editable.toString()) {
                            suggestionsAdapter.new(suggestions)
                        }
                    }
                }
            }
        }

        class SuggestionsItemsAdapter(val watcher: UserIDChangeWatcher) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

            class Holder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view){
                val title:TextView = view.findViewById(R.id.suggestions_item_title)
                val info:TextView = view.findViewById(R.id.suggestions_item_info)
            }

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.account_suggestions_item, parent, false) as ConstraintLayout
                return Holder(view)
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                with(holder as Holder){
                    val (title, info, userId) = data[position]
                    this.title.text = title
                    this.info.text = info
                    view.setOnClickListener {
                        watcher.changedByChoose = true
                        watcher.handleEditor.setText(userId)
                        watcher.handleEditor.setSelection(userId.length)
                    }
                }
            }

            override fun getItemCount(): Int = data.size

            private var data : List<Triple<String,String,String>> = emptyList()

            fun new(suggestions: List<Triple<String,String,String>>?){
                data = suggestions ?: emptyList()
                notifyDataSetChanged()
            }

            fun clear(){
                data = emptyList()
                notifyDataSetChanged()
            }

            fun loading(){
                clear()
            }

        }

    }
}