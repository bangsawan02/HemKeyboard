package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SettingDao {
    @Query("SELECT value FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: SettingEntity)
}
