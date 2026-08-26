package com.example.database

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "bigrams",
    primaryKeys = ["word1", "word2"],
    indices = [
        Index(value = ["word1"]),
        Index(value = ["frequency"])
    ]
)
data class BigramEntity(
    val word1: String, // The previous word
    val word2: String, // The current word
    val frequency: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)
