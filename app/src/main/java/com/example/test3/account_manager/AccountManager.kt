package com.example.test3.account_manager

import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


abstract class AccountManager(val activity: AppCompatActivity) {
    abstract val PREFERENCES_FILE_NAME: String


    protected abstract suspend fun downloadInfo(data: String): UserInfo
    suspend fun loadInfo(data: String): UserInfo {
        return withContext(Dispatchers.IO){
            downloadInfo(data)
        }
    }

    open suspend fun loadSuggestions(str: String): List<Pair<String,String>>? {
        return null
    }

    protected abstract fun readInfo(): UserInfo
    protected abstract fun writeInfo(info: UserInfo): Boolean
    protected abstract var cachedInfo: UserInfo?

    var savedInfo: UserInfo
        get() = cachedInfo ?: readInfo().also { cachedInfo = it }
        set(info) {
            if(info == cachedInfo) return
            writeInfo(info)
            cachedInfo = info
            println("${PREFERENCES_FILE_NAME} rewrited to ${info.makeInfoString()}")
        }

    abstract fun getColor(info: UserInfo): Int?
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

