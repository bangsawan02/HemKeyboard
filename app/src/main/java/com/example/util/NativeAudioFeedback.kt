package com.example.util

import android.content.Context
import android.media.AudioManager

/**
 * Native Android SDK AudioManager feedback helper.
 * Provides instant native sound effects for keyboard key presses without third-party audio files.
 */
object NativeAudioFeedback {

    enum class SoundType {
        STANDARD,
        SPACEBAR,
        DELETE,
        RETURN
    }

    fun playKeyPressSound(context: Context, type: SoundType = SoundType.STANDARD) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val fx = when (type) {
                SoundType.STANDARD -> AudioManager.FX_KEYPRESS_STANDARD
                SoundType.SPACEBAR -> AudioManager.FX_KEYPRESS_SPACEBAR
                SoundType.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                SoundType.RETURN -> AudioManager.FX_KEYPRESS_RETURN
            }
            audioManager.playSoundEffect(fx)
        } catch (_: Exception) {
            // Graceful fallback if audio service is unavailable
        }
    }
}
