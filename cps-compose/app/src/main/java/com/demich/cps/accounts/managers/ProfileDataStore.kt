package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.accounts.userinfo.jsonProfile
import com.demich.cps.ui.settings.SettingsContainerScope
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.DataStoreWrapper
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.flow.Flow

abstract class ProfileDataStore<U: UserInfo>(
    dataStoreWrapper: DataStoreWrapper
): ItemizedDataStore(dataStoreWrapper) {
    protected abstract val profileItem: DataStoreItem<ProfileResult<U>?>
    protected inline fun <reified T: UserInfo> makeProfileItem(): DataStoreItem<ProfileResult<T>?> =
        jsonProfile.itemNullable(name = "profile_result")

    protected abstract suspend fun onResetProfile()

    suspend fun getProfile(): ProfileResult<U>? = profileItem()

    suspend fun setProfile(profileResult: ProfileResult<U>) {
        val oldUserId = getProfile()?.userId
        profileItem.setValue(profileResult)
        if (!oldUserId.equals(profileResult.userId, ignoreCase = true)) {
            onResetProfile()
        }
    }

    suspend fun deleteProfile() {
        profileItem.setValue(null)
    }

    fun flowOfProfile() = profileItem.asFlow()
}

abstract class ProfileUniqueDataStore<U: UserInfo>(
    dataStoreWrapper: DataStoreWrapper
): ProfileDataStore<U>(dataStoreWrapper) {
    final override suspend fun onResetProfile() {
        edit { prefs ->
            prefs[profileItem].let {
                prefs.clear()
                prefs[profileItem] = it
            }
        }
    }
}

internal val Context.multipleProfilesDataStoreWrapper by dataStoreWrapper("multiple_profiles")

internal inline fun<reified U: UserInfo> AccountManager<U>.simpleProfileDataStore(context: Context): ProfileDataStore<U> =
    object : ProfileDataStore<U>(context.multipleProfilesDataStoreWrapper) {
        override val profileItem = jsonProfile.itemNullable<ProfileResult<U>>(name = "${type}_profile_result")

        override suspend fun onResetProfile() { }

    }


interface ProfileSettingsProvider {
    fun getSettings(context: Context): ItemizedDataStore

    @Composable
    context(scope: SettingsContainerScope)
    fun SettingsItems() { }

    fun flowOfRequiredNotificationsPermission(context: Context): Flow<Boolean>? = null
}

fun profileDataStoreWrapper(type: AccountManagerType) =
    dataStoreWrapper(name = type.name + "_profile_datastore")

fun profileSettingsDataStoreWrapper(type: AccountManagerType) =
    dataStoreWrapper(name = type.name + "_profile_settings")