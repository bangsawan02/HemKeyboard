package com.example.util

import android.graphics.Paint
import android.os.Build
import androidx.core.graphics.PaintCompat

/**
 * Native Android SDK Paint.hasGlyph & PaintCompat helper.
 * Filters emoji lists dynamically to ensure only emojis natively supported by the current OS
 * version are displayed, preventing empty box (tofu) artifacts.
 */
object NativeEmojiHelper {

    private val paint = Paint()

    /**
     * Checks whether the current system font has a native glyph for the provided string/emoji.
     */
    fun isGlyphSupported(text: String): Boolean {
        if (text.isBlank()) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PaintCompat.hasGlyph(paint, text)
            } else {
                // On older APIs fallback to true or measurement check
                true
            }
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Filters an emoji collection, keeping only glyphs supported by the device.
     */
    fun filterSupportedEmojis(emojis: List<String>): List<String> {
        return emojis.filter { isGlyphSupported(it) }
    }
}
