package com.example.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [
        Index(value = ["word"]),
        Index(value = ["frequency", "timestamp"]),
        Index(value = ["isUserCustom"])
    ]
)
data class WordEntity(
    @PrimaryKey val word: String,
    val frequency: Int = 1,
    val isUserCustom: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
