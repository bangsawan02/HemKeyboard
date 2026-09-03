package com.example.ime.engine

import android.content.Context
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.example.util.NativeAudioFeedback
import com.example.util.NativeClipboardHelper
import com.example.util.NativeHapticFeedback

/**
 * Modularized Input Handler for Keyboard IME.
 * Encapsulates key presses, special IME actions (cut, copy, paste, undo, redo),
 * cursor movements, and audio/haptic feedback dispatching.
 */
class InputHandler(
    private val context: Context
) {

    fun executeSpecialAction(
        action: String,
        ic: InputConnection?,
        info: EditorInfo?,
        currentWord: String,
        autocorrectEnabled: Boolean,
        predictionsList: List<String>,
        lastSpacePressTime: Long,
        onWordStateChange: (newWord: String, newPrevWord: String?, newSpaceTime: Long) -> Unit,
        onUpdateSuggestions: () -> Unit,
        onRequestHide: () -> Unit,
        onRefreshClipboard: () -> Unit,
        triggerFeedback: (NativeAudioFeedback.SoundType, NativeHapticFeedback.HapticType) -> Unit,
        learnWord: (word: String, prevWord: String?) -> Unit,
        previousWord: String?
    ) {
        if (ic == null) return

        when {
            action == "BACKSPACE" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.DELETE, NativeHapticFeedback.HapticType.KEY_ACTION_HEAVY)
                if (currentWord.isNotEmpty()) {
                    val updatedWord = currentWord.substring(0, currentWord.length - 1)
                    ic.deleteSurroundingText(1, 0)
                    onWordStateChange(updatedWord, previousWord, lastSpacePressTime)
                    onUpdateSuggestions()
                } else {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                }
            }
            action == "SPACE" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.SPACEBAR, NativeHapticFeedback.HapticType.KEY_SPACE_SYMBOL)
                val now = System.currentTimeMillis()
                if (now - lastSpacePressTime < 300) {
                    ic.deleteSurroundingText(1, 0)
                    ic.commitText(". ", 1)
                    onWordStateChange("", ".", 0L)
                    onUpdateSuggestions()
                } else {
                    var updatedPrev = previousWord
                    if (currentWord.isNotEmpty()) {
                        if (autocorrectEnabled && predictionsList.isNotEmpty()) {
                            val correction = predictionsList[0]
                            if (correction.lowercase() != currentWord.lowercase()) {
                                ic.deleteSurroundingText(currentWord.length, 0)
                                ic.commitText(correction, 1)
                                learnWord(correction, previousWord)
                                updatedPrev = correction.lowercase()
                            } else {
                                learnWord(currentWord, previousWord)
                                updatedPrev = currentWord.lowercase()
                            }
                        } else {
                            learnWord(currentWord, previousWord)
                            updatedPrev = currentWord.lowercase()
                        }
                    }
                    ic.commitText(" ", 1)
                    onWordStateChange("", updatedPrev, now)
                    onUpdateSuggestions()
                }
            }
            action == "ENTER" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.RETURN, NativeHapticFeedback.HapticType.KEY_ACTION_HEAVY)
                var updatedPrev = previousWord
                if (currentWord.isNotEmpty()) {
                    learnWord(currentWord, previousWord)
                    updatedPrev = currentWord.lowercase()
                } else {
                    updatedPrev = null
                }
                onWordStateChange("", updatedPrev, lastSpacePressTime)
                onUpdateSuggestions()

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
            action == "CURSOR_LEFT" || action == "ARROW_LEFT" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
            }
            action == "CURSOR_RIGHT" || action == "ARROW_RIGHT" -> {
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
                onRefreshClipboard()
            }
            action == "CUT" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_SPACE_SYMBOL)
                val handled = ic.performContextMenuAction(android.R.id.cut)
                if (!handled) {
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_X, 0, KeyEvent.META_CTRL_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_X, 0, KeyEvent.META_CTRL_ON))
                }
                onRefreshClipboard()
            }
            action == "PASTE" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_SPACE_SYMBOL)
                val handled = ic.performContextMenuAction(android.R.id.paste)
                if (!handled) {
                    val clip = NativeClipboardHelper.getPrimaryClipText(context)
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
            action == "DELETE_WORD" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.DELETE, NativeHapticFeedback.HapticType.KEY_ACTION_HEAVY)
                if (currentWord.isNotEmpty()) {
                    ic.deleteSurroundingText(currentWord.length, 0)
                    onWordStateChange("", previousWord, lastSpacePressTime)
                    onUpdateSuggestions()
                } else {
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0, KeyEvent.META_CTRL_ON))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, 0, KeyEvent.META_CTRL_ON))
                }
            }
            action == "HIDE_KEYBOARD" -> {
                onRequestHide()
            }
            action == "PAGE_UP" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_UP))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PAGE_UP))
            }
            action == "PAGE_DOWN" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_DOWN))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PAGE_DOWN))
            }
            action == "CURSOR_HOME" || action == "HOME" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_HOME))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_HOME))
            }
            action == "CURSOR_END" || action == "END" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_END))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_END))
            }
            action == "SELECT_LEFT" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, 0, KeyEvent.META_SHIFT_ON))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT, 0, KeyEvent.META_SHIFT_ON))
            }
            action == "SELECT_RIGHT" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, 0, KeyEvent.META_SHIFT_ON))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT, 0, KeyEvent.META_SHIFT_ON))
            }
            action == "SELECT_UP" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP, 0, KeyEvent.META_SHIFT_ON))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP, 0, KeyEvent.META_SHIFT_ON))
            }
            action == "SELECT_DOWN" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, 0, KeyEvent.META_SHIFT_ON))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN, 0, KeyEvent.META_SHIFT_ON))
            }
            action == "SELECT_HOME" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_HOME, 0, KeyEvent.META_SHIFT_ON))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_HOME, 0, KeyEvent.META_SHIFT_ON))
            }
            action == "SELECT_END" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_END, 0, KeyEvent.META_SHIFT_ON))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_END, 0, KeyEvent.META_SHIFT_ON))
            }
            action == "ESC" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ESCAPE))
            }
            action == "TAB" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB))
            }
            action == "DEL" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.DELETE, NativeHapticFeedback.HapticType.KEY_ACTION_HEAVY)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_FORWARD_DEL))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_FORWARD_DEL))
            }
            action == "INS" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_INSERT))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_INSERT))
            }
            action == "PRTSCR" -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SYSRQ))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SYSRQ))
            }
            action.startsWith("F") && action.length in 2..3 && (action.drop(1).toIntOrNull() ?: 0) in 1..12 -> {
                triggerFeedback(NativeAudioFeedback.SoundType.STANDARD, NativeHapticFeedback.HapticType.KEY_NORMAL)
                val fNum = action.drop(1).toInt()
                val keyCode = KeyEvent.KEYCODE_F1 + (fNum - 1)
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
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
    }
}
