package com.example.util

import android.content.Context
import android.media.AudioManager

/**
 * Native Android SDK AudioManager feedback helper.
 * Provides instant sound effects for keyboard key presses without third-party libraries.
 */
object NativeAudioFeedback {

    fun playKeyPressSound(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
        } catch (_: Exception) {
            // Graceful fallback if audio service is unavailable
        }
    }
}
