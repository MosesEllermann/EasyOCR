package com.easyocr.editor.ocr

import android.graphics.Bitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OcrRepository(
    private val engine: OcrEngine,
) {
    private val mutex = Mutex()
    private var cachedBitmapKey: BitmapKey? = null
    private var cachedLanguage: OcrLanguage? = null
    private var cachedResult: OcrResult? = null

    suspend fun recognize(
        bitmap: Bitmap,
        language: OcrLanguage,
        force: Boolean = false,
    ): OcrResult = mutex.withLock {
        val key = BitmapKey(bitmap.width, bitmap.height, bitmap.generationId)
        val cached = cachedResult
        if (!force && cached != null && cachedBitmapKey == key && cachedLanguage == language) {
            return@withLock cached
        }
        engine.recognize(bitmap, language).also {
            cachedBitmapKey = key
            cachedLanguage = language
            cachedResult = it
        }
    }

    fun clear() {
        cachedBitmapKey = null
        cachedLanguage = null
        cachedResult = null
    }
}

private data class BitmapKey(
    val width: Int,
    val height: Int,
    val generationId: Int,
)
