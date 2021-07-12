package com.example.test3.contests

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.test3.R
import com.example.test3.makeIntentOpenUrl
import com.example.test3.ui.CPSDataStoreDelegate
import com.example.test3.ui.CPSFragment
import com.example.test3.utils.CPSDataStore
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ContestsSettingsFragment: CPSFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_contests_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cpsTitle = "::contests.settings"
        setHasOptionsMenu(true)

        view.findViewById<ConstraintLayout>(R.id.contests_settings_clistid).apply {
            setOnClickListener {
                val dialogView = mainActivity.layoutInflater.inflate(R.layout.dialog_clist_api, null)
                val context = requireContext()
                dialogView.findViewById<ImageButton>(R.id.dialog_clist_api_info).setOnClickListener {
                    startActivity(makeIntentOpenUrl("https://clist.by/api/v2/doc/"))
                }
                val loginTextField = dialogView.findViewById<TextInputLayout>(R.id.dialog_clist_api_login).apply {
                    editText?.setText(runBlocking { context.settingsContests.getClistApiLogin() ?: "" })
                }
                val apikeyTextField = dialogView.findViewById<TextInputLayout>(R.id.dialog_clist_api_key).apply {
                    editText?.setText(runBlocking { context.settingsContests.getClistApiKey() ?: "" })
                }
                AlertDialog.Builder(mainActivity).apply {
                    setView(dialogView)
                    setPositiveButton("Save") { _, i ->
                        runBlocking {
                            context.settingsContests.setClistApiLogin(loginTextField.editText?.text?.toString())
                            context.settingsContests.setClistApiKey(apikeyTextField.editText?.text?.toString())
                        }
                    }
                }.create().show()
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
        super.onPrepareOptionsMenu(menu)
    }

}

val Context.settingsContests by CPSDataStoreDelegate { ContestsSettingsDataStore(it) }

class ContestsSettingsDataStore(context: Context): CPSDataStore(context.contests_settings_dataStore) {

    companion object {
        private val Context.contests_settings_dataStore by preferencesDataStore("contests_settings")
    }

    private val KEY_CLIST_API_LOGIN get() = stringPreferencesKey("clist_api_login")
    private val KEY_CLIST_API_KEY get() = stringPreferencesKey("clist_api_key")

    suspend fun getClistApiLogin() = dataStore.data.first()[KEY_CLIST_API_LOGIN]
    suspend fun setClistApiLogin(login: String?) {
        dataStore.edit {
            if (login == null || login.isBlank()) it.remove(KEY_CLIST_API_LOGIN)
            else it[KEY_CLIST_API_LOGIN] = login
        }
    }

    suspend fun getClistApiKey() = dataStore.data.first()[KEY_CLIST_API_KEY]
    suspend fun setClistApiKey(key: String?) {
        dataStore.edit {
            if (key == null || key.isBlank()) it.remove(KEY_CLIST_API_KEY)
            else it[KEY_CLIST_API_KEY] = key
        }
    }

}