package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.DatabaseProvider
import com.example.database.SettingEntity
import com.example.database.WordEntity
import com.example.ui.theme.KeyShapeStyle
import com.example.ui.theme.KeyboardHeightStyle
import com.example.ui.theme.KeyboardThemeStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DictionaryFilterType(val title: String) {
    ALL("Semua Kata"),
    USER_CUSTOM("Kustom & Nama"),
    FREQUENT("Sering Digunakan")
}

class KeyboardSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = DatabaseProvider.getDatabase(context)

    private val _activeTheme = MutableStateFlow(KeyboardThemeStyle.LIGHT)
    val activeTheme: StateFlow<KeyboardThemeStyle> = _activeTheme

    private val _heightStyle = MutableStateFlow(KeyboardHeightStyle.NORMAL)
    val heightStyle: StateFlow<KeyboardHeightStyle> = _heightStyle

    private val _shapeStyle = MutableStateFlow(KeyShapeStyle.ROUNDED)
    val shapeStyle: StateFlow<KeyShapeStyle> = _shapeStyle

    private val _autocorrectEnabled = MutableStateFlow(true)
    val autocorrectEnabled: StateFlow<Boolean> = _autocorrectEnabled

    private val _predictionEnabled = MutableStateFlow(true)
    val predictionEnabled: StateFlow<Boolean> = _predictionEnabled

    private val _hapticEnabled = MutableStateFlow(true)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled

    private val _codingBarEnabled = MutableStateFlow(true)
    val codingBarEnabled: StateFlow<Boolean> = _codingBarEnabled

    private val _cursorArrowsEnabled = MutableStateFlow(true)
    val cursorArrowsEnabled: StateFlow<Boolean> = _cursorArrowsEnabled

    private val _codeSnippetsEnabled = MutableStateFlow(true)
    val codeSnippetsEnabled: StateFlow<Boolean> = _codeSnippetsEnabled

    private val _tabUsesSpaces = MutableStateFlow(true)
    val tabUsesSpaces: StateFlow<Boolean> = _tabUsesSpaces

    private val _vowelOptionalEnabled = MutableStateFlow(false)
    val vowelOptionalEnabled: StateFlow<Boolean> = _vowelOptionalEnabled

    private val _guessMissingLettersEnabled = MutableStateFlow(false)
    val guessMissingLettersEnabled: StateFlow<Boolean> = _guessMissingLettersEnabled

    private val _mistypeTolerance = MutableStateFlow(20) // 20% by default
    val mistypeTolerance: StateFlow<Int> = _mistypeTolerance

    private val _nextWordPredictionEnabled = MutableStateFlow(true)
    val nextWordPredictionEnabled: StateFlow<Boolean> = _nextWordPredictionEnabled

    private val _alwaysPredictEnabled = MutableStateFlow(false)
    val alwaysPredictEnabled: StateFlow<Boolean> = _alwaysPredictEnabled

    private val _predictPasswordsEnabled = MutableStateFlow(false)
    val predictPasswordsEnabled: StateFlow<Boolean> = _predictPasswordsEnabled

    private val _showPasswordEnabled = MutableStateFlow(false)
    val showPasswordEnabled: StateFlow<Boolean> = _showPasswordEnabled

    private val _suggestionBarActionsEnabled = MutableStateFlow(true)
    val suggestionBarActionsEnabled: StateFlow<Boolean> = _suggestionBarActionsEnabled

    val isKeyboardEnabled = mutableStateOf(false)
    val isKeyboardSelected = mutableStateOf(false)

    // User Dictionary State & Flows
    val wordCount: StateFlow<Int> = database.wordDao().getWordCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val customWordCount: StateFlow<Int> = database.wordDao().getCustomWordCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filterType = MutableStateFlow(DictionaryFilterType.ALL)
    val filterType: StateFlow<DictionaryFilterType> = _filterType

    val dictionaryWords: StateFlow<List<WordEntity>> = combine(
        database.wordDao().getAllWordsFlow(),
        _searchQuery,
        _filterType
    ) { words, query, filter ->
        var list = words
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter { it.word.lowercase().contains(q) }
        }
        when (filter) {
            DictionaryFilterType.ALL -> list
            DictionaryFilterType.USER_CUSTOM -> list.filter { it.isUserCustom }
            DictionaryFilterType.FREQUENT -> list.filter { it.frequency >= 5 }.sortedByDescending { it.frequency }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadSettings()
        checkKeyboardStatus()
    }

    fun loadSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            val themeVal = database.settingDao().getSetting("keyboard_theme") ?: KeyboardThemeStyle.LIGHT.name
            val heightVal = database.settingDao().getSetting("keyboard_height") ?: KeyboardHeightStyle.NORMAL.name
            val shapeVal = database.settingDao().getSetting("key_shape") ?: KeyShapeStyle.ROUNDED.name
            val autoCorrectVal = database.settingDao().getSetting("autocorrect_enabled") ?: "true"
            val predictionVal = database.settingDao().getSetting("prediction_enabled") ?: "true"
            val hapticVal = database.settingDao().getSetting("haptic_enabled") ?: "true"
            val codingBarVal = database.settingDao().getSetting("coding_bar_enabled") ?: "true"
            val cursorArrowsVal = database.settingDao().getSetting("cursor_arrows_enabled") ?: "true"
            val codeSnippetsVal = database.settingDao().getSetting("code_snippets_enabled") ?: "true"
            val tabSpacesVal = database.settingDao().getSetting("tab_uses_spaces") ?: "true"
            val vowelOptionalVal = database.settingDao().getSetting("vowel_optional_enabled") ?: "false"
            val guessMissingVal = database.settingDao().getSetting("guess_missing_letters_enabled") ?: "false"
            val mistypeToleranceVal = database.settingDao().getSetting("mistype_tolerance") ?: "20"
            val nextWordVal = database.settingDao().getSetting("next_word_enabled") ?: "true"
            val alwaysPredictVal = database.settingDao().getSetting("always_predict_enabled") ?: "false"
            val predictPassVal = database.settingDao().getSetting("predict_passwords_enabled") ?: "false"
            val showPassVal = database.settingDao().getSetting("show_password_enabled") ?: "false"
            val barActionsVal = database.settingDao().getSetting("suggestion_bar_actions_enabled") ?: "true"

            withContext(Dispatchers.Main) {
                _activeTheme.value = try {
                    KeyboardThemeStyle.valueOf(themeVal)
                } catch (e: Exception) {
                    KeyboardThemeStyle.LIGHT
                }
                _heightStyle.value = try {
                    KeyboardHeightStyle.valueOf(heightVal)
                } catch (e: Exception) {
                    KeyboardHeightStyle.NORMAL
                }
                _shapeStyle.value = try {
                    KeyShapeStyle.valueOf(shapeVal)
                } catch (e: Exception) {
                    KeyShapeStyle.ROUNDED
                }
                _autocorrectEnabled.value = autoCorrectVal.toBoolean()
                _predictionEnabled.value = predictionVal.toBoolean()
                _hapticEnabled.value = hapticVal.toBoolean()
                _codingBarEnabled.value = codingBarVal.toBoolean()
                _cursorArrowsEnabled.value = cursorArrowsVal.toBoolean()
                _codeSnippetsEnabled.value = codeSnippetsVal.toBoolean()
                _tabUsesSpaces.value = tabSpacesVal.toBoolean()
                _vowelOptionalEnabled.value = vowelOptionalVal.toBoolean()
                _guessMissingLettersEnabled.value = guessMissingVal.toBoolean()
                _mistypeTolerance.value = mistypeToleranceVal.toIntOrNull() ?: 20
                _nextWordPredictionEnabled.value = nextWordVal.toBoolean()
                _alwaysPredictEnabled.value = alwaysPredictVal.toBoolean()
                _predictPasswordsEnabled.value = predictPassVal.toBoolean()
                _showPasswordEnabled.value = showPassVal.toBoolean()
                _suggestionBarActionsEnabled.value = barActionsVal.toBoolean()
            }
        }
    }

    fun checkKeyboardStatus() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledList = imm.enabledInputMethodList
        val isEnabled = enabledList.any { it.packageName == context.packageName }

        val currentInputMethodId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )
        val isSelected = currentInputMethodId != null && currentInputMethodId.startsWith(context.packageName)

        isKeyboardEnabled.value = isEnabled
        isKeyboardSelected.value = isSelected
    }

    fun updateTheme(theme: KeyboardThemeStyle) {
        _activeTheme.value = theme
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("keyboard_theme", theme.name))
        }
    }

    fun updateHeight(height: KeyboardHeightStyle) {
        _heightStyle.value = height
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("keyboard_height", height.name))
        }
    }

    fun updateShape(shape: KeyShapeStyle) {
        _shapeStyle.value = shape
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("key_shape", shape.name))
        }
    }

    fun updateAutocorrect(enabled: Boolean) {
        _autocorrectEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("autocorrect_enabled", enabled.toString()))
        }
    }

    fun updatePrediction(enabled: Boolean) {
        _predictionEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("prediction_enabled", enabled.toString()))
        }
    }

    fun updateHaptic(enabled: Boolean) {
        _hapticEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("haptic_enabled", enabled.toString()))
        }
    }

    fun updateCodingBar(enabled: Boolean) {
        _codingBarEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("coding_bar_enabled", enabled.toString()))
        }
    }

    fun updateCursorArrows(enabled: Boolean) {
        _cursorArrowsEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("cursor_arrows_enabled", enabled.toString()))
        }
    }

    fun updateCodeSnippets(enabled: Boolean) {
        _codeSnippetsEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("code_snippets_enabled", enabled.toString()))
        }
    }

    fun updateTabUsesSpaces(usesSpaces: Boolean) {
        _tabUsesSpaces.value = usesSpaces
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("tab_uses_spaces", usesSpaces.toString()))
        }
    }

    fun updateVowelOptional(enabled: Boolean) {
        _vowelOptionalEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("vowel_optional_enabled", enabled.toString()))
        }
    }

    fun updateGuessMissingLetters(enabled: Boolean) {
        _guessMissingLettersEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("guess_missing_letters_enabled", enabled.toString()))
        }
    }

    fun updateMistypeTolerance(tolerance: Int) {
        _mistypeTolerance.value = tolerance
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("mistype_tolerance", tolerance.toString()))
        }
    }

    fun updateNextWordPrediction(enabled: Boolean) {
        _nextWordPredictionEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("next_word_enabled", enabled.toString()))
        }
    }

    fun updateAlwaysPredict(enabled: Boolean) {
        _alwaysPredictEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("always_predict_enabled", enabled.toString()))
        }
    }

    fun updatePredictPasswords(enabled: Boolean) {
        _predictPasswordsEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("predict_passwords_enabled", enabled.toString()))
        }
    }

    fun updateShowPassword(enabled: Boolean) {
        _showPasswordEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("show_password_enabled", enabled.toString()))
        }
    }

    fun updateSuggestionBarActions(enabled: Boolean) {
        _suggestionBarActionsEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            database.settingDao().saveSetting(SettingEntity("suggestion_bar_actions_enabled", enabled.toString()))
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilterType(filter: DictionaryFilterType) {
        _filterType.value = filter
    }

    fun addUserCustomWord(word: String, frequency: Int = 10): Boolean {
        val cleanWord = word.trim()
        if (cleanWord.isBlank()) return false
        viewModelScope.launch(Dispatchers.IO) {
            database.wordDao().insertWord(
                WordEntity(
                    word = cleanWord,
                    frequency = frequency.coerceAtLeast(1),
                    isUserCustom = true,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        return true
    }

    fun deleteWord(word: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.wordDao().deleteWord(word)
        }
    }

    fun clearDictionary() {
        viewModelScope.launch(Dispatchers.IO) {
            database.wordDao().clearDictionary()
            
            // Re-populate basic defaults
            val initialWords = listOf(
                "saya", "yang", "dan", "untuk", "dengan", "kamu", "dia", "kita", "mereka",
                "bisa", "ada", "dari", "akan", "dalam", "bukan", "sudah", "belum", "sangat", "ialah",
                "adalah", "karena", "tetapi", "bahwa", "seperti", "kalau", "jika", "maka", "pada", "ke",
                "tentang", "banyak", "sedikit", "semua", "beberapa", "tahun", "hari", "waktu", "buku", "rumah",
                "jalan", "sekolah", "anak", "orang", "kerja", "makan", "minum", "tidur", "pergi", "datang",
                "baru", "lama", "besar", "kecil", "bagus", "jelek", "baik", "buruk", "sehat", "sakit",
                "nama", "apa", "siapa", "mengapa", "bagaimana", "kapan", "di mana", "terima", "kasih", "sama",
                "bantu", "buat", "tahu", "paham", "mengerti", "maaf", "halo", "selamat", "pagi", "siang",
                "sore", "malam", "kembali", "sama-sama", "permisi", "silakan", "tolong"
            )
            val entities = initialWords.distinct().map { WordEntity(it, 5, isUserCustom = false) }
            database.wordDao().insertWords(entities)
        }
    }
}

