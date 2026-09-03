package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype

/**
 * Native Android SDK InputMethodManager & InputMethodSubtype helper.
 * Provides IME status checks, subtype management, and triggers native system intent pickers.
 */
object NativeInputMethodHelper {

    fun isKeyboardEnabled(context: Context): Boolean {
        return try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val enabledList = imm?.enabledInputMethodList ?: emptyList()
            enabledList.any { it.packageName == context.packageName }
        } catch (_: Exception) {
            false
        }
    }

    fun isKeyboardSelected(context: Context): Boolean {
        return try {
            val currentIme = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )
            currentIme != null && currentIme.startsWith(context.packageName)
        } catch (_: Exception) {
            false
        }
    }

    fun openKeyboardSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun showInputMethodPicker(context: Context) {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        } catch (_: Exception) {
            // Graceful fallback
        }
    }

    fun getCurrentInputMethodSubtype(context: Context): InputMethodSubtype? {
        return try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.currentInputMethodSubtype
        } catch (_: Exception) {
            null
        }
    }

    fun switchToNextInputMethod(context: Context, token: IBinder?, onlyCurrentIme: Boolean = false): Boolean {
        if (token == null) {
            showInputMethodPicker(context)
            return true
        }
        return try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                imm?.switchToNextInputMethod(token, onlyCurrentIme) ?: false
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                @Suppress("DEPRECATION")
                imm?.switchToNextInputMethod(token, onlyCurrentIme) ?: false
            } else {
                showInputMethodPicker(context)
                true
            }
        } catch (_: Exception) {
            showInputMethodPicker(context)
            false
        }
    }
}
