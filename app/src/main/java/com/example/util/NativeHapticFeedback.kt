package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Native Android SDK Vibrator and VibrationEffect helper.
 * Delivers per-key precision haptic pulses across Android API levels.
 */
object NativeHapticFeedback {

    enum class HapticType {
        KEY_NORMAL,       // Soft tick for standard character keys
        KEY_SPACE_SYMBOL, // Normal click for space, punctuation, modifiers
        KEY_ACTION_HEAVY, // Heavy click for Backspace, Enter, Delete
        KEY_DOUBLE_PULSE, // Double click for CapsLock, Mode Switch, Subtype change
        LONG_PRESS        // Extended feedback for key long-press & alt popups
    }

    fun performHapticFeedback(
        context: Context,
        baseDurationMs: Long = 30L,
        type: HapticType = HapticType.KEY_NORMAL
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
                val effect = createVibrationEffect(baseDurationMs, type)
                vibrator?.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                val effect = createVibrationEffect(baseDurationMs, type)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                val duration = when (type) {
                    HapticType.KEY_NORMAL -> (baseDurationMs * 0.6f).toLong().coerceIn(8L, 25L)
                    HapticType.KEY_SPACE_SYMBOL -> baseDurationMs.coerceIn(15L, 40L)
                    HapticType.KEY_ACTION_HEAVY -> (baseDurationMs * 1.5f).toLong().coerceIn(30L, 60L)
                    HapticType.KEY_DOUBLE_PULSE -> (baseDurationMs * 1.8f).toLong().coerceIn(35L, 70L)
                    HapticType.LONG_PRESS -> (baseDurationMs * 2.0f).toLong().coerceIn(40L, 80L)
                }
                vibrator?.vibrate(duration)
            }
        } catch (_: Exception) {
            // Graceful fallback
        }
    }

    private fun createVibrationEffect(baseDurationMs: Long, type: HapticType): VibrationEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return when (type) {
                HapticType.KEY_NORMAL -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                HapticType.KEY_SPACE_SYMBOL -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                HapticType.KEY_ACTION_HEAVY -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                HapticType.KEY_DOUBLE_PULSE -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                HapticType.LONG_PRESS -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val (duration, amplitude) = when (type) {
                HapticType.KEY_NORMAL -> Pair((baseDurationMs * 0.5f).toLong().coerceAtLeast(10L), 60)
                HapticType.KEY_SPACE_SYMBOL -> Pair(baseDurationMs.coerceAtLeast(15L), 120)
                HapticType.KEY_ACTION_HEAVY -> Pair((baseDurationMs * 1.4f).toLong().coerceAtLeast(30L), 220)
                HapticType.KEY_DOUBLE_PULSE -> Pair((baseDurationMs * 1.6f).toLong().coerceAtLeast(35L), 200)
                HapticType.LONG_PRESS -> Pair((baseDurationMs * 2.0f).toLong().coerceAtLeast(40L), 255)
            }
            return VibrationEffect.createOneShot(duration, amplitude)
        }
        return VibrationEffect.createOneShot(baseDurationMs, VibrationEffect.DEFAULT_AMPLITUDE)
    }
}
