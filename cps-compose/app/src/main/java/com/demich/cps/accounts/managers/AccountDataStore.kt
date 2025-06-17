package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.accounts.userinfo.asResult
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.DataStoreWrapper
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

abstract class AccountDataStore<U: UserInfo>(
    dataStoreWrapper: DataStoreWrapper
): ItemizedDataStore(dataStoreWrapper) {
    protected abstract val userInfo: DataStoreItem<U?>
    protected inline fun <reified T: UserInfo> makeUserInfoItem(): DataStoreItem<T?> =
        jsonCPS.item(name = "user_info", defaultValue = null)

    abstract suspend fun onResetProfile()

    fun flowOfProfile() = userInfo.flow.map { it?.asResult() }

    protected abstract fun ProfileResult<U>.convert(): U
    suspend fun getProfile(): ProfileResult<U>? = userInfo()?.asResult()

    suspend fun setProfile(profileResult: ProfileResult<U>) {
        val oldUserId = getProfile()?.userId
        userInfo(newValue = profileResult.convert())
        if (!oldUserId.equals(profileResult.userId, ignoreCase = true)) {
            onResetProfile()
        }
    }

    suspend fun deleteProfile() {
        userInfo(newValue = null)
    }
}

abstract class AccountUniqueDataStore<U: UserInfo>(
    dataStoreWrapper: DataStoreWrapper
): AccountDataStore<U>(dataStoreWrapper) {
    final override suspend fun onResetProfile() {
        edit { prefs ->
            prefs[userInfo].let {
                prefs.clear()
                prefs[userInfo] = it
            }
        }
    }
}

internal val Context.multipleProfilesDataStoreWrapper by dataStoreWrapper("multiple_profiles")

internal inline fun<reified U: UserInfo> AccountManager<U>.simpleAccountDataStore(context: Context): AccountDataStore<U> =
    object : AccountDataStore<U>(context.multipleProfilesDataStoreWrapper) {
        override val userInfo = jsonCPS.item<U?>(
            name = "${type}_user_info",
            defaultValue = null
        )

        override suspend fun onResetProfile() { }

        override fun ProfileResult<U>.convert(): U =
            this@simpleAccountDataStore.convert(this)
    }


interface AccountSettingsProvider {
    fun getSettings(context: Context): ItemizedDataStore

    @Composable
    fun SettingsItems() {}

    fun flowOfRequiredNotificationsPermission(context: Context): Flow<Boolean>? = null
}