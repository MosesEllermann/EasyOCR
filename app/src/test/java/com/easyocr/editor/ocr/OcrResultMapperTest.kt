package com.easyocr.editor.ocr

import com.easyocr.editor.geometry.RectF2
import org.junit.Assert.assertEquals
import org.junit.Test

class OcrResultMapperTest {
    @Test
    fun mapsNonBlankBlocksAndPreservesBounds() {
        val result = OcrResultMapper.fromBlocks(
            listOf(
                RecognizedTextBlock(" Hello ", RectF2(1f, 2f, 10f, 20f)),
                RecognizedTextBlock("  ", RectF2(0f, 0f, 1f, 1f)),
                RecognizedTextBlock("Welt", RectF2(30f, 40f, 70f, 80f)),
            ),
        )

        assertEquals("Hello\n\nWelt", result.fullText)
        assertEquals(2, result.blocks.size)
        assertEquals(RectF2(1f, 2f, 10f, 20f), result.blocks[0].boundingBox)
        assertEquals("block-2", result.blocks[1].id)
    }
}
