package com.demich.cps.profiles.managers

import android.content.Context
import androidx.compose.runtime.Composable
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.UserInfo
import com.demich.cps.profiles.userinfo.jsonProfile
import com.demich.cps.ui.settings.SettingsContainerScope
import com.demich.datastore_itemized.DataStoreEditScope
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.DataStoreValue
import com.demich.datastore_itemized.DataStoreWrapper
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.edit
import com.demich.datastore_itemized.value
import kotlinx.coroutines.flow.Flow

abstract class ProfileStorage<U: UserInfo>(
    dataStoreWrapper: DataStoreWrapper
): ItemizedDataStore(dataStoreWrapper) {
    protected abstract val profileItem: DataStoreItem<ProfileResult<U>?>
    protected inline fun <reified T: UserInfo> makeProfileItem(): DataStoreItem<ProfileResult<T>?> =
        jsonProfile.itemNullable(name = "profile_result")

    context(scope: DataStoreEditScope)
    protected abstract fun onResetProfile()

    val profile: DataStoreValue<ProfileResult<U>?>
        get() = profileItem

    suspend fun setProfile(profileResult: ProfileResult<U>) {
        edit {
            val oldUserId = profileItem.value?.userId
            if (!oldUserId.equals(profileResult.userId, ignoreCase = true)) {
                onResetProfile()
            }
            profileItem.value = profileResult
        }
    }

    suspend fun deleteProfile() {
        profileItem.setValue(null)
    }
}

abstract class ProfileUniqueStorage<U: UserInfo>(
    dataStoreWrapper: DataStoreWrapper
): ProfileStorage<U>(dataStoreWrapper) {
    context(scope: DataStoreEditScope)
    final override fun onResetProfile() {
        scope.clear()
    }
}

internal val Context.multipleProfilesDataStoreWrapper by dataStoreWrapper("multiple_profiles")

internal inline fun <reified U: UserInfo> ProfileManager<U>.simpleProfileStorage(context: Context): ProfileStorage<U> =
    object : ProfileStorage<U>(context.multipleProfilesDataStoreWrapper) {
        override val profileItem = jsonProfile.itemNullable<ProfileResult<U>>(name = "${platform}_profile_result")

        context(scope: DataStoreEditScope)
        override fun onResetProfile() { }
    }


interface ProfileSettingsProvider {
    fun settingsStorage(context: Context): ItemizedDataStore

    @Composable
    context(scope: SettingsContainerScope)
    fun SettingsItems() { }

    fun flowOfRequiredNotificationsPermission(context: Context): Flow<Boolean>? = null
}

fun profileDataStoreWrapper(platform: ProfilePlatform) =
    dataStoreWrapper(name = platform.name + "_profile_datastore")

fun profileSettingsDataStoreWrapper(platform: ProfilePlatform) =
    dataStoreWrapper(name = platform.name + "_profile_settings")