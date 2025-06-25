package com.demich.cps.features.room

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


inline fun <reified D: RoomDatabase> instanceDelegate(
    name: String,
    crossinline migrations: () -> List<Migration> = { emptyList() }
): ReadOnlyProperty<Context, D> =
    object : ReadOnlyProperty<Context, D> {
        // based on PreferenceDataStoreSingletonDelegate
        private val lock = Any()

        @GuardedBy("lock")
        @Volatile
        private var instance: D? = null

        override fun getValue(thisRef: Context, property: KProperty<*>): D {
            return instance ?: synchronized(lock) {
                if (instance == null) {
                    val builder = Room.databaseBuilder<D>(context = thisRef, name = name)
                        .addMigrations(migrations = migrations().toTypedArray())
                    instance = builder.build()
                }
                requireNotNull(instance)
            }
        }
    }