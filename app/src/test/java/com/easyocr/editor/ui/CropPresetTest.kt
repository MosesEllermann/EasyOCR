package com.easyocr.editor.ui

import com.easyocr.editor.geometry.SizeF2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropPresetTest {
    @Test
    fun squarePresetCreatesOneToOneCrop() {
        val crop = cropRectForPreset(SizeF2(1200f, 800f), CropPreset.Square)

        assertEquals(1f, crop.rect.width / crop.rect.height, 0.001f)
        assertEquals(CropPreset.Square, crop.preset)
    }

    @Test
    fun documentPresetKeepsFourEditableCorners() {
        val crop = cropRectForPreset(SizeF2(1200f, 800f), CropPreset.Document)

        assertEquals(4, crop.documentCorners.size)
        assertTrue(crop.rect.width > 0f)
        assertTrue(crop.rect.height > 0f)
    }
}
