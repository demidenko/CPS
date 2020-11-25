package com.example.test3

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.test3.account_manager.AccountManager
import com.example.test3.account_manager.UserInfo
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class DialogAccountChooser(
    private val initialText: String,
    private val manager: AccountManager,
    private val cont: Continuation<UserInfo?>
): DialogFragment() {

    private lateinit var userIDChangeWatcher: AccountEditFragment.UserIDChangeWatcher

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return requireActivity().let { activity ->
            val view = activity.layoutInflater.inflate(R.layout.dialog_choose_userid, null)

            val builder = AlertDialog.Builder(activity)
                .setTitle("getUserID(${manager.PREFERENCES_FILE_NAME})")
                .setView(view)
                .setPositiveButton("return"){ _, _ ->
                    cont.resume(userIDChangeWatcher.lastLoadedInfo)
                }

            builder.create()
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = getDialog()!! as AlertDialog
        dialog.findViewById<TextView>(resources.getIdentifier("alertTitle", "id", "android")).typeface = Typeface.MONOSPACE

        val input = dialog.findViewById<EditText>(R.id.account_choose_input).apply {
            setText(initialText)
        }

        val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        userIDChangeWatcher = AccountEditFragment.UserIDChangeWatcher(
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
}