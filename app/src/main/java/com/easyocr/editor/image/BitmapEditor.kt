package com.easyocr.editor.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import com.easyocr.editor.geometry.PointF2
import com.easyocr.editor.geometry.RectF2
import com.easyocr.editor.ui.DrawingStroke
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

object BitmapEditor {
    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun crop(bitmap: Bitmap, rect: RectF2): Bitmap {
        val left = rect.left.roundToInt().coerceIn(0, bitmap.width - 1)
        val top = rect.top.roundToInt().coerceIn(0, bitmap.height - 1)
        val right = rect.right.roundToInt().coerceIn(left + 1, bitmap.width)
        val bottom = rect.bottom.roundToInt().coerceIn(top + 1, bitmap.height)
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    fun cropPerspective(bitmap: Bitmap, corners: List<PointF2>): Bitmap {
        require(corners.size == 4) { "Perspective crop requires four corners" }
        val topWidth = corners[0].distanceTo(corners[1])
        val bottomWidth = corners[3].distanceTo(corners[2])
        val leftHeight = corners[0].distanceTo(corners[3])
        val rightHeight = corners[1].distanceTo(corners[2])
        val outputWidth = max(topWidth, bottomWidth).roundToInt().coerceAtLeast(1)
        val outputHeight = max(leftHeight, rightHeight).roundToInt().coerceAtLeast(1)

        val source = floatArrayOf(
            corners[0].x, corners[0].y,
            corners[1].x, corners[1].y,
            corners[2].x, corners[2].y,
            corners[3].x, corners[3].y,
        )
        val destination = floatArrayOf(
            0f, 0f,
            outputWidth.toFloat(), 0f,
            outputWidth.toFloat(), outputHeight.toFloat(),
            0f, outputHeight.toFloat(),
        )
        val matrix = Matrix().apply {
            setPolyToPoly(source, 0, destination, 0, 4)
        }
        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(
            bitmap,
            matrix,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
        return output
    }

    fun drawStrokes(bitmap: Bitmap, strokes: List<DrawingStroke>): Bitmap {
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        strokes.forEach { stroke ->
            if (stroke.points.size < 2) return@forEach
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stroke.color
                strokeWidth = stroke.width
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val path = Path().apply {
                moveTo(stroke.points.first().x, stroke.points.first().y)
                stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            canvas.drawPath(path, paint)
        }
        return output
    }
}

private fun PointF2.distanceTo(other: PointF2): Float =
    hypot(x - other.x, y - other.y)
