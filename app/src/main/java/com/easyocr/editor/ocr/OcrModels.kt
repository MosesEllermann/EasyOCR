package com.easyocr.editor.ocr

import com.easyocr.editor.geometry.RectF2

data class OcrResult(
    val fullText: String,
    val blocks: List<OcrTextBlock>,
)

data class OcrTextBlock(
    val id: String,
    val text: String,
    val boundingBox: RectF2,
    val lines: List<OcrTextLine> = emptyList(),
)

data class OcrTextLine(
    val id: String,
    val text: String,
    val boundingBox: RectF2,
)

enum class OcrLanguage(val label: String) {
    EnglishGerman("English + German"),
    English("English"),
    German("German"),
}
