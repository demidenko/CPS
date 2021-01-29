package com.example.test3.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [LostBlogEntry::class], version = 1)
@TypeConverters(ColorTagConverter::class)
abstract class RoomSingleton: RoomDatabase(){
    abstract fun lostBlogsDao(): LostBlogsDao

    companion object {
        private var instance: RoomSingleton? = null
        fun getInstance(context: Context): RoomSingleton {
            return instance
                ?: Room.databaseBuilder(context, RoomSingleton::class.java, "CPSdb")
                    .build()
                    .also { instance = it }
        }
    }
}