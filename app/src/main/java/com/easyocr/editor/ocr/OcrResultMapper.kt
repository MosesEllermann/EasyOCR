package com.easyocr.editor.ocr

import com.easyocr.editor.geometry.RectF2

object OcrResultMapper {
    fun fromBlocks(blocks: List<RecognizedTextBlock>): OcrResult {
        val mappedBlocks = blocks.mapIndexedNotNull { index, block ->
            val text = block.text.trim()
            if (text.isBlank()) return@mapIndexedNotNull null
            OcrTextBlock(
                id = "block-$index",
                text = text,
                boundingBox = block.boundingBox,
                lines = block.lines.mapIndexed { lineIndex, line ->
                    OcrTextLine(
                        id = "block-$index-line-$lineIndex",
                        text = line.text,
                        boundingBox = line.boundingBox,
                    )
                },
            )
        }

        return OcrResult(
            fullText = mappedBlocks.joinToString(separator = "\n\n") { it.text },
            blocks = mappedBlocks,
        )
    }
}

data class RecognizedTextBlock(
    val text: String,
    val boundingBox: RectF2,
    val lines: List<RecognizedTextLine> = emptyList(),
)

data class RecognizedTextLine(
    val text: String,
    val boundingBox: RectF2,
)
