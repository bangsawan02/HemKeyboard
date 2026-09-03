package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Native Android SDK ClipboardManager helper.
 * Provides direct clipboard access using native Android ClipData APIs.
 */
object NativeClipboardHelper {

    fun getPrimaryClipText(context: Context): String? {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val item = clipboard?.primaryClip?.getItemAt(0)
            item?.text?.toString()
        } catch (_: Exception) {
            null
        }
    }

    fun setPrimaryClipText(context: Context, label: String, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
        } catch (_: Exception) {
            // Graceful fallback
        }
    }
}
