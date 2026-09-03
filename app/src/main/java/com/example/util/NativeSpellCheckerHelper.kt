package com.example.util

import android.content.Context
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import java.util.Locale

/**
 * Native Android SDK TextServicesManager & SpellCheckerSession helper.
 * Provides system-level spell checking and word suggestions.
 */
class NativeSpellCheckerHelper(
    private val context: Context,
    private val onSuggestionsReceived: (String, List<String>) -> Unit
) : SpellCheckerSession.SpellCheckerSessionListener {

    private var spellCheckerSession: SpellCheckerSession? = null

    fun initialize(locale: Locale = Locale.getDefault()) {
        try {
            val textServicesManager = context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as? TextServicesManager
            spellCheckerSession?.close()
            spellCheckerSession = textServicesManager?.newSpellCheckerSession(null, locale, this, false)
        } catch (_: Exception) {
            spellCheckerSession = null
        }
    }

    fun checkSpelling(word: String) {
        if (word.isBlank() || spellCheckerSession == null) return
        try {
            val textInfo = TextInfo(word)
            spellCheckerSession?.getSuggestions(textInfo, 5)
        } catch (_: Exception) {
            // Graceful fallback
        }
    }

    override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
        if (results == null || results.isEmpty()) return
        val suggestionsList = mutableListOf<String>()
        for (info in results) {
            val count = info.suggestionsCount
            for (i in 0 until count) {
                val suggestion = info.getSuggestionAt(i)
                if (!suggestion.isNullOrBlank() && !suggestionsList.contains(suggestion)) {
                    suggestionsList.add(suggestion)
                }
            }
        }
        if (suggestionsList.isNotEmpty()) {
            onSuggestionsReceived("", suggestionsList)
        }
    }

    override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
        if (results == null) return
        val suggestionsList = mutableListOf<String>()
        for (sentenceInfo in results) {
            for (i in 0 until sentenceInfo.suggestionsCount) {
                val suggestionsInfo = sentenceInfo.getSuggestionsInfoAt(i)
                for (j in 0 until suggestionsInfo.suggestionsCount) {
                    val s = suggestionsInfo.getSuggestionAt(j)
                    if (!s.isNullOrBlank() && !suggestionsList.contains(s)) {
                        suggestionsList.add(s)
                    }
                }
            }
        }
        if (suggestionsList.isNotEmpty()) {
            onSuggestionsReceived("", suggestionsList)
        }
    }

    fun close() {
        try {
            spellCheckerSession?.close()
            spellCheckerSession = null
        } catch (_: Exception) {}
    }
}
