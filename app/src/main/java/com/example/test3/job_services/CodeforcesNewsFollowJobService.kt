package com.example.test3.job_services

import android.content.Context
import androidx.preference.PreferenceManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class CodeforcesNewsFollowJobService {
    companion object {

        private const val CF_FOLLOW_HANDLES = "cf_follow_handles"
        private val adapter = Moshi.Builder().build().adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))

        fun saveHandles(context: Context, handles: List<String>) {
            with(PreferenceManager.getDefaultSharedPreferences(context).edit()){
                val str = adapter.toJson(handles)
                putString(CF_FOLLOW_HANDLES, str)
                commit()
            }
        }

        fun getSavedHandles(context: Context): List<String> {
            val str = PreferenceManager.getDefaultSharedPreferences(context).getString(CF_FOLLOW_HANDLES, null) ?: return emptyList()
            return adapter.fromJson(str) ?: emptyList()
        }
    }
}