package com.demich.cps.profiles

import android.content.Context
import com.demich.cps.platforms.Platform
import com.demich.cps.profiles.managers.profilePlatforms
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper

val Context.settingsProfiles: ProfilesSettingsStorage
    get() = ProfilesSettingsStorage(context = this)

class ProfilesSettingsStorage(context: Context): ItemizedDataStore(context.dataStore) {
    companion object {
        private val Context.dataStore by dataStoreWrapper(name = "profiles_settings")
    }

    val platformsOrder = jsonCPS.itemList<Platform>(name = "platforms_order").mapGetter { order ->
        order.filter { it in profilePlatforms } + profilePlatforms.filter { it !in order }
    }
}