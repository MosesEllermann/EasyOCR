package com.easyocr.editor.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrCoordinateMapperTest {
    @Test
    fun imageRectMapsToFittedScreenRect() {
        val rect = OcrCoordinateMapper.imageToScreenRect(
            imageRect = RectF2(100f, 100f, 300f, 200f),
            imageSize = SizeF2(1000f, 500f),
            viewportSize = SizeF2(500f, 500f),
            transform = ImageTransform(),
        )

        assertEquals(50f, rect.left, 0.001f)
        assertEquals(175f, rect.top, 0.001f)
        assertEquals(150f, rect.right, 0.001f)
        assertEquals(225f, rect.bottom, 0.001f)
    }

    @Test
    fun hitTestUsesZoomAndPan() {
        val imageSize = SizeF2(1000f, 500f)
        val viewportSize = SizeF2(500f, 500f)
        val transform = ImageTransform(zoom = 2f, panX = -100f, panY = 30f)
        val block = RectF2(100f, 100f, 200f, 160f)
        val screenRect = OcrCoordinateMapper.imageToScreenRect(
            imageRect = block,
            imageSize = imageSize,
            viewportSize = viewportSize,
            transform = transform,
        )

        val hit = OcrCoordinateMapper.hitTest(
            screenPoint = PointF2(
                x = (screenRect.left + screenRect.right) / 2f,
                y = (screenRect.top + screenRect.bottom) / 2f,
            ),
            blockRects = listOf(block),
            imageSize = imageSize,
            viewportSize = viewportSize,
            transform = transform,
        )

        assertEquals(0, hit)
    }

    @Test
    fun visibleImageRectIsClampedToImageBounds() {
        val rect = OcrCoordinateMapper.visibleImageRect(
            imageSize = SizeF2(400f, 400f),
            viewportSize = SizeF2(800f, 800f),
            transform = ImageTransform(zoom = 1f, panX = 200f, panY = 200f),
        )

        assertTrue(rect.left >= 0f)
        assertTrue(rect.top >= 0f)
        assertTrue(rect.right <= 400f)
        assertTrue(rect.bottom <= 400f)
    }
}
