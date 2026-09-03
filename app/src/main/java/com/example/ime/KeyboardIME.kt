package com.example.ime

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse
import android.view.inputmethod.InputMethodSubtype
import android.widget.inline.InlinePresentationSpec
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
import com.example.database.DatabaseProvider
import com.example.database.KeyboardDatabase
import com.example.ime.engine.InputHandler
import com.example.ime.engine.PredictionEngine
import com.example.ui.theme.KeyShapeStyle
import com.example.ui.theme.KeyboardHeightStyle
import com.example.ui.theme.KeyboardThemeStyle
import com.example.util.NativeAudioFeedback
import com.example.util.NativeClipboardHelper
import com.example.util.NativeHapticFeedback
import com.example.util.NativeInputMethodHelper
import com.example.util.NativeSpellCheckerHelper
import com.example.util.NativeVoiceInputHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main Android InputMethodService (Keyboard IME).
 * Delegates prediction to PredictionEngine and user input processing to InputHandler
 * for clean modularity and maintainability.
 */
class KeyboardIME : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var database: KeyboardDatabase
    private lateinit var predictionEngine: PredictionEngine
    private lateinit var inputHandler: InputHandler

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
        predictionEngine = PredictionEngine(this, database, lifecycleScope)
        inputHandler = InputHandler(this)

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
                        predictionEngine.learnWord(text, previousWord)
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

    // --- Android 11+ Autofill Inline Suggestions ---
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

    // --- Hardware Keyboard Shortcuts ---
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

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
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
        if (lifecycleRegistry.currentState == Lifecycle.State.CREATED || lifecycleRegistry.currentState == Lifecycle.State.INITIALIZED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
        if (lifecycleRegistry.currentState == Lifecycle.State.STARTED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        updateActionLabel(info)
        checkAutoCapitalize()
        refreshClipboardPreview()
        reloadSettingsFromDb()
    }

    override fun onEvaluateInputViewShown(): Boolean {
        // Ensure input view is made visible when input connection is attached
        return super.onEvaluateInputViewShown()
    }

    override fun onShowInputRequested(flags: Int, configChange: Boolean): Boolean {
        return super.onShowInputRequested(flags, configChange)
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentWord = ""
        predictionsList = emptyList()
        inlineSuggestionViews = emptyList()
        updateActionLabel(attribute)
        reloadSettingsFromDb()
    }

    private fun reloadSettingsFromDb() {
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
                activeTheme = try { KeyboardThemeStyle.valueOf(themeVal) } catch (_: Exception) { KeyboardThemeStyle.LIGHT }
                heightStyle = try { KeyboardHeightStyle.valueOf(heightVal) } catch (_: Exception) { KeyboardHeightStyle.NORMAL }
                shapeStyle = try { KeyShapeStyle.valueOf(shapeVal) } catch (_: Exception) { KeyShapeStyle.ROUNDED }
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
        if (lifecycleRegistry.currentState == Lifecycle.State.RESUMED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        if (lifecycleRegistry.currentState == Lifecycle.State.STARTED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        if (lifecycleRegistry.currentState == Lifecycle.State.CREATED || lifecycleRegistry.currentState == Lifecycle.State.INITIALIZED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
        if (lifecycleRegistry.currentState == Lifecycle.State.STARTED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        refreshClipboardPreview()
        reloadSettingsFromDb()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        voiceInputHelper?.stopListening()
        if (lifecycleRegistry.currentState == Lifecycle.State.RESUMED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        if (lifecycleRegistry.currentState == Lifecycle.State.STARTED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        predictionEngine.cancelJob()
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

        ic.beginBatchEdit()
        try {
            if (char.isLetterOrDigit() || char == '\'') {
                currentWord += char
                ic.commitText(char.toString(), 1)
                updateSuggestions()
                spellCheckerHelper?.checkSpelling(currentWord)
            } else {
                if (currentWord.isNotEmpty()) {
                    predictionEngine.learnWord(currentWord, previousWord)
                    previousWord = currentWord.lowercase()
                    currentWord = ""
                }
                ic.commitText(char.toString(), 1)
                updateSuggestions()
            }
        } finally {
            ic.endBatchEdit()
        }
        checkAutoCapitalize()
    }

    private fun handleSpecialPress(action: String) {
        val editorInfo = currentInputEditorInfo
        val ic = currentInputConnection
        inputHandler.executeSpecialAction(
            action = action,
            ic = ic,
            info = editorInfo,
            currentWord = currentWord,
            autocorrectEnabled = autocorrectEnabled,
            predictionsList = predictionsList,
            lastSpacePressTime = lastSpacePressTime,
            onWordStateChange = { newWord, newPrevWord, newSpaceTime ->
                currentWord = newWord
                previousWord = newPrevWord
                lastSpacePressTime = newSpaceTime
            },
            onUpdateSuggestions = { updateSuggestions() },
            onRequestHide = { requestHideSelf(0) },
            onRefreshClipboard = { refreshClipboardPreview() },
            triggerFeedback = { sound, haptic -> triggerFeedback(sound, haptic) },
            learnWord = { w, p -> predictionEngine.learnWord(w, p) },
            previousWord = previousWord
        )
        checkAutoCapitalize()
    }

    private fun applyPrediction(word: String) {
        val ic = currentInputConnection ?: return
        triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
        ic.beginBatchEdit()
        try {
            if (currentWord.isNotEmpty()) {
                ic.deleteSurroundingText(currentWord.length, 0)
            }
            ic.commitText(word + " ", 1)
            predictionEngine.learnWord(word, previousWord)
            previousWord = word.lowercase()
            currentWord = ""
            updateSuggestions()
        } finally {
            ic.endBatchEdit()
        }
        checkAutoCapitalize()
    }

    private fun updateSuggestions() {
        val editorInfo = currentInputEditorInfo
        val isPasswordField = editorInfo?.let { 
            val variation = it.inputType and EditorInfo.TYPE_MASK_VARIATION
            variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD || 
            variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD
        } ?: false

        predictionEngine.updateSuggestions(
            currentWord = currentWord,
            previousWord = previousWord,
            predictionEnabled = predictionEnabled,
            predictPasswordsEnabled = predictPasswordsEnabled,
            isPasswordField = isPasswordField,
            vowelOptionalEnabled = vowelOptionalEnabled,
            guessMissingLettersEnabled = guessMissingLettersEnabled,
            nextWordPredictionEnabled = nextWordPredictionEnabled,
            autoCapitalizeNext = autoCapitalizeNext,
            onResults = { results -> predictionsList = results }
        )
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
