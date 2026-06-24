package com.easyocr.editor.ui

import com.easyocr.editor.geometry.PointF2
import com.easyocr.editor.geometry.RectF2
import com.easyocr.editor.geometry.SizeF2
import kotlin.math.max
import kotlin.math.min

enum class EditorTool {
    View,
    Crop,
    Draw,
}

enum class CropPreset(
    val label: String,
    val aspectRatio: Float?,
) {
    Free("Free", null),
    Square("1:1", 1f),
    Portrait("4:5", 4f / 5f),
    Landscape("16:9", 16f / 9f),
    Story("9:16", 9f / 16f),
    Document("Document", null),
}

data class CropState(
    val rect: RectF2,
    val preset: CropPreset = CropPreset.Free,
    val documentCorners: List<PointF2> = cornersFor(rect),
) {
    companion object {
        fun initial(imageSize: SizeF2): CropState {
            val horizontalInset = imageSize.width * 0.08f
            val verticalInset = imageSize.height * 0.08f
            val rect = RectF2(
                left = horizontalInset,
                top = verticalInset,
                right = imageSize.width - horizontalInset,
                bottom = imageSize.height - verticalInset,
            )
            return CropState(rect = rect)
        }
    }
}

data class PenSettings(
    val color: Int = 0xFFF59E0B.toInt(),
    val width: Float = 10f,
)

data class DrawingStroke(
    val points: List<PointF2>,
    val color: Int,
    val width: Float,
)

fun cornersFor(rect: RectF2): List<PointF2> =
    listOf(
        PointF2(rect.left, rect.top),
        PointF2(rect.right, rect.top),
        PointF2(rect.right, rect.bottom),
        PointF2(rect.left, rect.bottom),
    )

fun boundingRect(points: List<PointF2>, imageSize: SizeF2): RectF2 {
    val left = points.minOfOrNull { it.x } ?: 0f
    val top = points.minOfOrNull { it.y } ?: 0f
    val right = points.maxOfOrNull { it.x } ?: imageSize.width
    val bottom = points.maxOfOrNull { it.y } ?: imageSize.height
    return RectF2(left, top, right, bottom).normalized().clampTo(imageSize).ensureMinSize(imageSize)
}

fun RectF2.normalized(): RectF2 =
    RectF2(
        left = min(left, right),
        top = min(top, bottom),
        right = max(left, right),
        bottom = max(top, bottom),
    )

fun RectF2.ensureMinSize(imageSize: SizeF2, minSize: Float = 24f): RectF2 {
    val normalized = normalized().clampTo(imageSize)
    val right = if (normalized.width < minSize) {
        (normalized.left + minSize).coerceAtMost(imageSize.width)
    } else {
        normalized.right
    }
    val bottom = if (normalized.height < minSize) {
        (normalized.top + minSize).coerceAtMost(imageSize.height)
    } else {
        normalized.bottom
    }
    return normalized.copy(right = right, bottom = bottom)
}

fun cropRectForPreset(
    imageSize: SizeF2,
    preset: CropPreset,
): CropState {
    if (preset.aspectRatio == null) {
        return CropState.initial(imageSize).copy(preset = preset)
    }
    val imageRatio = imageSize.width / imageSize.height
    val width: Float
    val height: Float
    if (imageRatio > preset.aspectRatio) {
        height = imageSize.height * 0.86f
        width = height * preset.aspectRatio
    } else {
        width = imageSize.width * 0.86f
        height = width / preset.aspectRatio
    }
    val left = (imageSize.width - width) / 2f
    val top = (imageSize.height - height) / 2f
    val rect = RectF2(left, top, left + width, top + height)
    return CropState(rect = rect, preset = preset, documentCorners = cornersFor(rect))
}
