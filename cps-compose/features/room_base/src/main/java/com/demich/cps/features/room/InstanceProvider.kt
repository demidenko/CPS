package com.demich.cps.features.room

import android.content.Context
import androidx.room.RoomDatabase

abstract class InstanceProvider<T: RoomDatabase>(
    private val builder: (Context) ->  RoomDatabase.Builder<T>
) {
    private var instance: T? = null
    fun getInstance(context: Context): T {
        return instance ?: builder(context).build().also { instance = it }
    }
}