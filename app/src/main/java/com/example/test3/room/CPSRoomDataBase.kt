package com.example.test3.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LostBlogEntry::class, UserBlogs::class], version = 2)
@TypeConverters(IntsListConverter::class)
abstract class RoomSingleton: RoomDatabase(){
    abstract fun lostBlogsDao(): LostBlogsDao
    abstract fun followListDao(): FollowListDao

    companion object {
        private var instance: RoomSingleton? = null
        fun getInstance(context: Context): RoomSingleton {
            return instance
                ?: Room.databaseBuilder(context, RoomSingleton::class.java, "CPSdb")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
        }

        private val MIGRATION_1_2 by lazy { object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE `cf_follow_list` (`id` INTEGER PRIMARY KEY autoincrement NOT NULL, `handle` TEXT NOT NULL, `blogs` TEXT)")
                }
            }
        }
    }
}