package com.easyocr.editor.ocr

import android.graphics.Bitmap
import com.easyocr.editor.geometry.RectF2
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MlKitOcrEngine : OcrEngine {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognize(
        bitmap: Bitmap,
        language: OcrLanguage,
    ): OcrResult = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val text = recognizer.process(image).await()
        val blocks = text.textBlocks.mapNotNull { block ->
            val box = block.boundingBox ?: return@mapNotNull null
            RecognizedTextBlock(
                text = block.text.trim(),
                boundingBox = RectF2(
                    left = box.left.toFloat(),
                    top = box.top.toFloat(),
                    right = box.right.toFloat(),
                    bottom = box.bottom.toFloat(),
                ),
            )
        }.filter { it.text.isNotBlank() }

        OcrResultMapper.fromBlocks(blocks)
    }
}
