package com.easyocr.editor.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easyocr.editor.geometry.ImageTransform
import com.easyocr.editor.geometry.OcrCoordinateMapper
import com.easyocr.editor.geometry.PointF2
import com.easyocr.editor.geometry.RectF2
import com.easyocr.editor.geometry.SizeF2
import com.easyocr.editor.image.BitmapEditor
import com.easyocr.editor.ocr.OcrResult
import com.easyocr.editor.ocr.OcrTextLine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ImageCanvasWithOcrOverlay(
    bitmap: Bitmap,
    ocrResult: OcrResult?,
    overlaysEnabled: Boolean,
    transform: ImageTransform,
    viewportSize: SizeF2,
    activeTool: EditorTool,
    cropState: CropState?,
    cropPreview: Boolean,
    drawingStrokes: List<DrawingStroke>,
    currentStroke: DrawingStroke?,
    penSettings: PenSettings,
    canvasRotation: Float,
    previewScale: Float,
    onViewportChanged: (SizeF2) -> Unit,
    onTransformChanged: (ImageTransform) -> Unit,
    onCropChanged: (CropState) -> Unit,
    onCropPreviewChanged: (Boolean) -> Unit,
    onDrawStart: (PointF2) -> Unit,
    onDrawMove: (PointF2) -> Unit,
    onDrawEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageSize = SizeF2(bitmap.width.toFloat(), bitmap.height.toFloat())
    var magnifierPoint by remember { mutableStateOf<Offset?>(null) }
    val latestCropState by rememberUpdatedState(cropState)
    val latestTransform by rememberUpdatedState(transform)
    val documentPreviewProgress by animateFloatAsState(
        targetValue = if (cropPreview && cropState?.preset == CropPreset.Document) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "documentPreviewProgress",
    )
    val documentPreviewBitmap = remember(bitmap, cropState?.documentCorners, cropPreview) {
        if (cropPreview && cropState?.preset == CropPreset.Document) {
            runCatching { BitmapEditor.cropPerspective(bitmap, cropState.documentCorners) }.getOrNull()
        } else {
            null
        }
    }
    LaunchedEffect(bitmap) {
        onTransformChanged(ImageTransform())
    }

    Box(
        modifier = modifier
            .background(Color(0xFF0F172A))
            .onSizeChanged {
                onViewportChanged(SizeF2(it.width.toFloat(), it.height.toFloat()))
            }
            .viewGestures(
                enabled = activeTool == EditorTool.View,
                imageSize = imageSize,
                viewportSize = viewportSize,
                transformProvider = { latestTransform },
                onTransformChanged = onTransformChanged,
            )
            .cropGestures(
                enabled = activeTool == EditorTool.Crop && cropState != null,
                cropStateProvider = { latestCropState },
                imageSize = imageSize,
                viewportSize = viewportSize,
                transform = transform,
                onCropChanged = onCropChanged,
                onCropPreviewChanged = onCropPreviewChanged,
                onMagnifierPointChanged = { magnifierPoint = it },
            )
            .drawGestures(
                enabled = activeTool == EditorTool.Draw,
                imageSize = imageSize,
                viewportSize = viewportSize,
                transform = transform,
                onDrawStart = onDrawStart,
                onDrawMove = onDrawMove,
                onDrawEnd = onDrawEnd,
            ),
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = canvasRotation
                    scaleX = previewScale
                    scaleY = previewScale
                },
        ) {
            val placement = OcrCoordinateMapper.placement(imageSize, viewportSize, transform)
            val drawnWidth = (bitmap.width * placement.scale).roundToInt()
            val drawnHeight = (bitmap.height * placement.scale).roundToInt()
            drawImage(
                image = bitmap.asImageBitmap(),
                dstOffset = IntOffset(
                    placement.left.roundToInt(),
                    placement.top.roundToInt(),
                ),
                dstSize = IntSize(drawnWidth, drawnHeight),
            )

            drawingStrokes.forEach {
                drawStroke(it, imageSize, viewportSize, transform)
            }
            currentStroke?.let {
                drawStroke(it, imageSize, viewportSize, transform)
            }

            if (overlaysEnabled && activeTool == EditorTool.View) {
                ocrResult?.blocks.orEmpty().forEach { block ->
                    val rect = OcrCoordinateMapper.imageToScreenRect(
                        imageRect = block.boundingBox,
                        imageSize = imageSize,
                        viewportSize = viewportSize,
                        transform = transform,
                    )
                    drawRoundedOverlay(rect)
                }
            }

            if (activeTool == EditorTool.Crop && cropState != null) {
                drawCropOverlay(
                    cropState = cropState,
                    imageSize = imageSize,
                    viewportSize = viewportSize,
                    transform = transform,
                    preview = cropPreview && cropState.preset == CropPreset.Document,
                )
                magnifierPoint?.let {
                    drawMagnifier(
                        bitmap = bitmap,
                        focus = it,
                        imageSize = imageSize,
                        viewportSize = viewportSize,
                        transform = transform,
                    )
                }
                if (documentPreviewProgress > 0f) {
                    documentPreviewBitmap?.let { preview ->
                        drawDocumentMorphPreview(
                            originalBitmap = bitmap,
                            bitmap = preview,
                            cropState = cropState,
                            imageSize = imageSize,
                            viewportSize = viewportSize,
                            transform = transform,
                            progress = documentPreviewProgress,
                        )
                    }
                }
            }

            if (activeTool == EditorTool.Draw) {
                drawCircle(
                    color = Color(penSettings.color),
                    radius = penSettings.width / 2f,
                    center = Offset(size.width - 36f, 36f),
                )
            }
        }

        if (activeTool == EditorTool.View) {
            SelectableOcrTextLayer(
                ocrResult = ocrResult,
                imageSize = imageSize,
                viewportSize = viewportSize,
                transform = transform,
            )
        }
    }
}

private fun Modifier.viewGestures(
    enabled: Boolean,
    imageSize: SizeF2,
    viewportSize: SizeF2,
    transformProvider: () -> ImageTransform,
    onTransformChanged: (ImageTransform) -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(imageSize, viewportSize) {
        detectTransformGestures { centroid, pan, zoom, _ ->
            val current = transformProvider()
            val imagePointAtCentroid = OcrCoordinateMapper.screenToImagePoint(
                screenPoint = PointF2(centroid.x, centroid.y),
                imageSize = imageSize,
                viewportSize = viewportSize,
                transform = current,
            )
            val nextZoom = (current.zoom * zoom).coerceIn(0.6f, 8f)
            val baseTransform = ImageTransform(zoom = nextZoom)
            val basePlacement = OcrCoordinateMapper.placement(imageSize, viewportSize, baseTransform)
            val desiredCentroid = centroid + pan
            val next = ImageTransform(
                zoom = nextZoom,
                panX = desiredCentroid.x - basePlacement.left - imagePointAtCentroid.x * basePlacement.scale,
                panY = desiredCentroid.y - basePlacement.top - imagePointAtCentroid.y * basePlacement.scale,
            ).clamped(imageSize, viewportSize)
            onTransformChanged(next)
        }
    }
}

private fun ImageTransform.clamped(
    imageSize: SizeF2,
    viewportSize: SizeF2,
): ImageTransform {
    if (imageSize.isEmpty || viewportSize.isEmpty) return this
    val fitScale = min(
        viewportSize.width / imageSize.width,
        viewportSize.height / imageSize.height,
    )
    val drawnWidth = imageSize.width * fitScale * zoom
    val drawnHeight = imageSize.height * fitScale * zoom
    val slackX = max(0f, (drawnWidth - viewportSize.width) / 2f) + 72f
    val slackY = max(0f, (drawnHeight - viewportSize.height) / 2f) + 72f
    return copy(
        panX = panX.coerceIn(-slackX, slackX),
        panY = panY.coerceIn(-slackY, slackY),
    )
}

@Composable
private fun SelectableOcrTextLayer(
    ocrResult: OcrResult?,
    imageSize: SizeF2,
    viewportSize: SizeF2,
    transform: ImageTransform,
) {
    val blocks = ocrResult?.blocks.orEmpty()
    val selectableLines = blocks.flatMap { block ->
        block.lines.ifEmpty {
            listOf(
                OcrTextLine(
                    id = "${block.id}-fallback",
                    text = block.text,
                    boundingBox = block.boundingBox,
                ),
            )
        }
    }
    if (selectableLines.isEmpty() || viewportSize.isEmpty) return

    val density = LocalDensity.current
    SelectionContainer {
        Box(Modifier.fillMaxSize()) {
            selectableLines.forEach { line ->
                val rect = OcrCoordinateMapper.imageToScreenRect(
                    imageRect = line.boundingBox,
                    imageSize = imageSize,
                    viewportSize = viewportSize,
                    transform = transform,
                )
                if (rect.width <= 2f || rect.height <= 2f) return@forEach

                val widthDp = with(density) { rect.width.toDp() }
                val heightDp = with(density) { rect.height.toDp() }
                val fontSize = with(density) {
                    val heightBasedSize = rect.height * 0.72f
                    val widthBasedSize = rect.width / max(1f, line.text.length * 0.52f)
                    min(heightBasedSize, widthBasedSize).coerceIn(8f, 30f).toSp()
                }

                Text(
                    text = line.text,
                    color = Color.Transparent,
                    fontSize = fontSize,
                    lineHeight = fontSize,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .absoluteOffset {
                            IntOffset(rect.left.roundToInt(), rect.top.roundToInt())
                        }
                        .size(width = widthDp, height = heightDp),
                )
            }
        }
    }
}

private fun Modifier.cropGestures(
    enabled: Boolean,
    cropStateProvider: () -> CropState?,
    imageSize: SizeF2,
    viewportSize: SizeF2,
    transform: ImageTransform,
    onCropChanged: (CropState) -> Unit,
    onCropPreviewChanged: (Boolean) -> Unit,
    onMagnifierPointChanged: (Offset?) -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(enabled, imageSize, viewportSize, transform) {
        var handle: CropHandle? = null
        var workingState: CropState? = null
        detectDragGestures(
            onDragStart = { offset ->
                val state = cropStateProvider() ?: return@detectDragGestures
                workingState = state
                handle = findCropHandle(offset, state, imageSize, viewportSize, transform)
                onCropPreviewChanged(state.preset == CropPreset.Document)
                onMagnifierPointChanged(if (state.preset == CropPreset.Document) offset else null)
            },
            onDragEnd = {
                handle = null
                workingState = null
                onCropPreviewChanged(false)
                onMagnifierPointChanged(null)
            },
            onDragCancel = {
                handle = null
                workingState = null
                onCropPreviewChanged(false)
                onMagnifierPointChanged(null)
            },
            onDrag = { change, dragAmount ->
                val state = workingState ?: cropStateProvider() ?: return@detectDragGestures
                change.consume()
                onMagnifierPointChanged(
                    if (state.preset == CropPreset.Document) change.position else null,
                )
                val placement = OcrCoordinateMapper.placement(imageSize, viewportSize, transform)
                val dx = dragAmount.x / placement.scale
                val dy = dragAmount.y / placement.scale
                val next = updateCrop(state, handle, dx, dy, imageSize)
                workingState = next
                onCropChanged(next)
            },
        )
    }.pointerInput(enabled, imageSize, viewportSize, transform) {
        detectTapGestures(
            onLongPress = {
                if (cropStateProvider()?.preset == CropPreset.Document) {
                    onCropPreviewChanged(true)
                }
            },
            onPress = {
                tryAwaitRelease()
                if (cropStateProvider()?.preset == CropPreset.Document) {
                    onCropPreviewChanged(false)
                }
            },
        )
    }
}

private fun Modifier.drawGestures(
    enabled: Boolean,
    imageSize: SizeF2,
    viewportSize: SizeF2,
    transform: ImageTransform,
    onDrawStart: (PointF2) -> Unit,
    onDrawMove: (PointF2) -> Unit,
    onDrawEnd: () -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(imageSize, viewportSize, transform) {
        detectDragGestures(
            onDragStart = { offset ->
                onDrawStart(offset.toImagePoint(imageSize, viewportSize, transform))
            },
            onDragEnd = onDrawEnd,
            onDragCancel = onDrawEnd,
            onDrag = { change, _ ->
                change.consume()
                onDrawMove(change.position.toImagePoint(imageSize, viewportSize, transform))
            },
        )
    }
}

private fun Offset.toImagePoint(
    imageSize: SizeF2,
    viewportSize: SizeF2,
    transform: ImageTransform,
): PointF2 =
    OcrCoordinateMapper.screenToImagePoint(
        PointF2(x, y),
        imageSize,
        viewportSize,
        transform,
    ).let {
        PointF2(
            x = it.x.coerceIn(0f, imageSize.width),
            y = it.y.coerceIn(0f, imageSize.height),
        )
    }

private enum class CropHandle {
    Move,
    Left,
    Top,
    Right,
    Bottom,
    TopLeft,
    TopRight,
    BottomRight,
    BottomLeft,
    DocumentTopLeft,
    DocumentTopRight,
    DocumentBottomRight,
    DocumentBottomLeft,
}

private fun findCropHandle(
    offset: Offset,
    cropState: CropState,
    imageSize: SizeF2,
    viewportSize: SizeF2,
    transform: ImageTransform,
): CropHandle {
    val threshold = 64f
    if (cropState.preset == CropPreset.Document) {
        cropState.documentCorners.forEachIndexed { index, point ->
            val screen = OcrCoordinateMapper.imageToScreenPoint(point, imageSize, viewportSize, transform)
            if (abs(screen.x - offset.x) < threshold && abs(screen.y - offset.y) < threshold) {
                return when (index) {
                    0 -> CropHandle.DocumentTopLeft
                    1 -> CropHandle.DocumentTopRight
                    2 -> CropHandle.DocumentBottomRight
                    else -> CropHandle.DocumentBottomLeft
                }
            }
        }
    }

    val rect = OcrCoordinateMapper.imageToScreenRect(cropState.rect, imageSize, viewportSize, transform)
    val nearLeft = abs(offset.x - rect.left) < threshold
    val nearRight = abs(offset.x - rect.right) < threshold
    val nearTop = abs(offset.y - rect.top) < threshold
    val nearBottom = abs(offset.y - rect.bottom) < threshold
    return when {
        nearLeft && nearTop -> CropHandle.TopLeft
        nearRight && nearTop -> CropHandle.TopRight
        nearRight && nearBottom -> CropHandle.BottomRight
        nearLeft && nearBottom -> CropHandle.BottomLeft
        nearLeft -> CropHandle.Left
        nearRight -> CropHandle.Right
        nearTop -> CropHandle.Top
        nearBottom -> CropHandle.Bottom
        else -> CropHandle.Move
    }
}

private fun updateCrop(
    state: CropState,
    handle: CropHandle?,
    dx: Float,
    dy: Float,
    imageSize: SizeF2,
): CropState {
    if (handle == null) return state
    if (state.preset == CropPreset.Document && handle.name.startsWith("Document")) {
        val index = when (handle) {
            CropHandle.DocumentTopLeft -> 0
            CropHandle.DocumentTopRight -> 1
            CropHandle.DocumentBottomRight -> 2
            CropHandle.DocumentBottomLeft -> 3
            else -> 0
        }
        val corners = state.documentCorners.toMutableList()
        val current = corners[index]
        corners[index] = PointF2(
            x = (current.x + dx).coerceIn(0f, imageSize.width),
            y = (current.y + dy).coerceIn(0f, imageSize.height),
        )
        return state.copy(
            documentCorners = corners,
            rect = boundingRect(corners, imageSize),
        )
    }

    val rawRect = when (handle) {
        CropHandle.Move -> RectF2(
            state.rect.left + dx,
            state.rect.top + dy,
            state.rect.right + dx,
            state.rect.bottom + dy,
        ).moveInside(imageSize)
        CropHandle.Left -> state.rect.copy(left = state.rect.left + dx)
        CropHandle.Top -> state.rect.copy(top = state.rect.top + dy)
        CropHandle.Right -> state.rect.copy(right = state.rect.right + dx)
        CropHandle.Bottom -> state.rect.copy(bottom = state.rect.bottom + dy)
        CropHandle.TopLeft -> state.rect.copy(left = state.rect.left + dx, top = state.rect.top + dy)
        CropHandle.TopRight -> state.rect.copy(right = state.rect.right + dx, top = state.rect.top + dy)
        CropHandle.BottomRight -> state.rect.copy(right = state.rect.right + dx, bottom = state.rect.bottom + dy)
        CropHandle.BottomLeft -> state.rect.copy(left = state.rect.left + dx, bottom = state.rect.bottom + dy)
        else -> state.rect
    }
    val rect = if (state.preset.aspectRatio != null && handle != CropHandle.Move) {
        constrainToAspectRatio(
            original = state.rect,
            changed = rawRect,
            handle = handle,
            aspectRatio = state.preset.aspectRatio,
            imageSize = imageSize,
        )
    } else {
        rawRect.normalized().clampTo(imageSize).ensureMinSize(imageSize)
    }
    return state.copy(rect = rect, documentCorners = cornersFor(rect))
}

private fun constrainToAspectRatio(
    original: RectF2,
    changed: RectF2,
    handle: CropHandle,
    aspectRatio: Float,
    imageSize: SizeF2,
): RectF2 {
    val minSize = 24f
    val originalWidth = original.width.coerceAtLeast(minSize)
    val originalHeight = original.height.coerceAtLeast(minSize)
    val requestedWidth = changed.normalized().width.coerceAtLeast(minSize)
    val requestedHeight = changed.normalized().height.coerceAtLeast(minSize)
    val widthDriven = when (handle) {
        CropHandle.Left,
        CropHandle.Right,
        -> true
        CropHandle.Top,
        CropHandle.Bottom,
        -> false
        else -> requestedWidth / originalWidth >= requestedHeight / originalHeight
    }

    var width = if (widthDriven) requestedWidth else requestedHeight * aspectRatio
    var height = width / aspectRatio
    if (height < minSize) {
        height = minSize
        width = height * aspectRatio
    }

    val anchoredLeft = handle in setOf(CropHandle.Right, CropHandle.TopRight, CropHandle.BottomRight)
    val anchoredRight = handle in setOf(CropHandle.Left, CropHandle.TopLeft, CropHandle.BottomLeft)
    val anchoredTop = handle in setOf(CropHandle.Bottom, CropHandle.BottomLeft, CropHandle.BottomRight)
    val anchoredBottom = handle in setOf(CropHandle.Top, CropHandle.TopLeft, CropHandle.TopRight)

    var left = when {
        anchoredLeft -> original.left
        anchoredRight -> original.right - width
        else -> original.left + (originalWidth - width) / 2f
    }
    var top = when {
        anchoredTop -> original.top
        anchoredBottom -> original.bottom - height
        else -> original.top + (originalHeight - height) / 2f
    }

    if (left < 0f) left = 0f
    if (top < 0f) top = 0f
    if (left + width > imageSize.width) left = imageSize.width - width
    if (top + height > imageSize.height) top = imageSize.height - height
    left = left.coerceAtLeast(0f)
    top = top.coerceAtLeast(0f)
    width = min(width, imageSize.width - left)
    height = width / aspectRatio
    if (top + height > imageSize.height) {
        height = imageSize.height - top
        width = height * aspectRatio
    }

    return RectF2(left, top, left + width, top + height)
        .normalized()
        .clampTo(imageSize)
        .ensureMinSize(imageSize)
}

private fun RectF2.moveInside(imageSize: SizeF2): RectF2 {
    val width = width
    val height = height
    val left = left.coerceIn(0f, imageSize.width - width)
    val top = top.coerceIn(0f, imageSize.height - height)
    return RectF2(left, top, left + width, top + height)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundedOverlay(rect: RectF2) {
    val topLeft = Offset(rect.left, rect.top)
    val size = Size(rect.width, rect.height)
    val radius = CornerRadius(8f, 8f)
    drawRoundRect(
        color = Color(0x3332C7B7),
        topLeft = topLeft,
        size = size,
        cornerRadius = radius,
    )
    drawRoundRect(
        color = Color(0xCC14B8A6),
        topLeft = topLeft,
        size = size,
        cornerRadius = radius,
        style = Stroke(width = 2f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(
    stroke: DrawingStroke,
    imageSize: SizeF2,
    viewportSize: SizeF2,
    transform: ImageTransform,
) {
    if (stroke.points.size < 2) return
    val path = Path()
    stroke.points.forEachIndexed { index, point ->
        val screen = OcrCoordinateMapper.imageToScreenPoint(point, imageSize, viewportSize, transform)
        if (index == 0) {
            path.moveTo(screen.x, screen.y)
        } else {
            path.lineTo(screen.x, screen.y)
        }
    }
    drawPath(
        path = path,
        color = Color(stroke.color),
        style = Stroke(width = stroke.width, cap = androidx.compose.ui.graphics.StrokeCap.Round),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCropOverlay(
    cropState: CropState,
    imageSize: SizeF2,
    viewportSize: SizeF2,
    transform: ImageTransform,
    preview: Boolean,
) {
    val rect = OcrCoordinateMapper.imageToScreenRect(cropState.rect, imageSize, viewportSize, transform)
    if (!preview) {
        drawRect(Color(0x99000000), topLeft = Offset.Zero, size = Size(size.width, rect.top.coerceAtLeast(0f)))
        drawRect(
            Color(0x99000000),
            topLeft = Offset(0f, rect.bottom),
            size = Size(size.width, (size.height - rect.bottom).coerceAtLeast(0f)),
        )
        drawRect(
            Color(0x99000000),
            topLeft = Offset(0f, rect.top),
            size = Size(rect.left.coerceAtLeast(0f), rect.height),
        )
        drawRect(
            Color(0x99000000),
            topLeft = Offset(rect.right, rect.top),
            size = Size((size.width - rect.right).coerceAtLeast(0f), rect.height),
        )
    }
    drawRoundRect(
        color = Color(0xFFFFFFFF),
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(10f, 10f),
        style = Stroke(width = 3f),
    )

    if (cropState.preset == CropPreset.Document) {
        val corners = cropState.documentCorners.map {
            OcrCoordinateMapper.imageToScreenPoint(it, imageSize, viewportSize, transform)
        }
        val path = Path().apply {
            corners.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
            close()
        }
        drawPath(path, Color(0x2238BDF8))
        drawPath(path, Color(0xFF38BDF8), style = Stroke(width = 4f))
        corners.forEach { drawHandle(it, Color(0xFF38BDF8)) }
    } else {
        cornersFor(cropState.rect).forEach {
            drawHandle(OcrCoordinateMapper.imageToScreenPoint(it, imageSize, viewportSize, transform), Color.White)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandle(
    point: PointF2,
    color: Color,
) {
    drawCircle(color = Color(0x99000000), radius = 16f, center = Offset(point.x, point.y))
    drawCircle(color = color, radius = 10f, center = Offset(point.x, point.y))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMagnifier(
    bitmap: Bitmap,
    focus: Offset,
    imageSize: SizeF2,
    viewportSize: SizeF2,
    transform: ImageTransform,
) {
    val imagePoint = OcrCoordinateMapper.screenToImagePoint(
        PointF2(focus.x, focus.y),
        imageSize,
        viewportSize,
        transform,
    )
    val sourceSize = 96
    val actualSourceWidth = sourceSize.coerceAtMost(bitmap.width)
    val actualSourceHeight = sourceSize.coerceAtMost(bitmap.height)
    val srcLeft = (imagePoint.x.roundToInt() - actualSourceWidth / 2)
        .coerceIn(0, bitmap.width - actualSourceWidth)
    val srcTop = (imagePoint.y.roundToInt() - actualSourceHeight / 2)
        .coerceIn(0, bitmap.height - actualSourceHeight)
    val lensSize = 144
    val lensLeft = if (focus.x < size.width / 2f) size.width - lensSize - 20f else 20f
    val lensTop = 20f
    drawCircle(
        color = Color(0xDD0F172A),
        radius = lensSize / 2f + 5f,
        center = Offset(lensLeft + lensSize / 2f, lensTop + lensSize / 2f),
    )
    drawImage(
        image = bitmap.asImageBitmap(),
        srcOffset = IntOffset(srcLeft, srcTop),
        srcSize = IntSize(actualSourceWidth, actualSourceHeight),
        dstOffset = IntOffset(lensLeft.roundToInt(), lensTop.roundToInt()),
        dstSize = IntSize(lensSize, lensSize),
    )
    drawCircle(
        color = Color.White,
        radius = lensSize / 2f,
        center = Offset(lensLeft + lensSize / 2f, lensTop + lensSize / 2f),
        style = Stroke(width = 3f),
    )
    drawLine(
        color = Color.White,
        start = Offset(lensLeft + lensSize / 2f - 18f, lensTop + lensSize / 2f),
        end = Offset(lensLeft + lensSize / 2f + 18f, lensTop + lensSize / 2f),
        strokeWidth = 2f,
    )
    drawLine(
        color = Color.White,
        start = Offset(lensLeft + lensSize / 2f, lensTop + lensSize / 2f - 18f),
        end = Offset(lensLeft + lensSize / 2f, lensTop + lensSize / 2f + 18f),
        strokeWidth = 2f,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDocumentMorphPreview(
    originalBitmap: Bitmap,
    bitmap: Bitmap,
    cropState: CropState,
    imageSize: SizeF2,
    viewportSize: SizeF2,
    transform: ImageTransform,
    progress: Float,
) {
    val eased = progress.coerceIn(0f, 1f)
    val originalCorners = cropState.documentCorners.map {
        OcrCoordinateMapper.imageToScreenPoint(it, imageSize, viewportSize, transform)
    }
    val targetMaxWidth = size.width * 0.86f
    val targetMaxHeight = size.height * 0.72f
    val targetScale = min(targetMaxWidth / bitmap.width, targetMaxHeight / bitmap.height)
    val targetWidth = bitmap.width * targetScale
    val targetHeight = bitmap.height * targetScale
    val targetLeft = (size.width - targetWidth) / 2f
    val targetTop = (size.height - targetHeight) / 2f
    val targetCorners = listOf(
        PointF2(targetLeft, targetTop),
        PointF2(targetLeft + targetWidth, targetTop),
        PointF2(targetLeft + targetWidth, targetTop + targetHeight),
        PointF2(targetLeft, targetTop + targetHeight),
    )
    val startMatrix = Matrix().apply {
        setPolyToPoly(
            floatArrayOf(
                0f, 0f,
                bitmap.width.toFloat(), 0f,
                bitmap.width.toFloat(), bitmap.height.toFloat(),
                0f, bitmap.height.toFloat(),
            ),
            0,
            floatArrayOf(
                originalCorners[0].x, originalCorners[0].y,
                originalCorners[1].x, originalCorners[1].y,
                originalCorners[2].x, originalCorners[2].y,
                originalCorners[3].x, originalCorners[3].y,
            ),
            0,
            4,
        )
    }

    val meshWidth = 18
    val meshHeight = 18
    val verts = FloatArray((meshWidth + 1) * (meshHeight + 1) * 2)
    val mapped = FloatArray(2)
    var index = 0
    for (y in 0..meshHeight) {
        val v = y / meshHeight.toFloat()
        for (x in 0..meshWidth) {
            val u = x / meshWidth.toFloat()
            mapped[0] = u * bitmap.width
            mapped[1] = v * bitmap.height
            startMatrix.mapPoints(mapped)
            val targetX = targetLeft + u * targetWidth
            val targetY = targetTop + v * targetHeight
            verts[index++] = lerp(mapped[0], targetX, eased)
            verts[index++] = lerp(mapped[1], targetY, eased)
        }
    }

    drawRect(Color(0x660F172A), topLeft = Offset.Zero, size = size)
    drawIntoCanvas {
        it.nativeCanvas.drawBitmapMesh(
            bitmap,
            meshWidth,
            meshHeight,
            verts,
            0,
            null,
            0,
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG),
        )
    }

    val outline = Path().apply {
        val first = interpolatedCorner(originalCorners[0], targetCorners[0], eased)
        moveTo(first.x, first.y)
        for (i in 1..3) {
            val point = interpolatedCorner(originalCorners[i], targetCorners[i], eased)
            lineTo(point.x, point.y)
        }
        close()
    }
    drawPath(outline, Color.White, style = Stroke(width = 2.5f))

    if (eased < 0.04f) {
        val placement = OcrCoordinateMapper.placement(imageSize, viewportSize, transform)
        drawImage(
            image = originalBitmap.asImageBitmap(),
            dstOffset = IntOffset(
                placement.left.roundToInt(),
                placement.top.roundToInt(),
            ),
            dstSize = IntSize(
                (originalBitmap.width * placement.scale).roundToInt(),
                (originalBitmap.height * placement.scale).roundToInt(),
            ),
            alpha = 1f - eased / 0.04f,
        )
    }
}

private fun interpolatedCorner(
    start: PointF2,
    end: PointF2,
    fraction: Float,
): PointF2 =
    PointF2(
        x = lerp(start.x, end.x, fraction),
        y = lerp(start.y, end.y, fraction),
    )

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
