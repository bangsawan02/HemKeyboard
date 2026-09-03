package com.example.ime

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.database.BigramEntity
import com.example.database.DatabaseProvider
import com.example.database.KeyboardDatabase
import com.example.database.WordEntity
import com.example.ui.theme.KeyShapeStyle
import com.example.ui.theme.KeyboardHeightStyle
import com.example.ui.theme.KeyboardThemeStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KeyboardIME : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var database: KeyboardDatabase
    private var currentWord by mutableStateOf("")
    private var predictionsList by mutableStateOf<List<String>>(emptyList())
    private var activeTheme by mutableStateOf(KeyboardThemeStyle.LIGHT)
    private var heightStyle by mutableStateOf(KeyboardHeightStyle.NORMAL)
    private var shapeStyle by mutableStateOf(KeyShapeStyle.ROUNDED)
    private var autocorrectEnabled by mutableStateOf(true)
    private var predictionEnabled by mutableStateOf(true)
    private var hapticEnabled by mutableStateOf(true)
    private var hapticDurationMs by mutableStateOf(30L)
    private var codingBarEnabled by mutableStateOf(true)
    private var cursorArrowsEnabled by mutableStateOf(true)
    private var codeSnippetsEnabled by mutableStateOf(true)
    private var tabUsesSpaces by mutableStateOf(true)
    private var autoCapitalizeNext by mutableStateOf(false)

    private var vowelOptionalEnabled by mutableStateOf(false)
    private var guessMissingLettersEnabled by mutableStateOf(false)
    private var mistypeTolerance by mutableStateOf(20)
    private var nextWordPredictionEnabled by mutableStateOf(true)
    private var alwaysPredictEnabled by mutableStateOf(false)
    private var predictPasswordsEnabled by mutableStateOf(false)
    private var showPasswordEnabled by mutableStateOf(false)
    
    private var lastSpacePressTime: Long = 0
    private var predictionJob: Job? = null
    private var wordsTypedSincePrune = 0
    private var previousWord: String? = null

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        database = DatabaseProvider.getDatabase(this)
    }

    override fun onCreateInputView(): View {
        val frameLayout = android.widget.FrameLayout(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val composeView = ComposeView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            
            setContent {
                KeyboardView(
                    onKeyPress = { char -> handleKeyPress(char) },
                    onSpecialPress = { action -> handleSpecialPress(action) },
                    predictions = predictionsList,
                    onPredictionClick = { word -> applyPrediction(word) },
                    activeTheme = activeTheme,
                    heightStyle = heightStyle,
                    shapeStyle = shapeStyle,
                    codingBarEnabled = codingBarEnabled,
                    cursorArrowsEnabled = cursorArrowsEnabled,
                    codeSnippetsEnabled = codeSnippetsEnabled,
                    autoCapitalizeNext = autoCapitalizeNext,
                    currentWord = currentWord
                )
            }
        }
        
        frameLayout.addView(composeView)
        
        val applyOwners = { view: View ->
            view.setViewTreeLifecycleOwner(this@KeyboardIME)
            view.setViewTreeSavedStateRegistryOwner(this@KeyboardIME)
            view.setViewTreeViewModelStoreOwner(this@KeyboardIME)
        }
        
        applyOwners(frameLayout)
        applyOwners(composeView)
        
        window.window?.decorView?.let { applyOwners(it) }
        
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        return frameLayout
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        checkAutoCapitalize()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentWord = ""
        predictionsList = emptyList()

        lifecycleScope.launch(Dispatchers.IO) {
            val themeVal = database.settingDao().getSetting("keyboard_theme") ?: KeyboardThemeStyle.LIGHT.name
            val heightVal = database.settingDao().getSetting("keyboard_height") ?: KeyboardHeightStyle.NORMAL.name
            val shapeVal = database.settingDao().getSetting("key_shape") ?: KeyShapeStyle.ROUNDED.name
            val autoCorrectVal = database.settingDao().getSetting("autocorrect_enabled") ?: "true"
            val predictionVal = database.settingDao().getSetting("prediction_enabled") ?: "true"
            val hapticVal = database.settingDao().getSetting("haptic_enabled") ?: "true"
            val hapticDurationVal = database.settingDao().getSetting("haptic_duration_ms") ?: "30"
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

            withContext(Dispatchers.Main) {
                activeTheme = try {
                    KeyboardThemeStyle.valueOf(themeVal)
                } catch (e: Exception) {
                    KeyboardThemeStyle.LIGHT
                }
                heightStyle = try {
                    KeyboardHeightStyle.valueOf(heightVal)
                } catch (e: Exception) {
                    KeyboardHeightStyle.NORMAL
                }
                shapeStyle = try {
                    KeyShapeStyle.valueOf(shapeVal)
                } catch (e: Exception) {
                    KeyShapeStyle.ROUNDED
                }
                autocorrectEnabled = autoCorrectVal.toBoolean()
                predictionEnabled = predictionVal.toBoolean()
                hapticEnabled = hapticVal.toBoolean()
                hapticDurationMs = hapticDurationVal.toLongOrNull() ?: 30L
                codingBarEnabled = codingBarVal.toBoolean()
                cursorArrowsEnabled = cursorArrowsVal.toBoolean()
                codeSnippetsEnabled = codeSnippetsVal.toBoolean()
                tabUsesSpaces = tabSpacesVal.toBoolean()
                vowelOptionalEnabled = vowelOptionalVal.toBoolean()
                guessMissingLettersEnabled = guessMissingVal.toBoolean()
                mistypeTolerance = mistypeToleranceVal.toIntOrNull() ?: 20
                nextWordPredictionEnabled = nextWordVal.toBoolean()
                alwaysPredictEnabled = alwaysPredictVal.toBoolean()
                predictPasswordsEnabled = predictPassVal.toBoolean()
                showPasswordEnabled = showPassVal.toBoolean()
                updateSuggestions()
            }
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        predictionJob?.cancel()
        super.onDestroy()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        // If user moved cursor manually, clear current typing state
        if (currentWord.isNotEmpty() && newSelStart != oldSelStart + 1 && newSelStart != oldSelStart) {
            currentWord = ""
            predictionsList = emptyList()
        }
        checkAutoCapitalize()
    }

    private fun checkAutoCapitalize() {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(10, 0) ?: ""
        val trimmed = textBefore.trimEnd()
        autoCapitalizeNext = textBefore.isEmpty() || 
                             trimmed.endsWith(".") || 
                             trimmed.endsWith("?") || 
                             trimmed.endsWith("!") || 
                             textBefore.endsWith("\n")
    }

    private fun handleKeyPress(char: Char) {
        val ic = currentInputConnection ?: return
        triggerFeedback()

        if (char.isLetterOrDigit() || char == '\'') {
            currentWord += char
            ic.commitText(char.toString(), 1)
            updateSuggestions()
        } else {
            if (currentWord.isNotEmpty()) {
                learnWord(currentWord, previousWord)
                previousWord = currentWord.lowercase()
                currentWord = ""
            }
            ic.commitText(char.toString(), 1)
            updateSuggestions()
        }
        checkAutoCapitalize()
    }

    private fun handleSpecialPress(action: String) {
        val ic = currentInputConnection ?: return
        triggerFeedback()

        when {
            action == "BACKSPACE" -> {
                if (currentWord.isNotEmpty()) {
                    currentWord = currentWord.substring(0, currentWord.length - 1)
                    ic.deleteSurroundingText(1, 0)
                    updateSuggestions()
                } else {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                }
            }
            action == "SPACE" -> {
                val now = System.currentTimeMillis()
                if (now - lastSpacePressTime < 300) {
                    // Double tap space: convert last space to period
                    ic.deleteSurroundingText(1, 0)
                    ic.commitText(". ", 1)
                    previousWord = "."
                    lastSpacePressTime = 0 // Reset to prevent triple-tap dots
                    updateSuggestions()
                } else {
                    if (currentWord.isNotEmpty()) {
                        if (autocorrectEnabled && predictionsList.isNotEmpty()) {
                            val correction = predictionsList[0]
                            if (correction.lowercase() != currentWord.lowercase()) {
                                ic.deleteSurroundingText(currentWord.length, 0)
                                ic.commitText(correction, 1)
                                learnWord(correction, previousWord)
                                previousWord = correction.lowercase()
                            } else {
                                learnWord(currentWord, previousWord)
                                previousWord = currentWord.lowercase()
                            }
                        } else {
                            learnWord(currentWord, previousWord)
                            previousWord = currentWord.lowercase()
                        }
                        currentWord = ""
                    }
                    ic.commitText(" ", 1)
                    lastSpacePressTime = now
                    updateSuggestions()
                }
            }
            action == "CURSOR_LEFT" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
            }
            action == "CURSOR_RIGHT" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
            }
            action == "ENTER" -> {
                if (currentWord.isNotEmpty()) {
                    learnWord(currentWord, previousWord)
                    previousWord = currentWord.lowercase()
                    currentWord = ""
                } else {
                    previousWord = null // Reset context on newline
                }
                predictionsList = emptyList()
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            action == "TAB" -> {
                if (tabUsesSpaces) {
                    ic.commitText("    ", 1)
                } else {
                    ic.commitText("\t", 1)
                }
            }
            action == "SELECT_ALL" -> {
                val handled = ic.performContextMenuAction(android.R.id.selectAll)
                if (!handled) {
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, 0, KeyEvent.META_CTRL_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A, 0, KeyEvent.META_CTRL_ON))
                }
            }
            action == "COPY" -> {
                val handled = ic.performContextMenuAction(android.R.id.copy)
                if (!handled) {
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_C, 0, KeyEvent.META_CTRL_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_C, 0, KeyEvent.META_CTRL_ON))
                }
            }
            action == "CUT" -> {
                val handled = ic.performContextMenuAction(android.R.id.cut)
                if (!handled) {
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_X, 0, KeyEvent.META_CTRL_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_X, 0, KeyEvent.META_CTRL_ON))
                }
            }
            action == "PASTE" -> {
                val handled = ic.performContextMenuAction(android.R.id.paste)
                if (!handled) {
                    val clip = com.example.util.NativeClipboardHelper.getPrimaryClipText(this)
                    if (!clip.isNullOrEmpty()) {
                        ic.commitText(clip, 1)
                    } else {
                        ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_V, 0, KeyEvent.META_CTRL_ON))
                        ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_V, 0, KeyEvent.META_CTRL_ON))
                    }
                }
            }
            action == "UNDO" -> {
                val handled = ic.performContextMenuAction(android.R.id.undo)
                if (!handled) {
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON))
                }
            }
            action == "REDO" -> {
                val handled = ic.performContextMenuAction(android.R.id.redo)
                if (!handled) {
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Y, 0, KeyEvent.META_CTRL_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Y, 0, KeyEvent.META_CTRL_ON))
                }
            }
            action == "ARROW_LEFT" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
            }
            action == "ARROW_RIGHT" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
            }
            action == "ARROW_UP" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP))
            }
            action == "ARROW_DOWN" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN))
            }
            action == "CURSOR_HOME" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_HOME))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_HOME))
            }
            action == "CURSOR_END" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_END))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_END))
            }
            action == "PAGE_UP" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_UP))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PAGE_UP))
            }
            action == "PAGE_DOWN" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_DOWN))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PAGE_DOWN))
            }
            action == "SELECT_LEFT" -> {
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, 0, KeyEvent.META_SHIFT_ON))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT, 0, KeyEvent.META_SHIFT_ON))
            }
            action == "SELECT_RIGHT" -> {
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, 0, KeyEvent.META_SHIFT_ON))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT, 0, KeyEvent.META_SHIFT_ON))
            }
            action == "SELECT_UP" -> {
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP, 0, KeyEvent.META_SHIFT_ON))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP, 0, KeyEvent.META_SHIFT_ON))
            }
            action == "SELECT_DOWN" -> {
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, 0, KeyEvent.META_SHIFT_ON))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN, 0, KeyEvent.META_SHIFT_ON))
            }
            action == "SELECT_HOME" -> {
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_HOME, 0, KeyEvent.META_SHIFT_ON))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_HOME, 0, KeyEvent.META_SHIFT_ON))
            }
            action == "SELECT_END" -> {
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_END, 0, KeyEvent.META_SHIFT_ON))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_END, 0, KeyEvent.META_SHIFT_ON))
            }
            action == "DELETE_WORD" -> {
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0, KeyEvent.META_CTRL_ON))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, 0, KeyEvent.META_CTRL_ON))
            }
            action == "DELETE_LINE" -> {
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_HOME, 0, KeyEvent.META_SHIFT_ON))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_HOME, 0, KeyEvent.META_SHIFT_ON))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            }
            action == "HIDE_KEYBOARD" -> {
                requestHideSelf(0)
            }
            action == "CLIPBOARD" -> {
                ic.performContextMenuAction(android.R.id.paste)
            }
            action == "ESC" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ESCAPE))
            }
            action == "DEL" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_FORWARD_DEL))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_FORWARD_DEL))
            }
            action.startsWith("F") && action.length in 2..3 && action.substring(1).all { it.isDigit() } -> {
                val fNum = action.substring(1).toIntOrNull() ?: 0
                if (fNum in 1..12) {
                    val keyCode = KeyEvent.KEYCODE_F1 + (fNum - 1)
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                }
            }
            action.startsWith("PAIR:") -> {
                val pair = action.removePrefix("PAIR:")
                ic.commitText(pair, 1)
                // Move cursor back inside the pair
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
            }
            action.startsWith("COMMIT:") -> {
                val text = action.removePrefix("COMMIT:")
                ic.commitText(text, 1)
                if (text.endsWith("()") || text.endsWith("{}") || text.endsWith("[]")) {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
                }
            }
        }
        checkAutoCapitalize()
    }

    private fun applyPrediction(word: String) {
        val ic = currentInputConnection ?: return
        triggerFeedback()
        if (currentWord.isNotEmpty()) {
            ic.deleteSurroundingText(currentWord.length, 0)
        }
        ic.commitText(word + " ", 1)
        learnWord(word, previousWord)
        previousWord = word.lowercase()
        currentWord = ""
        updateSuggestions()
        checkAutoCapitalize()
    }

    private fun updateSuggestions() {
        predictionJob?.cancel()
        
        // Check for password field or "no prediction" flag
        val ic = currentInputConnection
        val editorInfo = currentInputEditorInfo
        val isPasswordField = editorInfo?.let { 
            val variation = it.inputType and EditorInfo.TYPE_MASK_VARIATION
            variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD || 
            variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD
        } ?: false

        if (!predictionEnabled) {
            predictionsList = emptyList()
            return
        }

        if (isPasswordField && !predictPasswordsEnabled) {
            predictionsList = emptyList()
            return
        }

        val prefix = currentWord.trim().lowercase()
        val currentPreviousWord = previousWord

        predictionJob = lifecycleScope.launch(Dispatchers.IO) {
            delay(50)
            
            var dbPredictions = if (prefix.isNotEmpty()) {
                // 1. Regular Prefix Match
                database.wordDao().getPredictions(prefix, 10).toMutableList()
            } else if (currentPreviousWord != null && nextWordPredictionEnabled) {
                // 2. Next word prediction based on previous word
                database.wordDao().getNextWordPredictions(currentPreviousWord, 5).toMutableList()
            } else {
                mutableListOf()
            }

            // 3. Vowel Optional Logic
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

            // 4. Guess Missing Letters / Fuzzy Matching (Simple version with tolerance)
            if (prefix.length >= 2 && dbPredictions.size < 3 && guessMissingLettersEnabled) {
                val fuzzyPattern = "%" + prefix.toList().joinToString("%") + "%"
                val fuzzyMatches = database.wordDao().getFuzzyPredictions(fuzzyPattern, 10).filter { word ->
                    !dbPredictions.contains(word)
                }.take(5)
                dbPredictions.addAll(fuzzyMatches)
            }

            if (dbPredictions.isEmpty()) {
                withContext(Dispatchers.Main) { predictionsList = emptyList() }
                return@launch
            }

            val finalPredictions = dbPredictions.distinct().take(5)

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
                predictionsList = formatted
            }
        }
    }

    private fun learnWord(word: String, prevWord: String? = null) {
        val cleanWord = word.trim()
        // Don't learn very short words or garbage
        if (cleanWord.length < 2 || cleanWord.any { !it.isLetterOrDigit() && it != '-' && it != '\'' }) return

        val isNameOrCustom = cleanWord[0].isUpperCase()
        wordsTypedSincePrune++

        lifecycleScope.launch(Dispatchers.IO) {
            val lowerWord = cleanWord.lowercase()
            
            // 1. Learn/Update the word itself
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
            }

            // 2. Learn/Update the Bigram (Previous -> Current)
            if (prevWord != null) {
                val existingBigram = database.wordDao().getBigram(prevWord, lowerWord)
                if (existingBigram != null) {
                    database.wordDao().incrementBigramFrequency(prevWord, lowerWord)
                } else {
                    database.wordDao().insertBigram(
                        BigramEntity(
                            word1 = prevWord,
                            word2 = lowerWord,
                            frequency = 1,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }

            // Periodic pruning
            if (wordsTypedSincePrune >= 50) {
                database.wordDao().pruneDictionary(5000)
                // Also prune old bigrams (e.g., older than 30 days)
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                database.wordDao().pruneBigrams(thirtyDaysAgo)
                wordsTypedSincePrune = 0
            }
        }
    }


    private fun triggerFeedback() {
        com.example.util.NativeAudioFeedback.playKeyPressSound(this)
        if (hapticEnabled) {
            com.example.util.NativeHapticFeedback.performHapticFeedback(this, hapticDurationMs)
        }
    }
}
