package com.example.util

import android.content.ClipDescription
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat

/**
 * Native Android SDK InputContentInfo helper.
 * Facilitates committing rich media (GIFs, stickers, images) directly into compatible messaging apps.
 */
object NativeMediaHelper {

    fun isMimeTypeSupported(editorInfo: EditorInfo?, mimeType: String): Boolean {
        if (editorInfo == null) return false
        val supportedTypes = EditorInfoCompat.getContentMimeTypes(editorInfo)
        return supportedTypes.any { ClipDescription.compareMimeTypes(it, mimeType) }
    }

    fun commitMediaContent(
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
        contentUri: Uri,
        mimeType: String,
        label: String = "Media"
    ): Boolean {
        if (inputConnection == null || editorInfo == null) return false
        return try {
            val description = ClipDescription(label, arrayOf(mimeType))
            val inputContentInfoCompat = InputContentInfoCompat(
                contentUri,
                description,
                null
            )
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
            } else {
                0
            }
            InputConnectionCompat.commitContent(inputConnection, editorInfo, inputContentInfoCompat, flags, null)
        } catch (_: Exception) {
            false
        }
    }
}
