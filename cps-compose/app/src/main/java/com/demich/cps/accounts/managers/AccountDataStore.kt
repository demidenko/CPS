package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.DataStoreWrapper
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.flow.Flow

abstract class AccountDataStore<U: UserInfo>(
    dataStoreWrapper: DataStoreWrapper
): ItemizedDataStore(dataStoreWrapper) {
    protected abstract val userInfo: DataStoreItem<U?>
    protected inline fun<reified T: UserInfo> makeUserInfoItem(): DataStoreItem<T?> =
        jsonCPS.item(name = "user_info", defaultValue = null)

    abstract suspend fun onResetUserInfo()

    fun flowOfInfo() = userInfo.flow

    suspend fun getSavedInfo(): U? = userInfo()

    suspend fun setSavedInfo(info: U) {
        val oldUserId = getSavedInfo()?.userId
        userInfo(info)
        if (!oldUserId.equals(info.userId, ignoreCase = true)) {
            onResetUserInfo()
        }
    }

    suspend fun deleteSavedInfo() {
        userInfo(null)
    }
}

abstract class AccountUniqueDataStore<U: UserInfo>(
    dataStoreWrapper: DataStoreWrapper
): AccountDataStore<U>(dataStoreWrapper) {
    final override suspend fun onResetUserInfo() {
        edit { prefs ->
            prefs[userInfo].let {
                prefs.clear()
                prefs[userInfo] = it
            }
        }
    }
}

internal val Context.multipleUserInfoDataStoreWrapper by dataStoreWrapper("users_info")

internal inline fun<reified U: UserInfo> AccountManager<U>.simpleAccountDataStore(context: Context): AccountDataStore<U> =
    object : AccountDataStore<U>(context.multipleUserInfoDataStoreWrapper) {
        override val userInfo = jsonCPS.item<U?>(
            name = "${type}_user_info",
            defaultValue = null
        )

        override suspend fun onResetUserInfo() = Unit
    }


interface AccountSettingsProvider {
    fun getSettings(context: Context): ItemizedDataStore

    @Composable
    fun SettingsItems() {}

    fun flowOfRequiredNotificationsPermission(context: Context): Flow<Boolean>? = null
}