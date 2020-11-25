package com.example.test3

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.test3.account_manager.STATUS
import kotlinx.android.synthetic.main.activity_main.*
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

}