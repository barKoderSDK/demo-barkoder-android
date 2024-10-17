package com.barkoder.demoscanner.models

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.barkoder.demoscanner.utils.BitmapConverter

@Database(entities = [RecentScan2::class], version = 12, exportSchema = false)
@TypeConverters(BitmapConverter::class)
abstract class RecentDatabase : RoomDatabase() {

    abstract fun recentDaio(): RecentDao

    companion object {
        @Volatile
        private var INSTANCE: RecentDatabase? = null

        fun getDatabase(context: Context): RecentDatabase {
            val tempInstace = INSTANCE
            if (tempInstace != null) {
                return tempInstace
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RecentDatabase::class.java,
                    "recent_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                return instance
            }
        }
    }
}