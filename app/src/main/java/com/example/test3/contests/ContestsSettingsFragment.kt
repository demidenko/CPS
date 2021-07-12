package com.example.test3.contests

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.test3.R
import com.example.test3.ui.CPSDataStoreDelegate
import com.example.test3.ui.CPSFragment
import com.example.test3.utils.CPSDataStore
import kotlinx.coroutines.flow.first

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

    private val KEY_CLIST_API_LOGIN = stringPreferencesKey("clist_api_login")
    private val KEY_CLIST_API_KEY = stringPreferencesKey("clist_api_key")

    suspend fun getClistApiLogin() = dataStore.data.first()[KEY_CLIST_API_LOGIN]
    suspend fun setClistApiLogin(login: String) {
        dataStore.edit { it[KEY_CLIST_API_LOGIN] = login }
    }

    suspend fun getClistApiKey() = dataStore.data.first()[KEY_CLIST_API_KEY]
    suspend fun setClistApiKey(key: String) {
        dataStore.edit { it[KEY_CLIST_API_KEY] = key }
    }

}