package com.example.test3.account_manager

import android.content.Context
import com.example.test3.utils.CodeforcesAPI
import com.example.test3.utils.CodeforcesAPIStatus
import com.example.test3.utils.CodeforcesLinkFactory
import com.example.test3.utils.CodeforcesUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CodeforcesAccountManager(context: Context): AccountManager(context) {

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

        override fun link(): String = CodeforcesLinkFactory.user(handle)
    }

    override val PREFERENCES_FILE_NAME: String
        get() = preferences_file_name

    companion object{
        const val preferences_file_name = "codeforces"
        const val preferences_handle = "handle"
        const val preferences_rating = "rating"
        const val preferences_contribution = "contribution"

        var __cachedInfo: CodeforcesUserInfo? = null

    }

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
        CodeforcesUserInfo(
            STATUS.valueOf(getString(preferences_status, null) ?: STATUS.FAILED.name),
            handle = getString(preferences_handle, null) ?: "",
            rating = getInt(preferences_rating, NOT_RATED),
            contribution = getInt(preferences_contribution, 0)
        )
    }

    override fun writeInfo(info: UserInfo) = with(prefs.edit()){
        putString(preferences_status, info.status.name)
        info as CodeforcesUserInfo
        putString(preferences_handle, info.handle)
        putInt(preferences_rating, info.rating)
        putInt(preferences_contribution, info.contribution)
        commit()
    }

    override fun getColor(info: UserInfo): Int? = with(info as CodeforcesUserInfo){
        if(status != STATUS.OK || rating == NOT_RATED) return null
        return CodeforcesUtils.getHandleColor(info.rating).getARGB(CodeforcesUtils)
    }

    override suspend fun loadSuggestions(str: String): List<Pair<String, String>>? = withContext(Dispatchers.IO){
        val s = CodeforcesAPI.getHandleSuggestions(str) ?: return@withContext null
        val res = ArrayList<Pair<String, String>>()
        s.split('\n').filter { !it.contains('=') }.forEach {
            val i = it.indexOf('|')
            if (i != -1) {
                val handle = it.substring(i + 1)
                res += Pair(handle, handle)
            }
        }
        return@withContext res
    }
}