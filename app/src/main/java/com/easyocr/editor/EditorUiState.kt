package com.easyocr.editor

import android.graphics.Bitmap
import android.net.Uri
import com.easyocr.editor.ocr.OcrLanguage
import com.easyocr.editor.ocr.OcrResult

data class EditorUiState(
    val sourceUri: Uri? = null,
    val bitmap: Bitmap? = null,
    val ocrResult: OcrResult? = null,
    val language: OcrLanguage = OcrLanguage.EnglishGerman,
    val overlaysEnabled: Boolean = true,
    val isLoadingImage: Boolean = false,
    val isRunningOcr: Boolean = false,
    val lastSavedUri: Uri? = null,
    val errorMessage: String? = null,
) {
    val hasImage: Boolean = bitmap != null
    val fullText: String = ocrResult?.fullText.orEmpty()
}
