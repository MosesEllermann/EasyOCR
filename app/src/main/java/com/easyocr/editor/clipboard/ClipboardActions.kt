package com.easyocr.editor.clipboard

import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString

class ClipboardActions(
    private val clipboardManager: ClipboardManager,
) {
    fun copy(text: String): Boolean {
        val normalized = text.trim()
        if (normalized.isBlank()) return false
        clipboardManager.setText(AnnotatedString(normalized))
        return true
    }
}
