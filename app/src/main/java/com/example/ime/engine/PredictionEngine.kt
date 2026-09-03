package com.example.ime.engine

import android.content.Context
import com.example.database.BigramEntity
import com.example.database.KeyboardDatabase
import com.example.database.WordEntity
import com.example.util.NativeUserDictionaryHelper
import kotlinx.coroutines.*

/**
 * Modularized Prediction & Learning Engine for Keyboard IME.
 * Handles Room database queries, next-word prediction, vowel-optional matching,
 * fuzzy prediction, and background pruning.
 */
class PredictionEngine(
    private val context: Context,
    private val database: KeyboardDatabase,
    private val scope: CoroutineScope
) {
    private var predictionJob: Job? = null
    private var wordsTypedSincePrune = 0

    fun updateSuggestions(
        currentWord: String,
        previousWord: String?,
        predictionEnabled: Boolean,
        predictPasswordsEnabled: Boolean,
        isPasswordField: Boolean,
        vowelOptionalEnabled: Boolean,
        guessMissingLettersEnabled: Boolean,
        nextWordPredictionEnabled: Boolean,
        autoCapitalizeNext: Boolean,
        onResults: (List<String>) -> Unit
    ) {
        predictionJob?.cancel()

        if (!predictionEnabled || (isPasswordField && !predictPasswordsEnabled)) {
            onResults(emptyList())
            return
        }

        val prefix = currentWord.trim().lowercase()

        predictionJob = scope.launch(Dispatchers.IO) {
            delay(40)

            var dbPredictions = if (prefix.isNotEmpty()) {
                database.wordDao().getPredictions(prefix, 10).toMutableList()
            } else if (previousWord != null && nextWordPredictionEnabled) {
                database.wordDao().getNextWordPredictions(previousWord, 5).toMutableList()
            } else {
                mutableListOf()
            }

            // Native System UserDictionary fallback
            if (prefix.isNotEmpty() && dbPredictions.size < 5) {
                val systemUserWords = NativeUserDictionaryHelper.getSystemUserWords(context)
                val matchedSystemWords = systemUserWords
                    .filter { it.word.lowercase().startsWith(prefix) }
                    .map { it.word }
                dbPredictions.addAll(matchedSystemWords)
            }

            // Vowel optional matching
            if (prefix.isNotEmpty() && dbPredictions.size < 3 && vowelOptionalEnabled) {
                val prefixNoVowels = prefix.filter { it !in "aeiou" }
                if (prefixNoVowels.isNotEmpty()) {
                    val firstChar = prefix[0].toString()
                    val candidateWords = database.wordDao().getWordsStartingWith(firstChar)
                    val vowelOptionalMatches = candidateWords.filter { wordEntity ->
                        val wordNoVowels = wordEntity.word.filter { it !in "aeiou" }
                        wordNoVowels.startsWith(prefixNoVowels) && !dbPredictions.contains(wordEntity.word)
                    }.take(5).map { it.word }
                    dbPredictions.addAll(vowelOptionalMatches)
                }
            }

            // Fuzzy prediction / missing letters
            if (prefix.length >= 2 && dbPredictions.size < 3 && guessMissingLettersEnabled) {
                val fuzzyPattern = "%" + prefix.toList().joinToString("%") + "%"
                val fuzzyMatches = database.wordDao().getFuzzyPredictions(fuzzyPattern, 10).filter { word ->
                    !dbPredictions.contains(word)
                }.take(5)
                dbPredictions.addAll(fuzzyMatches)
            }

            if (dbPredictions.isEmpty()) {
                withContext(Dispatchers.Main) { onResults(emptyList()) }
                return@launch
            }

            val finalPredictions = dbPredictions.distinct().take(4)
            val isFirstUpper = currentWord.isNotEmpty() && currentWord[0].isUpperCase() || (currentWord.isEmpty() && autoCapitalizeNext)
            val isAllUpper = currentWord.length > 1 && currentWord.all { it.isUpperCase() }

            val formatted = finalPredictions.map { word ->
                when {
                    isAllUpper -> word.uppercase()
                    isFirstUpper -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    else -> word
                }
            }
            withContext(Dispatchers.Main) {
                onResults(formatted)
            }
        }
    }

    fun learnWord(word: String, prevWord: String? = null) {
        val cleanWord = word.trim()
        if (cleanWord.length < 2 || cleanWord.any { !it.isLetterOrDigit() && it != '-' && it != '\'' }) return

        val isNameOrCustom = cleanWord[0].isUpperCase()
        wordsTypedSincePrune++

        scope.launch(Dispatchers.IO) {
            val lowerWord = cleanWord.lowercase()
            val lowerPrevWord = prevWord?.trim()?.lowercase()

            val existing = database.wordDao().getWord(lowerWord)
            if (existing != null) {
                database.wordDao().incrementFrequency(lowerWord)
            } else {
                database.wordDao().insertWord(
                    WordEntity(
                        word = lowerWord,
                        frequency = if (isNameOrCustom) 2 else 1,
                        isUserCustom = isNameOrCustom,
                        timestamp = System.currentTimeMillis()
                    )
                )
                if (isNameOrCustom) {
                    NativeUserDictionaryHelper.addWordToSystemDictionary(context, cleanWord)
                }
            }

            if (!lowerPrevWord.isNullOrEmpty() && lowerPrevWord != lowerWord) {
                val existingBigram = database.wordDao().getBigram(lowerPrevWord, lowerWord)
                if (existingBigram != null) {
                    database.wordDao().incrementBigramFrequency(lowerPrevWord, lowerWord)
                } else {
                    database.wordDao().insertBigram(
                        BigramEntity(
                            word1 = lowerPrevWord,
                            word2 = lowerWord,
                            frequency = 1,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }

            if (wordsTypedSincePrune >= 50) {
                database.wordDao().pruneDictionary(5000)
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                database.wordDao().pruneBigrams(thirtyDaysAgo)
                wordsTypedSincePrune = 0
            }
        }
    }

    fun cancelJob() {
        predictionJob?.cancel()
    }
}
