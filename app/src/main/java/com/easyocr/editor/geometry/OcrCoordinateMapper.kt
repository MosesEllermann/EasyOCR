package com.easyocr.editor.geometry

import kotlin.math.max
import kotlin.math.min

object OcrCoordinateMapper {
    fun imageToScreenRect(
        imageRect: RectF2,
        imageSize: SizeF2,
        viewportSize: SizeF2,
        transform: ImageTransform,
    ): RectF2 {
        val placement = placement(imageSize, viewportSize, transform)
        return RectF2(
            left = placement.left + imageRect.left * placement.scale,
            top = placement.top + imageRect.top * placement.scale,
            right = placement.left + imageRect.right * placement.scale,
            bottom = placement.top + imageRect.bottom * placement.scale,
        )
    }

    fun screenToImagePoint(
        screenPoint: PointF2,
        imageSize: SizeF2,
        viewportSize: SizeF2,
        transform: ImageTransform,
    ): PointF2 {
        val placement = placement(imageSize, viewportSize, transform)
        return PointF2(
            x = (screenPoint.x - placement.left) / placement.scale,
            y = (screenPoint.y - placement.top) / placement.scale,
        )
    }

    fun imageToScreenPoint(
        imagePoint: PointF2,
        imageSize: SizeF2,
        viewportSize: SizeF2,
        transform: ImageTransform,
    ): PointF2 {
        val placement = placement(imageSize, viewportSize, transform)
        return PointF2(
            x = placement.left + imagePoint.x * placement.scale,
            y = placement.top + imagePoint.y * placement.scale,
        )
    }

    fun visibleImageRect(
        imageSize: SizeF2,
        viewportSize: SizeF2,
        transform: ImageTransform,
    ): RectF2 {
        val topLeft = screenToImagePoint(PointF2(0f, 0f), imageSize, viewportSize, transform)
        val bottomRight = screenToImagePoint(
            PointF2(viewportSize.width, viewportSize.height),
            imageSize,
            viewportSize,
            transform,
        )
        return RectF2(
            left = min(topLeft.x, bottomRight.x),
            top = min(topLeft.y, bottomRight.y),
            right = max(topLeft.x, bottomRight.x),
            bottom = max(topLeft.y, bottomRight.y),
        ).clampTo(imageSize)
    }

    fun hitTest(
        screenPoint: PointF2,
        blockRects: List<RectF2>,
        imageSize: SizeF2,
        viewportSize: SizeF2,
        transform: ImageTransform,
    ): Int? {
        val imagePoint = screenToImagePoint(screenPoint, imageSize, viewportSize, transform)
        return blockRects.indexOfFirst { it.contains(imagePoint) }.takeIf { it >= 0 }
    }

    fun placement(
        imageSize: SizeF2,
        viewportSize: SizeF2,
        transform: ImageTransform,
    ): ImagePlacement {
        if (imageSize.isEmpty || viewportSize.isEmpty) {
            return ImagePlacement(0f, 0f, 1f)
        }
        val fitScale = min(
            viewportSize.width / imageSize.width,
            viewportSize.height / imageSize.height,
        )
        val scale = fitScale * transform.zoom.coerceAtLeast(0.1f)
        val drawnWidth = imageSize.width * scale
        val drawnHeight = imageSize.height * scale
        return ImagePlacement(
            left = (viewportSize.width - drawnWidth) / 2f + transform.panX,
            top = (viewportSize.height - drawnHeight) / 2f + transform.panY,
            scale = scale,
        )
    }
}

data class ImagePlacement(
    val left: Float,
    val top: Float,
    val scale: Float,
)
