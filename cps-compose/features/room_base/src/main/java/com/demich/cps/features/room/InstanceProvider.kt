package com.demich.cps.features.room

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class InstanceProvider<T: RoomDatabase>(
    private val builder: (Context) ->  RoomDatabase.Builder<T>
) {
    private var instance: T? = null
    fun getInstance(context: Context): T {
        return instance ?: builder(context).build().also { instance = it }
    }
}

inline fun <reified D: RoomDatabase> instanceDelegate(
    name: String,
    crossinline migrations: () -> List<Migration> = { emptyList() }
): ReadOnlyProperty<Context, D> =
    object : ReadOnlyProperty<Context, D> {
        private var instance: D? = null
        override fun getValue(thisRef: Context, property: KProperty<*>): D {
            instance?.let { return it }

            val builder = Room.databaseBuilder<D>(context = thisRef, name = name)
                .addMigrations(migrations = migrations().toTypedArray())

            return builder.build().also { instance = it }
        }
    }