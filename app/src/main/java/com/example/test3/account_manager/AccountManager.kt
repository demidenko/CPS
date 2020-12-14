package com.example.test3.account_manager

import android.content.Context
import android.text.SpannableString
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import androidx.lifecycle.asLiveData
import com.example.test3.getUseRealColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext


abstract class AccountManager(val context: Context) {

    companion object {
        val KEY_USER_INFO = preferencesKey<String>("user_info")
    }

    abstract val userIDName: String

    abstract val PREFERENCES_FILE_NAME: String
    private val dataStore = context.createDataStore(name = PREFERENCES_FILE_NAME)
    val dataStoreLive = dataStore.data.asLiveData()

    abstract fun emptyInfo(): UserInfo

    protected abstract suspend fun downloadInfo(data: String): UserInfo
    suspend fun loadInfo(data: String): UserInfo {
        if(data.isBlank()) return emptyInfo()
        return withContext(Dispatchers.IO){
            downloadInfo(data)
        }
    }

    open suspend fun loadSuggestions(str: String): List<AccountSuggestion>? = null

    protected abstract fun decodeFromString(str: String): UserInfo
    protected abstract fun encodeToString(info: UserInfo): String

    suspend fun getSavedInfo(): UserInfo {
        val str = dataStore.data.first()[KEY_USER_INFO] ?: return emptyInfo().apply { status = STATUS.FAILED }
        return decodeFromString(str)
    }
    suspend fun setSavedInfo(info: UserInfo) {
        dataStore.edit {
            it[KEY_USER_INFO] = encodeToString(info)
        }
    }

    open fun getColor(info: UserInfo): Int? = null
}


abstract class RatedAccountManager(context: Context) : AccountManager(context){
    abstract fun getColor(handleColor: HandleColor): Int
    abstract val ratingsUpperBounds: Array<Pair<Int, HandleColor>>
    abstract val rankedHandleColorsList: Array<HandleColor>

    fun getHandleColor(rating: Int): HandleColor {
        return ratingsUpperBounds.find { (bound, color) ->
            rating < bound
        }?.second ?: HandleColor.RED
    }

    fun getHandleColorARGB(rating: Int): Int {
        return getHandleColor(rating).getARGB(this)
    }

    abstract fun makeSpan(info: UserInfo): SpannableString

    override val userIDName = "handle"
}

enum class STATUS{
    OK,
    NOT_FOUND,
    FAILED
}
const val NOT_RATED = Int.MIN_VALUE

abstract class UserInfo{
    abstract val userID: String
    abstract var status: STATUS

    protected abstract fun makeInfoOKString(): String
    fun makeInfoString(): String {
        return when(status){
            STATUS.FAILED -> "Error on load: $userID"
            STATUS.NOT_FOUND -> "Not found: $userID"
            else -> makeInfoOKString()
        }
    }

    abstract fun link(): String
}


enum class HandleColor(private val rgb: Int) {
    GRAY(0x888888),
    BROWN(0x80461B),
    GREEN(0x009000),
    CYAN(0x00A89E),
    BLUE(0x3F68F0),
    VIOLET(0xB04ECC),
    YELLOW(0xCCCC00),
    ORANGE(0xFB8000),
    RED(0xED301D);

    companion object {
        val rankedCodeforces    = arrayOf(GRAY, GRAY, GREEN, CYAN, BLUE, VIOLET, VIOLET, ORANGE, ORANGE, RED)
        val rankedAtCoder       = arrayOf(GRAY, BROWN, GREEN, CYAN, BLUE, YELLOW, YELLOW, ORANGE, ORANGE, RED)
        val rankedTopCoder      = arrayOf(GRAY, GRAY, GREEN, GREEN, BLUE, YELLOW, YELLOW, YELLOW, YELLOW, RED)
    }

    fun getARGB(manager: RatedAccountManager, realColor: Boolean): Int {
        return ((if(realColor) manager.getColor(this) else rgb) + 0xFF000000).toInt()
    }

    fun getARGB(manager: RatedAccountManager) = getARGB(manager, manager.context.getUseRealColors())

    class UnknownHandleColorException(color: HandleColor): Exception("${color.name} is invalid color for manager ")
}

data class AccountSuggestion(
    val title: String,
    val info: String,
    val userId: String
)