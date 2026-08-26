package com.example.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SettingEntity::class, WordEntity::class, BigramEntity::class], version = 5, exportSchema = false)
abstract class KeyboardDatabase : RoomDatabase() {
    abstract fun settingDao(): SettingDao
    abstract fun wordDao(): WordDao
}
