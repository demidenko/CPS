package com.example.test3.account_manager

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


abstract class AccountManager(val context: Context) {

    companion object {
        const val preferences_key_user_info = "user_info"
    }

    abstract val PREFERENCES_FILE_NAME: String
    val prefs by lazy { context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE) }

    abstract fun emptyInfo(): UserInfo

    protected abstract suspend fun downloadInfo(data: String): UserInfo
    suspend fun loadInfo(data: String): UserInfo {
        if(data.isBlank()) return emptyInfo()
        return withContext(Dispatchers.IO){
            downloadInfo(data)
        }
    }

    open suspend fun loadSuggestions(str: String): List<Triple<String,String,String>>? = null

    protected abstract var cachedInfo: UserInfo?
    protected abstract fun readInfo(): UserInfo
    protected abstract fun writeInfo(info: UserInfo): Boolean

    var savedInfo: UserInfo
        get() = cachedInfo ?: readInfo().also { cachedInfo = it }
        set(info) {
            if(info == cachedInfo) return
            writeInfo(info)
            cachedInfo = info
            println("account $PREFERENCES_FILE_NAME rewrited to ${info.makeInfoString()}")
        }

    open fun getColor(info: UserInfo): Int? = null
}

enum class STATUS{
    OK,
    NOT_FOUND,
    FAILED
}
const val NOT_RATED = Int.MIN_VALUE

abstract class UserInfo{
    abstract val userID: String
    abstract val status: STATUS

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

var useRealColors: Boolean = false
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

    fun getARGB(manager: ColoredHandles): Int {
        return ((if(useRealColors) manager.getColor(this) else rgb) + 0xFF000000).toInt()
    }

    class UnknownHandleColorException(color: HandleColor): Exception("${color.name} is invalid color for manager ")
}

interface ColoredHandles {
    fun getHandleColor(rating: Int): HandleColor
    fun getColor(tag: HandleColor): Int
}

