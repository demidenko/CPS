package com.example.test3.account_manager

import android.content.Context
import com.example.test3.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class CodeforcesAccountManager(context: Context): AccountManager(context) {

    @Serializable
    data class CodeforcesUserInfo(
        override var status: STATUS,
        var handle: String,
        var rating: Int = NOT_RATED,
        var contribution: Int = 0
    ): UserInfo() {
        override val userID: String
            get() = handle

        override fun makeInfoOKString(): String {
            return if(rating == NOT_RATED) "$handle [not rated]" else "$handle $rating"
        }

        override fun link(): String = CodeforcesURLFactory.user(handle)
    }

    override val PREFERENCES_FILE_NAME: String
        get() = preferences_file_name

    companion object{
        const val preferences_file_name = "codeforces"

        var __cachedInfo: CodeforcesUserInfo? = null

    }

    override fun emptyInfo() = CodeforcesUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String): CodeforcesUserInfo {
        val handle = data
        val res = CodeforcesUserInfo(STATUS.FAILED, handle)
        val response = CodeforcesAPI.getUser(handle) ?: return res
        if(response.status == CodeforcesAPIStatus.FAILED){
            if(response.comment == "handles: User with handle $handle not found") return res.copy( status = STATUS.NOT_FOUND )
            return res
        }
        val info = response.result!!
        return res.copy(
            status = STATUS.OK,
            handle = info.handle,
            rating = info.rating,
            contribution = info.contribution
        )
    }

    override var cachedInfo: UserInfo?
        get() = __cachedInfo
        set(value) { __cachedInfo = value as CodeforcesUserInfo }

    override fun readInfo(): CodeforcesUserInfo = with(prefs){
        val str = getString(preferences_key_user_info, null) ?: return@with emptyInfo().apply { status = STATUS.FAILED }
        jsonCPS.decodeFromString(str)
    }

    override fun writeInfo(info: UserInfo) = with(prefs.edit()){
        info as CodeforcesUserInfo
        putString(preferences_key_user_info, jsonCPS.encodeToString(info))
        commit()
    }

    override fun getColor(info: UserInfo): Int? = with(info as CodeforcesUserInfo){
        if(status != STATUS.OK || rating == NOT_RATED) return null
        return CodeforcesUtils.getHandleColorARGB(info.rating)
    }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion>? = withContext(Dispatchers.IO){
        val s = CodeforcesAPI.getHandleSuggestions(str) ?: return@withContext null
        val res = ArrayList<AccountSuggestion>()
        s.split('\n').filter { !it.contains('=') }.forEach {
            val i = it.indexOf('|')
            if (i != -1) {
                val handle = it.substring(i + 1)
                res += AccountSuggestion(handle, "", handle)
            }
        }
        return@withContext res
    }
}