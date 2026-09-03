package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT word FROM words WHERE word LIKE :prefix || '%' ORDER BY frequency DESC, timestamp DESC LIMIT :limit")
    suspend fun getPredictions(prefix: String, limit: Int = 3): List<String>

    @Query("SELECT * FROM words ORDER BY frequency DESC, timestamp DESC")
    fun getAllWordsFlow(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words ORDER BY frequency DESC, timestamp DESC")
    suspend fun getAllWords(): List<WordEntity>

    @Query("SELECT word FROM words WHERE word LIKE :pattern ORDER BY frequency DESC, timestamp DESC LIMIT :limit")
    suspend fun getFuzzyPredictions(pattern: String, limit: Int = 10): List<String>

    @Query("SELECT * FROM words WHERE word LIKE :firstChar || '%' ORDER BY frequency DESC, timestamp DESC")
    suspend fun getWordsStartingWith(firstChar: String): List<WordEntity>

    @Query("SELECT * FROM words WHERE isUserCustom = 1 ORDER BY frequency DESC, timestamp DESC")
    fun getUserCustomWordsFlow(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE word LIKE '%' || :query || '%' ORDER BY frequency DESC, timestamp DESC")
    fun searchWordsFlow(query: String): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE word = :word LIMIT 1")
    suspend fun getWord(word: String): WordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Query("UPDATE words SET frequency = frequency + 1, timestamp = :timestamp WHERE word = :word")
    suspend fun incrementFrequency(word: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM words WHERE word = :word")
    suspend fun deleteWord(word: String)

    @Query("DELETE FROM words")
    suspend fun clearDictionary()

    @Query("DELETE FROM words WHERE isUserCustom = 0 AND frequency = 1 AND word NOT IN (SELECT word FROM words ORDER BY frequency DESC, timestamp DESC LIMIT :keepLimit)")
    suspend fun pruneDictionary(keepLimit: Int = 5000)

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int

    @Query("SELECT COUNT(*) FROM words")
    fun getWordCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE isUserCustom = 1")
    fun getCustomWordCountFlow(): Flow<Int>

    // Bigram operations for next word prediction
    @Query("SELECT word2 FROM bigrams WHERE word1 = :word1 ORDER BY frequency DESC, timestamp DESC LIMIT :limit")
    suspend fun getNextWordPredictions(word1: String, limit: Int = 3): List<String>

    @Query("SELECT * FROM bigrams WHERE word1 = :word1 AND word2 = :word2 LIMIT 1")
    suspend fun getBigram(word1: String, word2: String): BigramEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBigram(bigram: BigramEntity)

    @Query("UPDATE bigrams SET frequency = frequency + 1, timestamp = :timestamp WHERE word1 = :word1 AND word2 = :word2")
    suspend fun incrementBigramFrequency(word1: String, word2: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM bigrams WHERE timestamp < :expiryTime")
    suspend fun pruneBigrams(expiryTime: Long)
}

