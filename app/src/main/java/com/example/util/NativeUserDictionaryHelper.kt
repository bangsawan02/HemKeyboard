package com.example.util

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.provider.UserDictionary
import java.util.Locale

data class NativeUserWord(
    val word: String,
    val frequency: Int,
    val shortcut: String? = null,
    val locale: String? = null
)

/**
 * Native Android SDK UserDictionary.Words helper using ContentResolver.
 * Interacts with system user dictionary for seamless synchronization and system-wide word prediction.
 */
object NativeUserDictionaryHelper {

    fun getSystemUserWords(context: Context): List<NativeUserWord> {
        val result = mutableListOf<NativeUserWord>()
        try {
            val contentResolver: ContentResolver = context.contentResolver
            val projection = arrayOf(
                UserDictionary.Words.WORD,
                UserDictionary.Words.FREQUENCY,
                UserDictionary.Words.SHORTCUT,
                UserDictionary.Words.LOCALE
            )
            val cursor = contentResolver.query(
                UserDictionary.Words.CONTENT_URI,
                projection,
                null,
                null,
                "${UserDictionary.Words.FREQUENCY} DESC"
            )

            cursor?.use {
                val wordCol = it.getColumnIndex(UserDictionary.Words.WORD)
                val freqCol = it.getColumnIndex(UserDictionary.Words.FREQUENCY)
                val shortcutCol = it.getColumnIndex(UserDictionary.Words.SHORTCUT)
                val localeCol = it.getColumnIndex(UserDictionary.Words.LOCALE)

                while (it.moveToNext()) {
                    val word = if (wordCol != -1) it.getString(wordCol) else null
                    val freq = if (freqCol != -1) it.getInt(freqCol) else 1
                    val shortcut = if (shortcutCol != -1) it.getString(shortcutCol) else null
                    val locale = if (localeCol != -1) it.getString(localeCol) else null

                    if (!word.isNullOrBlank()) {
                        result.add(NativeUserWord(word, freq, shortcut, locale))
                    }
                }
            }
        } catch (_: Exception) {
            // Graceful fallback if permission is denied or content provider is unavailable
        }
        return result
    }

    fun addWordToSystemDictionary(
        context: Context,
        word: String,
        frequency: Int = 128,
        shortcut: String? = null,
        locale: Locale = Locale.getDefault()
    ): Boolean {
        if (word.isBlank()) return false
        return try {
            UserDictionary.Words.addWord(
                context,
                word.trim(),
                frequency.coerceIn(1, 255),
                shortcut?.trim()?.ifEmpty { null },
                locale
            )
            true
        } catch (_: Exception) {
            false
        }
    }
}
