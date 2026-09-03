package com.example.ime

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
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
import com.example.util.NativeAudioFeedback
import com.example.util.NativeClipboardHelper
import com.example.util.NativeHapticFeedback
import com.example.util.NativeInputMethodHelper
import com.example.util.NativeSpellCheckerHelper
import com.example.util.NativeUserDictionaryHelper
import com.example.util.NativeVoiceInputHelper
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
    private var autoCapitalizeNext by mutableStateOf(false)
    private var actionLabel by mutableStateOf("Enter")
    private var latestClipboardText by mutableStateOf<String?>(null)
    private var isVoiceListening by mutableStateOf(false)
    private var inlineSuggestionViews by mutableStateOf<List<View>>(emptyList())

    private var vowelOptionalEnabled by mutableStateOf(false)
    private var guessMissingLettersEnabled by mutableStateOf(false)
    private var nextWordPredictionEnabled by mutableStateOf(true)
    private var alwaysPredictEnabled by mutableStateOf(false)
    private var predictPasswordsEnabled by mutableStateOf(false)
    private var enableKeyPreview by mutableStateOf(true)
    
    private var lastSpacePressTime: Long = 0
    private var predictionJob: Job? = null
    private var wordsTypedSincePrune = 0
    private var previousWord: String? = null

    // Native Android Helpers
    private var spellCheckerHelper: NativeSpellCheckerHelper? = null
    private var voiceInputHelper: NativeVoiceInputHelper? = null
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        database = DatabaseProvider.getDatabase(this)

        // Initialize Native Spell Checker Helper
        spellCheckerHelper = NativeSpellCheckerHelper(this) { _, suggestions ->
            if (suggestions.isNotEmpty() && currentWord.isNotEmpty()) {
                val merged = (predictionsList + suggestions).distinct().take(5)
                predictionsList = merged
            }
        }.apply { initialize() }

        // Initialize Native Voice Typing Helper
        voiceInputHelper = NativeVoiceInputHelper(
            context = this,
            onTextRecognized = { text, isFinal ->
                val ic = currentInputConnection
                if (ic != null && text.isNotBlank()) {
                    ic.commitText(if (isFinal) "$text " else text, 1)
                    if (isFinal) {
                        learnWord(text, previousWord)
                        previousWord = text.lowercase()
                        currentWord = ""
                        updateSuggestions()
                    }
                }
            },
            onListeningStateChanged = { listening, _ ->
                isVoiceListening = listening
            }
        )

        // Initialize Native Clipboard primary clip listener
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
            refreshClipboardPreview()
        }
        clipboardListener?.let { clipboard?.addPrimaryClipChangedListener(it) }
        refreshClipboardPreview()
    }

    private fun refreshClipboardPreview() {
        val clipText = NativeClipboardHelper.getPrimaryClipText(this)
        if (!clipText.isNullOrBlank() && clipText.length in 1..80) {
            latestClipboardText = clipText
        } else {
            latestClipboardText = null
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        // Modern keyboards should not take up the entire screen in landscape mode
        return false
    }

    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        if (outInsets == null) return
        val decorView = window?.window?.decorView ?: return
        val visibleTop = decorView.height
        outInsets.contentTopInsets = visibleTop
        outInsets.visibleTopInsets = visibleTop
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
        outInsets.touchableRegion.set(0, 0, decorView.width, decorView.height)
    }

    // --- Android 11+ (API 30+) Autofill Inline Suggestions (Passwords / OTP / Credentials) ---
    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val stylesBundle = Bundle()
            val inlinePresentationSpec = InlinePresentationSpec.Builder(
                Size(100, 36),
                Size(700, 100)
            ).setStyle(stylesBundle).build()

            return InlineSuggestionsRequest.Builder(listOf(inlinePresentationSpec))
                .setMaxSuggestionCount(6)
                .build()
        }
        return null
    }

    override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val suggestions = response.inlineSuggestions
            if (suggestions.isEmpty()) {
                inlineSuggestionViews = emptyList()
                return false
            }

            val context = this
            val views = mutableListOf<View>()
            val executor = ContextCompat.getMainExecutor(context)
            var pendingCount = suggestions.size

            for (suggestion in suggestions) {
                suggestion.inflate(context, Size(ViewGroup.LayoutParams.WRAP_CONTENT, 80), executor) { view ->
                    if (view != null) {
                        views.add(view)
                    }
                    pendingCount--
                    if (pendingCount == 0) {
                        inlineSuggestionViews = views.toList()
                    }
                }
            }
            return true
        }
        return false
    }

    // --- Hardware / Bluetooth Keyboard Interception (onKeyDown & onKeyUp) ---
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && event.isCtrlPressed) {
            when (keyCode) {
                KeyEvent.KEYCODE_C -> {
                    handleSpecialPress("COPY")
                    return true
                }
                KeyEvent.KEYCODE_V -> {
                    handleSpecialPress("PASTE")
                    return true
                }
                KeyEvent.KEYCODE_X -> {
                    handleSpecialPress("CUT")
                    return true
                }
                KeyEvent.KEYCODE_A -> {
                    handleSpecialPress("SELECT_ALL")
                    return true
                }
                KeyEvent.KEYCODE_Z -> {
                    if (event.isShiftPressed) handleSpecialPress("REDO") else handleSpecialPress("UNDO")
                    return true
                }
                KeyEvent.KEYCODE_Y -> {
                    handleSpecialPress("REDO")
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyUp(keyCode, event)
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        // Subtype changed dynamically
        triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_DOUBLE_PULSE)
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
                    autoCapitalizeNext = autoCapitalizeNext,
                    currentWord = currentWord,
                    actionLabel = actionLabel,
                    clipboardText = latestClipboardText,
                    isVoiceListening = isVoiceListening,
                    onVoiceClick = { toggleVoiceTyping() },
                    inlineSuggestionViews = inlineSuggestionViews,
                    onSwitchIme = {
                        val token = window?.window?.attributes?.token
                        NativeInputMethodHelper.switchToNextInputMethod(this@KeyboardIME, token)
                    },
                    enableKeyPreview = enableKeyPreview
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
        updateActionLabel(info)
        checkAutoCapitalize()
        refreshClipboardPreview()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentWord = ""
        predictionsList = emptyList()
        inlineSuggestionViews = emptyList()
        updateActionLabel(attribute)

        lifecycleScope.launch(Dispatchers.IO) {
            val themeVal = database.settingDao().getSetting("keyboard_theme") ?: KeyboardThemeStyle.LIGHT.name
            val heightVal = database.settingDao().getSetting("keyboard_height") ?: KeyboardHeightStyle.NORMAL.name
            val shapeVal = database.settingDao().getSetting("key_shape") ?: KeyShapeStyle.ROUNDED.name
            val autoCorrectVal = database.settingDao().getSetting("autocorrect_enabled") ?: "true"
            val predictionVal = database.settingDao().getSetting("prediction_enabled") ?: "true"
            val hapticVal = database.settingDao().getSetting("haptic_enabled") ?: "true"
            val hapticDurationVal = database.settingDao().getSetting("haptic_duration_ms") ?: "30"
            val vowelOptionalVal = database.settingDao().getSetting("vowel_optional_enabled") ?: "false"
            val guessMissingVal = database.settingDao().getSetting("guess_missing_letters_enabled") ?: "false"
            val nextWordVal = database.settingDao().getSetting("next_word_enabled") ?: "true"
            val alwaysPredictVal = database.settingDao().getSetting("always_predict_enabled") ?: "false"
            val predictPassVal = database.settingDao().getSetting("predict_passwords_enabled") ?: "false"
            val keyPreviewVal = database.settingDao().getSetting("key_preview_enabled") ?: "true"

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
                vowelOptionalEnabled = vowelOptionalVal.toBoolean()
                guessMissingLettersEnabled = guessMissingVal.toBoolean()
                nextWordPredictionEnabled = nextWordVal.toBoolean()
                alwaysPredictEnabled = alwaysPredictVal.toBoolean()
                predictPasswordsEnabled = predictPassVal.toBoolean()
                enableKeyPreview = keyPreviewVal.toBoolean()
                updateSuggestions()
            }
        }
    }

    private fun updateActionLabel(info: EditorInfo?) {
        if (info == null) {
            actionLabel = "Enter"
            return
        }
        val action = info.imeOptions and (EditorInfo.IME_MASK_ACTION or EditorInfo.IME_FLAG_NO_ENTER_ACTION)
        actionLabel = when (action) {
            EditorInfo.IME_ACTION_GO -> "Go"
            EditorInfo.IME_ACTION_SEARCH -> "Cari"
            EditorInfo.IME_ACTION_SEND -> "Kirim"
            EditorInfo.IME_ACTION_NEXT -> "Lanjut"
            EditorInfo.IME_ACTION_DONE -> "Selesai"
            else -> "Enter"
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        voiceInputHelper?.stopListening()
        inlineSuggestionViews = emptyList()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        refreshClipboardPreview()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        voiceInputHelper?.stopListening()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        predictionJob?.cancel()
        voiceInputHelper?.stopListening()
        spellCheckerHelper?.close()
        
        clipboardListener?.let {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.removePrimaryClipChangedListener(it)
        }
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

    private fun toggleVoiceTyping() {
        if (isVoiceListening) {
            voiceInputHelper?.stopListening()
        } else {
            voiceInputHelper?.startListening()
        }
    }

    private fun handleKeyPress(char: Char) {
        val ic = currentInputConnection ?: return
        triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)

        if (char.isLetterOrDigit() || char == '\'') {
            currentWord += char
            ic.commitText(char.toString(), 1)
            updateSuggestions()
            spellCheckerHelper?.checkSpelling(currentWord)
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

        when {
            action == "BACKSPACE" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.DELETE, NativeHapticFeedback.HapticType.KEY_ACTION_HEAVY)
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
                triggerFeedback(NativeAudioFeedback.SoundType.SPACEBAR, NativeHapticFeedback.HapticType.KEY_SPACE_SYMBOL)
                val now = System.currentTimeMillis()
                if (now - lastSpacePressTime < 300) {
                    // Double tap space: convert last space to period
                    ic.deleteSurroundingText(1, 0)
                    ic.commitText(". ", 1)
                    previousWord = "."
                    lastSpacePressTime = 0
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
            action == "ENTER" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.RETURN, NativeHapticFeedback.HapticType.KEY_ACTION_HEAVY)
                if (currentWord.isNotEmpty()) {
                    learnWord(currentWord, previousWord)
                    previousWord = currentWord.lowercase()
                    currentWord = ""
                } else {
                    previousWord = null
                }
                predictionsList = emptyList()

                val info = currentInputEditorInfo
                if (info != null && (info.imeOptions and EditorInfo.IME_MASK_ACTION) != EditorInfo.IME_ACTION_NONE &&
                    (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
                    val performed = ic.performEditorAction(info.imeOptions and EditorInfo.IME_MASK_ACTION)
                    if (!performed) {
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                    }
                } else {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }
            }
            action == "CURSOR_LEFT" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
            }
            action == "CURSOR_RIGHT" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
            }
            action == "SELECT_ALL" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_SPACE_SYMBOL)
                val handled = ic.performContextMenuAction(android.R.id.selectAll)
                if (!handled) {
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, 0, KeyEvent.META_CTRL_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A, 0, KeyEvent.META_CTRL_ON))
                }
            }
            action == "COPY" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_SPACE_SYMBOL)
                val handled = ic.performContextMenuAction(android.R.id.copy)
                if (!handled) {
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_C, 0, KeyEvent.META_CTRL_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_C, 0, KeyEvent.META_CTRL_ON))
                }
                refreshClipboardPreview()
            }
            action == "CUT" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_SPACE_SYMBOL)
                val handled = ic.performContextMenuAction(android.R.id.cut)
                if (!handled) {
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_X, 0, KeyEvent.META_CTRL_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_X, 0, KeyEvent.META_CTRL_ON))
                }
                refreshClipboardPreview()
            }
            action == "PASTE" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_SPACE_SYMBOL)
                val handled = ic.performContextMenuAction(android.R.id.paste)
                if (!handled) {
                    val clip = NativeClipboardHelper.getPrimaryClipText(this)
                    if (!clip.isNullOrEmpty()) {
                        ic.commitText(clip, 1)
                    } else {
                        ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_V, 0, KeyEvent.META_CTRL_ON))
                        ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_V, 0, KeyEvent.META_CTRL_ON))
                    }
                }
            }
            action == "UNDO" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_SPACE_SYMBOL)
                val handled = ic.performContextMenuAction(android.R.id.undo)
                if (!handled) {
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON))
                }
            }
            action == "REDO" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_SPACE_SYMBOL)
                val handled = ic.performContextMenuAction(android.R.id.redo)
                if (!handled) {
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Y, 0, KeyEvent.META_CTRL_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Y, 0, KeyEvent.META_CTRL_ON))
                }
            }
            action == "ARROW_LEFT" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
            }
            action == "ARROW_RIGHT" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
            }
            action == "ARROW_UP" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP))
            }
            action == "ARROW_DOWN" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN))
            }
            action == "DELETE_WORD" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.DELETE, NativeHapticFeedback.HapticType.KEY_ACTION_HEAVY)
                if (currentWord.isNotEmpty()) {
                    ic.deleteSurroundingText(currentWord.length, 0)
                    currentWord = ""
                    updateSuggestions()
                } else {
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0, KeyEvent.META_CTRL_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, 0, KeyEvent.META_CTRL_ON))
                }
            }
            action == "HIDE_KEYBOARD" -> {
                requestHideSelf(0)
            }
            action.startsWith("COMMIT:") -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                val text = action.removePrefix("COMMIT:")
                ic.commitText(text, 1)
            }
            else -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
            }
        }
        checkAutoCapitalize()
    }

    private fun applyPrediction(word: String) {
        val ic = currentInputConnection ?: return
        triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
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
            delay(40)
            
            var dbPredictions = if (prefix.isNotEmpty()) {
                database.wordDao().getPredictions(prefix, 10).toMutableList()
            } else if (currentPreviousWord != null && nextWordPredictionEnabled) {
                database.wordDao().getNextWordPredictions(currentPreviousWord, 5).toMutableList()
            } else {
                mutableListOf()
            }

            // Include system UserDictionary.Words words via ContentResolver
            if (prefix.isNotEmpty() && dbPredictions.size < 5) {
                val systemUserWords = NativeUserDictionaryHelper.getSystemUserWords(this@KeyboardIME)
                val matchedSystemWords = systemUserWords.filter { it.word.lowercase().startsWith(prefix) }
                    .map { it.word }
                dbPredictions.addAll(matchedSystemWords)
            }

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
        if (cleanWord.length < 2 || cleanWord.any { !it.isLetterOrDigit() && it != '-' && it != '\'' }) return

        val isNameOrCustom = cleanWord[0].isUpperCase()
        wordsTypedSincePrune++

        lifecycleScope.launch(Dispatchers.IO) {
            val lowerWord = cleanWord.lowercase()
            
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
                // Also write to Android native system UserDictionary
                if (isNameOrCustom) {
                    NativeUserDictionaryHelper.addWordToSystemDictionary(this@KeyboardIME, cleanWord)
                }
            }

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

            if (wordsTypedSincePrune >= 50) {
                database.wordDao().pruneDictionary(5000)
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                database.wordDao().pruneBigrams(thirtyDaysAgo)
                wordsTypedSincePrune = 0
            }
        }
    }

    private fun triggerFeedback(
        soundType: NativeAudioFeedback.SoundType = NativeAudioFeedback.SoundType.STANDARD,
        hapticType: NativeHapticFeedback.HapticType = NativeHapticFeedback.HapticType.KEY_NORMAL
    ) {
        NativeAudioFeedback.playKeyPressSound(this, soundType)
        if (hapticEnabled) {
            NativeHapticFeedback.performHapticFeedback(this, hapticDurationMs, hapticType)
        }
    }
}
