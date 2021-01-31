package com.example.test3

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
    private val initialUserInfo: UserInfo,
    private val manager: AccountManager,
    private val cont: Continuation<UserInfo?>
): DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val context = requireContext()
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_choose_userid, null)

        val builder = AlertDialog.Builder(context)
            .setTitle("getUser(${manager.PREFERENCES_FILE_NAME})")
            .setView(view)
            .setPositiveButton("return"){ _, _ ->

            }

        return builder.create().apply {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
    }

    override fun onStart() {
        super.onStart()

        val mainActivity = requireActivity() as MainActivity

        val dialog = getDialog()!! as AlertDialog
        dialog.findViewById<TextView>(resources.getIdentifier("alertTitle", "id", "android")).typeface = Typeface.MONOSPACE

        val input = dialog.findViewById<EditText>(R.id.account_choose_input).apply {
            setText(initialUserInfo.userID)
            typeface = Typeface.MONOSPACE
            filters = arrayOf(createSearchInputFilter(manager))
            requestFocus()
        }

        val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        val userIDChangeWatcher = UserIDChangeWatcher(
            this,
            manager,
            initialUserInfo,
            input,
            saveButton,
            dialog.findViewById(R.id.account_choose_info),
            dialog.findViewById(R.id.account_choose_suggestions)
        )

        saveButton.apply {
            setOnClickListener {
                with(userIDChangeWatcher.lastLoadedInfo){
                    if(userID.any { char -> !manager.isValidForUserID(char) }){
                        mainActivity.showToast("Incorrect characters for userID")
                        return@with
                    }
                    if(status == STATUS.NOT_FOUND){
                        mainActivity.showToast("User not found")
                        return@with
                    }
                    cont.resume(this)
                    dismiss()
                }
            }
        }

        input.addTextChangedListener(userIDChangeWatcher)
    }

    override fun onCancel(dialog: DialogInterface) {
        cont.resume(null)
    }

    class UserIDChangeWatcher(
        val fragment: Fragment,
        val manager: AccountManager,
        var lastLoadedInfo: UserInfo,
        val handleEditor: EditText,
        val saveButton: Button,
        val preview: TextView,
        val suggestionsView: RecyclerView
    ) : TextWatcher {

        private var jobInfo: Job? = null
        private var jobSuggestions: Job? = null

        private val suggestionsAdapter: SuggestionsItemsAdapter

        init {
            preview.text = if(lastLoadedInfo.userID.isEmpty()) ""
            else lastLoadedInfo.makeInfoString()

            suggestionsAdapter = SuggestionsItemsAdapter(this)

            suggestionsView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = suggestionsAdapter
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                setHasFixedSize(true)
                visibility = View.GONE
            }
        }

        private val successLine = ColorStateList.valueOf(getColorFromResource(manager.context, R.color.success))
        private val neutralColor = ColorStateList.valueOf(getColorFromResource(manager.context, R.color.colorAccent))
        private val failLine = ColorStateList.valueOf(getColorFromResource(manager.context, R.color.fail))

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        private var changedByChoose = false

        fun selectSuggestion(userID: String) {
            changedByChoose = true
            handleEditor.setText(userID)
            handleEditor.setSelection(userID.length)
        }

        override fun afterTextChanged(editable: Editable?) {
            val userId = editable?.toString() ?: return

            if(userId.isNotBlank() && userId == lastLoadedInfo.userID) return

            saveButton.isEnabled = false
            lastLoadedInfo = manager.emptyInfo()

            handleEditor.backgroundTintList = neutralColor
            jobInfo?.cancel()
            jobSuggestions?.cancel()
            if(userId.isBlank()){
                preview.text = ""
                return
            }

            val isSelectedSuggestion = changedByChoose

            preview.text = "..."
            jobInfo = fragment.lifecycleScope.launch {
                if(!isSelectedSuggestion) delay(300)
                val info = manager.loadInfo(userId)
                if (userId == editable.toString()) {
                    preview.text = info.makeInfoString()
                    lastLoadedInfo = info
                    saveButton.isEnabled = true
                    handleEditor.backgroundTintList =
                        if (info.status == STATUS.OK) successLine
                        else failLine
                }
            }

            if(isSelectedSuggestion) changedByChoose = false
            else {
                if(manager.isProvidesSuggestions && userId.length >= 3){
                    suggestionsAdapter.loading(suggestionsView)
                    jobSuggestions = fragment.lifecycleScope.launch {
                        delay(300)
                        val suggestions = manager.loadSuggestions(userId)
                        if (userId == editable.toString()) {
                            suggestionsAdapter.setData(suggestions, suggestionsView)
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

        fun setData(suggestions: List<AccountSuggestion>?, view: RecyclerView){
            if(suggestions == null || suggestions.isEmpty()) view.visibility = View.GONE
            data = suggestions ?: emptyList()
            notifyDataSetChanged()
        }

        fun clear(view: RecyclerView){
            view.visibility = View.GONE
            data = emptyList()
            notifyDataSetChanged()
        }

        fun loading(view: RecyclerView){
            clear(view)
            view.visibility = View.VISIBLE
        }

    }

    fun createSearchInputFilter(accountManager: AccountManager): InputFilter {
        return InputFilter { source, start, end, dest, dstart, dend ->
            var keepOriginal = true
            val str = StringBuilder()
            for(i in start until end) {
                val c = source[i]
                if(accountManager.isValidForSearch(c)) str.append(c)
                else keepOriginal = false
            }
            if(keepOriginal) null
            else {
                if(source is Spanned){
                    val span = SpannableString(str)
                    TextUtils.copySpansFrom(source, start, str.length, null, span, 0)
                    span
                }else str
            }
        }
    }
}