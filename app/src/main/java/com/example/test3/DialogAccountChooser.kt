package com.example.test3

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.account_manager.AccountManager
import com.example.test3.account_manager.AccountSuggestion
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.UserInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class DialogAccountChooser(
    private val initialText: String,
    private val manager: AccountManager,
    private val cont: Continuation<UserInfo?>
): DialogFragment() {

    private lateinit var userIDChangeWatcher: UserIDChangeWatcher

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity() as MainActivity
        val view = activity.layoutInflater.inflate(R.layout.dialog_choose_userid, null)

        val builder = AlertDialog.Builder(activity)
            .setTitle("getUserID(${manager.PREFERENCES_FILE_NAME})")
            .setView(view)
            .setPositiveButton("return"){ _, _ ->
                with(userIDChangeWatcher.lastLoadedInfo!!){
                    if(status == STATUS.NOT_FOUND){
                        activity.showToast("User not found")
                        cont.resume(null)
                    }else{
                        cont.resume(this)
                    }
                }
            }

        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        val dialog = getDialog()!! as AlertDialog
        dialog.findViewById<TextView>(resources.getIdentifier("alertTitle", "id", "android")).typeface = Typeface.MONOSPACE

        val input = dialog.findViewById<EditText>(R.id.account_choose_input).apply {
            setText(initialText)
        }

        val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        userIDChangeWatcher = UserIDChangeWatcher(
            this,
            manager,
            input,
            saveButton,
            dialog.findViewById(R.id.account_choose_info),
            dialog.findViewById(R.id.account_choose_suggestions)
        )

        input.addTextChangedListener(userIDChangeWatcher)
    }

    override fun onCancel(dialog: DialogInterface) {
        cont.resume(null)
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

        var lastLoadedInfo: UserInfo? = null

        val suggestionsAdapter: SuggestionsItemsAdapter

        init {
            preview.text = ""

            suggestionsAdapter = SuggestionsItemsAdapter(this)

            suggestionsView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = suggestionsAdapter
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                setHasFixedSize(true)
            }
        }

        private val successColor = getColorFromResource(activity, R.color.success)
        private val failColor = getColorFromResource(activity, R.color.fail)

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        private var changedByChoose = false

        fun selectSuggestion(userID: String) {
            changedByChoose = true
            handleEditor.setText(userID)
            handleEditor.setSelection(userID.length)
        }

        override fun afterTextChanged(editable: Editable?) {
            //TODO: check symbols
            val userId = editable?.toString() ?: return

            if(userId == lastLoadedInfo?.userID) return

            saveButton.isEnabled = false
            lastLoadedInfo = null

            jobInfo?.cancel()
            jobSuggestions?.cancel()
            if(userId.isBlank()){
                preview.text = ""
                return
            }

            val isSelectedSuggestion = changedByChoose

            preview.text = "..."
            jobInfo = activity.scope.launch {
                if(!isSelectedSuggestion) delay(300)
                val info = manager.loadInfo(userId)
                if (userId == editable.toString()) {
                    preview.text = info.makeInfoString()
                    lastLoadedInfo = info
                    saveButton.isEnabled = true
                    handleEditor.backgroundTintList = ColorStateList.valueOf(
                        if (info.status == STATUS.OK) successColor
                        else failColor
                    )
                }
            }


            if(isSelectedSuggestion) changedByChoose = false
            else {
                if (userId.length < 3) suggestionsAdapter.clear()
                else {
                    suggestionsAdapter.loading()
                    jobSuggestions = activity.scope.launch {
                        delay(300)
                        val suggestions = manager.loadSuggestions(userId)
                        if (userId == editable.toString()) {
                            suggestionsAdapter.setData(suggestions)
                        }
                    }
                }
            }
        }

    }


    class SuggestionsItemsAdapter(val watcher: UserIDChangeWatcher) : RecyclerView.Adapter<SuggestionsItemsAdapter.ItemHolder>(){

        class ItemHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view){
            val title: TextView = view.findViewById(R.id.suggestions_item_title)
            val info: TextView = view.findViewById(R.id.suggestions_item_info)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ItemHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.account_suggestions_item, parent, false) as ConstraintLayout
            return ItemHolder(view)
        }

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            with(holder){
                val (title, info, userId) = data[position]
                this.title.text = title
                this.info.text = info
                view.setOnClickListener {
                    watcher.selectSuggestion(userId)
                }
            }
        }

        override fun getItemCount(): Int = data.size

        private var data : List<AccountSuggestion> = emptyList()

        fun setData(suggestions: List<AccountSuggestion>?){
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