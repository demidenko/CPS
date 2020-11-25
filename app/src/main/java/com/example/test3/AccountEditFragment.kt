package com.example.test3

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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

    val panel: AccountPanel by lazy {
        val managerType = arguments?.getString("manager") ?: throw Exception("Unset type of manager")
        (requireActivity() as MainActivity).accountsFragment.getPanel(managerType)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity

        val manager = panel.manager
        val currentUserID = manager.savedInfo.userID

        activity.setActionBarSubTitle("::accounts.${manager.PREFERENCES_FILE_NAME}.edit")
        activity.navigation.visibility = View.GONE

        val textView: TextView = view.findViewById(R.id.account_edit_userid)

        textView.setOnClickListener {
            activity.scope.launch {
                val userInfo = activity.chooseUserID(manager)
                if(userInfo==null){
                    if(currentUserID.isEmpty()) close()
                }else{
                    manager.savedInfo = userInfo
                    textView.text = userInfo.userID
                    panel.show()
                }
            }
        }

        val linkButton: ImageButton = view.findViewById<ImageButton>(R.id.account_panel_link_button).apply {
            setOnClickListener {
                val info = manager.savedInfo
                if(info.status == STATUS.OK){
                    activity.startActivity(makeIntentOpenUrl(info.link()))
                }
            }
        }

        setHasOptionsMenu(true)

        textView.text = currentUserID
        if(currentUserID.isEmpty()) textView.callOnClick()

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            menu.setGroupDividerEnabled(true)
        }
        inflater.inflate(R.menu.menu_account_view, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.account_delete_button -> {
                AlertDialog.Builder(requireActivity())
                    .setMessage("Delete ${panel.manager.PREFERENCES_FILE_NAME} account?")
                    .setPositiveButton("YES"){ _, _ ->
                        panel.manager.savedInfo = panel.manager.emptyInfo()
                        panel.show()
                        close()
                    }
                    .setNegativeButton("NO"){ _, _ ->

                    }
                    .create()
                    .show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun close(){
        with(requireActivity() as MainActivity){
            onBackPressed()
        }
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

        private val successColor = fragment.resources.getColor(R.color.success, null)
        private val failColor = fragment.resources.getColor(R.color.fail, null)

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        var changedByChoose = false
        override fun afterTextChanged(editable: Editable?) {
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

            preview.text = "..."
            jobInfo = activity.scope.launch {
                delay(300)
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


            if(changedByChoose) changedByChoose = false
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

            fun setData(suggestions: List<Triple<String,String,String>>?){
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