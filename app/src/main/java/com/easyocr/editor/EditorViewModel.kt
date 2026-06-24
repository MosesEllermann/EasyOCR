package com.easyocr.editor

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.easyocr.editor.geometry.RectF2
import com.easyocr.editor.image.BitmapEditor
import com.easyocr.editor.image.ImageLoader
import com.easyocr.editor.image.SaveImageUseCase
import com.easyocr.editor.intents.ImageEditIntentHandler
import com.easyocr.editor.ocr.MlKitOcrEngine
import com.easyocr.editor.ocr.OcrLanguage
import com.easyocr.editor.ocr.OcrRepository
import com.easyocr.editor.ui.CropPreset
import com.easyocr.editor.ui.CropState
import com.easyocr.editor.ui.DrawingStroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val intentHandler = ImageEditIntentHandler()
    private val imageLoader = ImageLoader(application.contentResolver)
    private val ocrRepository = OcrRepository(MlKitOcrEngine())
    private val saveImageUseCase = SaveImageUseCase(application)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState

    fun openIntent(intent: Intent?) {
        val input = intentHandler.extract(intent) ?: return
        intentHandler.tryPersistReadPermission(getApplication<Application>().contentResolver, input)
        openUri(input.uri)
    }

    fun openUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    sourceUri = uri,
                    isLoadingImage = true,
                    isRunningOcr = false,
                    ocrResult = null,
                    errorMessage = null,
                )
            }
            runCatching { imageLoader.decode(uri) }
                .onSuccess { bitmap ->
                    ocrRepository.clear()
                    _uiState.update {
                        it.copy(
                            bitmap = bitmap,
                            isLoadingImage = false,
                            lastSavedUri = null,
                        )
                    }
                    runOcr(force = true)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingImage = false,
                            errorMessage = throwable.message ?: "Could not open image",
                        )
                    }
                }
        }
    }

    fun setLanguage(language: OcrLanguage) {
        if (_uiState.value.language == language) return
        _uiState.update { it.copy(language = language, ocrResult = null) }
        runOcr(force = true)
    }

    fun toggleOverlays() {
        _uiState.update { it.copy(overlaysEnabled = !it.overlaysEnabled) }
    }

    fun rerunOcr() {
        runOcr(force = true)
    }

    fun rotateLeft() {
        rotate(-90f)
    }

    fun rotateRight() {
        rotate(90f)
    }

    fun cropTo(cropState: CropState) {
        val current = _uiState.value.bitmap ?: return
        viewModelScope.launch {
            runCatching {
                if (cropState.preset == CropPreset.Document) {
                    BitmapEditor.cropPerspective(current, cropState.documentCorners)
                } else {
                    BitmapEditor.crop(current, cropState.rect)
                }
            }
                .onSuccess { bitmap ->
                    ocrRepository.clear()
                    _uiState.update { it.copy(bitmap = bitmap, ocrResult = null) }
                    runOcr(force = true)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(errorMessage = throwable.message ?: "Could not crop image")
                    }
                }
        }
    }

    fun applyDrawing(strokes: List<DrawingStroke>) {
        val current = _uiState.value.bitmap ?: return
        if (strokes.isEmpty()) return
        viewModelScope.launch {
            runCatching { BitmapEditor.drawStrokes(current, strokes) }
                .onSuccess { bitmap ->
                    ocrRepository.clear()
                    _uiState.update { it.copy(bitmap = bitmap, ocrResult = null) }
                    runOcr(force = true)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(errorMessage = throwable.message ?: "Could not apply drawing")
                    }
                }
        }
    }

    fun saveCopy() {
        val current = _uiState.value.bitmap ?: return
        viewModelScope.launch {
            runCatching { saveImageUseCase.saveCopy(current) }
                .onSuccess { uri ->
                    _uiState.update { it.copy(lastSavedUri = uri, errorMessage = null) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(errorMessage = throwable.message ?: "Could not save copy")
                    }
                }
        }
    }

    fun share(onReady: (Intent) -> Unit) {
        val current = _uiState.value.bitmap ?: return
        viewModelScope.launch {
            runCatching { saveImageUseCase.shareCopy(current) }
                .onSuccess(onReady)
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(errorMessage = throwable.message ?: "Could not share image")
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, lastSavedUri = null) }
    }

    private fun rotate(degrees: Float) {
        val current = _uiState.value.bitmap ?: return
        viewModelScope.launch {
            val rotated = BitmapEditor.rotate(current, degrees)
            ocrRepository.clear()
            _uiState.update { it.copy(bitmap = rotated, ocrResult = null) }
            runOcr(force = true)
        }
    }

    private fun runOcr(force: Boolean) {
        val current: Bitmap = _uiState.value.bitmap ?: return
        val language = _uiState.value.language
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningOcr = true, errorMessage = null) }
            runCatching { ocrRepository.recognize(current, language, force) }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(ocrResult = result, isRunningOcr = false)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isRunningOcr = false,
                            errorMessage = throwable.message ?: "OCR failed",
                        )
                    }
                }
        }
    }
}
