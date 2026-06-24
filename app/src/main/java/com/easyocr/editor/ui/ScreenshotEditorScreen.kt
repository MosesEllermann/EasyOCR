package com.easyocr.editor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RotateLeft
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Subject
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import com.easyocr.editor.EditorUiState
import com.easyocr.editor.clipboard.ClipboardActions
import com.easyocr.editor.geometry.ImageTransform
import com.easyocr.editor.geometry.SizeF2
import com.easyocr.editor.ocr.OcrLanguage
import com.easyocr.editor.privacy.PrivacyScreen
import kotlinx.coroutines.launch
import kotlin.math.min

private val WorkbenchBackground = Color(0xFF0F172A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotEditorScreen(
    state: EditorUiState,
    onOpenPicker: () -> Unit,
    onToggleOverlays: () -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onCrop: (CropState) -> Unit,
    onApplyDrawing: (List<DrawingStroke>) -> Unit,
    onSaveCopy: () -> Unit,
    onShare: () -> Unit,
    onShowMessageConsumed: () -> Unit,
    onCopyAll: () -> Unit,
    onRerunOcr: () -> Unit,
    onLanguageSelected: (OcrLanguage) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardActions = ClipboardActions(LocalClipboardManager.current)
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }
    val previewScale = remember { Animatable(1f) }
    val drawingStrokes = remember { mutableStateListOf<DrawingStroke>() }
    var showOcrSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var transform by remember { mutableStateOf(ImageTransform()) }
    var viewportSize by remember { mutableStateOf(SizeF2(0f, 0f)) }
    var activeTool by remember { mutableStateOf(EditorTool.View) }
    var cropState by remember { mutableStateOf<CropState?>(null) }
    var cropPreview by remember { mutableStateOf(false) }
    var penSettings by remember { mutableStateOf(PenSettings()) }
    var currentStroke by remember { mutableStateOf<DrawingStroke?>(null) }

    LaunchedEffect(state.bitmap) {
        previewScale.snapTo(0.96f)
        previewScale.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
        drawingStrokes.clear()
        currentStroke = null
    }

    LaunchedEffect(state.errorMessage, state.lastSavedUri) {
        val message = state.errorMessage ?: state.lastSavedUri?.let { "Saved copy" }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onShowMessageConsumed()
        }
    }

    fun copyText(text: String) {
        if (clipboardActions.copy(text)) {
            scope.launch { snackbarHostState.showSnackbar("Copied") }
        }
    }

    fun startCrop() {
        val bitmap = state.bitmap ?: return
        val imageSize = SizeF2(bitmap.width.toFloat(), bitmap.height.toFloat())
        cropState = cropState ?: CropState.initial(imageSize)
        activeTool = EditorTool.Crop
    }

    fun startDrawing() {
        activeTool = EditorTool.Draw
    }

    fun rotateAnimated(direction: Float, rotate: () -> Unit) {
        scope.launch {
            val currentBitmap = state.bitmap
            val targetScale = if (currentBitmap != null && !viewportSize.isEmpty) {
                val oldFit = min(
                    viewportSize.width / currentBitmap.width.toFloat(),
                    viewportSize.height / currentBitmap.height.toFloat(),
                )
                val newFit = min(
                    viewportSize.width / currentBitmap.height.toFloat(),
                    viewportSize.height / currentBitmap.width.toFloat(),
                )
                (newFit / oldFit).coerceIn(0.55f, 1.8f)
            } else {
                1f
            }
            rotation.snapTo(0f)
            previewScale.snapTo(1f)
            launch {
                previewScale.animateTo(targetScale, tween(durationMillis = 190, easing = FastOutSlowInEasing))
            }
            rotation.animateTo(direction * 90f, tween(durationMillis = 190, easing = FastOutSlowInEasing))
            rotate()
            rotation.snapTo(0f)
            previewScale.snapTo(1f)
        }
    }

    Scaffold(
        containerColor = WorkbenchBackground,
        topBar = {
            TopAppBar(
                title = { Text("EasyOCR", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WorkbenchBackground,
                    scrolledContainerColor = WorkbenchBackground,
                    actionIconContentColor = Color.White,
                    titleContentColor = Color.White,
                ),
                actions = {
                    IconButton(onClick = onToggleOverlays, enabled = state.hasImage) {
                        Icon(
                            imageVector = if (state.overlaysEnabled) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = "Toggle text boxes",
                        )
                    }
                    IconButton(onClick = { showPrivacySheet = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = "Privacy")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WorkbenchBackground)
                .padding(padding),
        ) {
            val bitmap = state.bitmap
            val imageBottomPadding = when (activeTool) {
                EditorTool.View -> 104.dp
                EditorTool.Crop,
                EditorTool.Draw,
                -> 232.dp
            }
            when {
                bitmap != null -> ImageCanvasWithOcrOverlay(
                    bitmap = bitmap,
                    ocrResult = state.ocrResult,
                    overlaysEnabled = state.overlaysEnabled,
                    transform = transform,
                    viewportSize = viewportSize,
                    activeTool = activeTool,
                    cropState = cropState,
                    cropPreview = cropPreview,
                    drawingStrokes = drawingStrokes,
                    currentStroke = currentStroke,
                    penSettings = penSettings,
                    canvasRotation = rotation.value,
                    previewScale = previewScale.value,
                    onViewportChanged = { viewportSize = it },
                    onTransformChanged = { transform = it },
                    onCropChanged = { cropState = it },
                    onCropPreviewChanged = { cropPreview = it },
                    onDrawStart = { point ->
                        currentStroke = DrawingStroke(
                            points = listOf(point),
                            color = penSettings.color,
                            width = penSettings.width,
                        )
                    },
                    onDrawMove = { point ->
                        currentStroke = currentStroke?.copy(
                            points = currentStroke?.points.orEmpty() + point,
                        )
                    },
                    onDrawEnd = {
                        currentStroke?.takeIf { it.points.size > 1 }?.let(drawingStrokes::add)
                        currentStroke = null
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = imageBottomPadding),
                )

                state.isLoadingImage -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> EmptyState(
                    onOpenPicker = onOpenPicker,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            EditorFloatingControls(
                enabled = state.hasImage,
                activeTool = activeTool,
                cropState = cropState,
                penSettings = penSettings,
                hasDrawing = drawingStrokes.isNotEmpty() || currentStroke != null,
                onCrop = ::startCrop,
                onDraw = ::startDrawing,
                onRotateLeft = { rotateAnimated(-1f, onRotateLeft) },
                onRotateRight = { rotateAnimated(1f, onRotateRight) },
                onSaveCopy = onSaveCopy,
                onShare = onShare,
                onOcrText = { showOcrSheet = true },
                onCropPreset = { preset ->
                    val current = state.bitmap ?: return@EditorFloatingControls
                    cropState = cropRectForPreset(
                        imageSize = SizeF2(current.width.toFloat(), current.height.toFloat()),
                        preset = preset,
                    )
                },
                onApplyCrop = {
                    cropState?.let {
                        onCrop(it)
                        activeTool = EditorTool.View
                        cropState = null
                    }
                },
                onCancelCrop = {
                    activeTool = EditorTool.View
                    cropState = null
                    cropPreview = false
                },
                onPenSettings = { penSettings = it },
                onApplyDrawing = {
                    val strokes = drawingStrokes.toList()
                    onApplyDrawing(strokes)
                    drawingStrokes.clear()
                    currentStroke = null
                    activeTool = EditorTool.View
                },
                onCancelDrawing = {
                    drawingStrokes.clear()
                    currentStroke = null
                    activeTool = EditorTool.View
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            AnimatedVisibility(
                visible = state.isRunningOcr,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    tonalElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                        Text("Running OCR")
                    }
                }
            }
        }
    }

    if (showOcrSheet) {
        OcrBottomSheet(
            fullText = state.fullText,
            language = state.language,
            onLanguageSelected = onLanguageSelected,
            onCopyAll = {
                copyText(state.fullText)
                onCopyAll()
            },
            onRerunOcr = onRerunOcr,
            onDismiss = { showOcrSheet = false },
        )
    }

    if (showPrivacySheet) {
        PrivacyScreen(onDismiss = { showPrivacySheet = false })
    }
}

@Composable
private fun EmptyState(
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.FileOpen,
            contentDescription = null,
            tint = Color.White,
        )
        Button(onClick = onOpenPicker) {
            Text("Choose image")
        }
    }
}

@Composable
private fun EditorFloatingControls(
    enabled: Boolean,
    activeTool: EditorTool,
    cropState: CropState?,
    penSettings: PenSettings,
    hasDrawing: Boolean,
    onCrop: () -> Unit,
    onDraw: () -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onSaveCopy: () -> Unit,
    onShare: () -> Unit,
    onOcrText: () -> Unit,
    onCropPreset: (CropPreset) -> Unit,
    onApplyCrop: () -> Unit,
    onCancelCrop: () -> Unit,
    onPenSettings: (PenSettings) -> Unit,
    onApplyDrawing: () -> Unit,
    onCancelDrawing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedVisibility(
            visible = activeTool == EditorTool.Crop,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            FloatingPanel {
                CropControlPanel(
                    cropState = cropState,
                    onPreset = onCropPreset,
                    onApply = onApplyCrop,
                    onCancel = onCancelCrop,
                )
            }
        }

        AnimatedVisibility(
            visible = activeTool == EditorTool.Draw,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            FloatingPanel {
                DrawControlPanel(
                    penSettings = penSettings,
                    hasDrawing = hasDrawing,
                    onPenSettings = onPenSettings,
                    onApply = onApplyDrawing,
                    onCancel = onCancelDrawing,
                )
            }
        }

        Surface(
            shape = CircleShape,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 344.dp)
                    .padding(horizontal = 7.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FloatingToolButton(selected = activeTool == EditorTool.Crop, enabled = enabled, onClick = onCrop) {
                    Icon(Icons.Outlined.Crop, contentDescription = "Crop")
                }
                FloatingToolButton(selected = activeTool == EditorTool.Draw, enabled = enabled, onClick = onDraw) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Draw")
                }
                CompactIconButton(onClick = onRotateLeft, enabled = enabled) {
                    Icon(Icons.Outlined.RotateLeft, contentDescription = "Rotate left")
                }
                CompactIconButton(onClick = onRotateRight, enabled = enabled) {
                    Icon(Icons.Outlined.RotateRight, contentDescription = "Rotate right")
                }
                CompactIconButton(onClick = onSaveCopy, enabled = enabled) {
                    Icon(Icons.Outlined.SaveAlt, contentDescription = "Save copy")
                }
                CompactIconButton(onClick = onShare, enabled = enabled) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share")
                }
                CompactIconButton(onClick = onOcrText, enabled = enabled) {
                    Icon(Icons.Outlined.Subject, contentDescription = "Full OCR text")
                }
            }
        }
    }
}

@Composable
private fun FloatingPanel(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 620.dp),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        content = { Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) { content() } },
    )
}

@Composable
private fun FloatingToolButton(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    ) {
        CompactIconButton(onClick = onClick, enabled = enabled, content = content)
    }
}

@Composable
private fun CompactIconButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(42.dp),
        content = content,
    )
}

@Composable
private fun CropControlPanel(
    cropState: CropState?,
    onPreset: (CropPreset) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CropPreset.entries.forEach { preset ->
                FilterChip(
                    selected = cropState?.preset == preset,
                    onClick = { onPreset(preset) },
                    label = { Text(preset.label) },
                    leadingIcon = if (preset == CropPreset.Document) {
                        { Icon(Icons.Outlined.CropFree, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) {
                Icon(Icons.Outlined.Close, contentDescription = null)
                Text("Cancel")
            }
            Button(onClick = onApply) {
                Icon(Icons.Outlined.Check, contentDescription = null)
                Text("Apply")
            }
        }
    }
}

@Composable
private fun DrawControlPanel(
    penSettings: PenSettings,
    hasDrawing: Boolean,
    onPenSettings: (PenSettings) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = listOf(
        0xFFF59E0B.toInt(),
        0xFF14B8A6.toInt(),
        0xFF2563EB.toInt(),
        0xFFEF4444.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF111827.toInt(),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.ColorLens, contentDescription = null)
            colors.forEach { color ->
                ColorSwatch(
                    color = color,
                    selected = penSettings.color == color,
                    onClick = { onPenSettings(penSettings.copy(color = color)) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Size")
            Slider(
                value = penSettings.width,
                onValueChange = { onPenSettings(penSettings.copy(width = it)) },
                valueRange = 3f..36f,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onCancel) {
                Icon(Icons.Outlined.Close, contentDescription = null)
                Text("Cancel")
            }
            Button(onClick = onApply, enabled = hasDrawing) {
                Icon(Icons.Outlined.Check, contentDescription = null)
                Text("Apply")
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(42.dp)) {
        Box(
            modifier = Modifier
                .size(if (selected) 34.dp else 30.dp)
                .clip(CircleShape)
                .background(Color(color))
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                ),
        )
    }
}
