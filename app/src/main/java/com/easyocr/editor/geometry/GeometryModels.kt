package com.easyocr.editor.geometry

data class PointF2(
    val x: Float,
    val y: Float,
)

data class SizeF2(
    val width: Float,
    val height: Float,
) {
    val isEmpty: Boolean = width <= 0f || height <= 0f
}

data class RectF2(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float = right - left
    val height: Float = bottom - top

    fun contains(point: PointF2): Boolean =
        point.x in left..right && point.y in top..bottom

    fun clampTo(bounds: SizeF2): RectF2 {
        val clampedLeft = left.coerceIn(0f, bounds.width)
        val clampedTop = top.coerceIn(0f, bounds.height)
        val clampedRight = right.coerceIn(clampedLeft, bounds.width)
        val clampedBottom = bottom.coerceIn(clampedTop, bounds.height)
        return RectF2(clampedLeft, clampedTop, clampedRight, clampedBottom)
    }
}

data class ImageTransform(
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
)
