package com.example.test3.account_manager

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


abstract class AccountManager(val activity: AppCompatActivity) {
    abstract val PREFERENCES_FILE_NAME: String
    val prefs = activity.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

    protected abstract suspend fun downloadInfo(data: String): UserInfo
    suspend fun loadInfo(data: String): UserInfo {
        return withContext(Dispatchers.IO){
            downloadInfo(data)
        }
    }

    open suspend fun loadSuggestions(str: String): List<Pair<String,String>>? = null

    protected abstract var cachedInfo: UserInfo?
    protected abstract fun readInfo(): UserInfo
    protected abstract fun writeInfo(info: UserInfo): Boolean

    var savedInfo: UserInfo
        get() = cachedInfo ?: readInfo().also { cachedInfo = it }
        set(info) {
            if(info == cachedInfo) return
            writeInfo(info)
            cachedInfo = info
            println("${PREFERENCES_FILE_NAME} rewrited to ${info.makeInfoString()}")
        }

    open fun getColor(info: UserInfo): Int? = null
}

enum class STATUS{
    OK,
    NOT_FOUND,
    FAILED
}
const val NOT_RATED = Int.MIN_VALUE
const val preferences_status = "preferences_status"

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
}

var useCommonColors: Boolean = true
enum class HandleColor(private val rgb: Int) {
    GRAY(0x999999),
    BROWN(0x804000),
    GREEN(0x009000),
    CYAN(0x03A89E),
    BLUE(0x3E64FF),
    VIOLET(0xAA50C5), //0xB455EE
    YELLOW(0xCCCC00),
    ORANGE(0xFF8C00),
    RED(0xED3B1C);

    fun getARGB(manager: ColoredHandles): Int {
        return ((if(useCommonColors) rgb else manager.getColor(this)) + 0xFF000000).toInt()
    }
}

interface ColoredHandles {
    fun getHandleColor(rating: Int): HandleColor
    fun getColor(tag: HandleColor): Int
}

