package com.easyocr.editor

import android.Manifest
import android.graphics.Color
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.easyocr.editor.ui.ScreenshotEditorScreen
import com.easyocr.editor.ui.theme.EasyOcrTheme

class MainActivity : ComponentActivity() {
    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        viewModel.openIntent(intent)

        setContent {
            EasyOcrTheme {
                val state by viewModel.uiState.collectAsState()
                val imagePicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent(),
                ) { uri: Uri? ->
                    uri?.let(viewModel::openUri)
                }
                val savePermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) viewModel.saveCopy()
                }
                val shareChooserTitle = remember { "Share image" }

                ScreenshotEditorScreen(
                    state = state,
                    onOpenPicker = { imagePicker.launch("image/*") },
                    onToggleOverlays = viewModel::toggleOverlays,
                    onRotateLeft = viewModel::rotateLeft,
                    onRotateRight = viewModel::rotateRight,
                    onCrop = viewModel::cropTo,
                    onApplyDrawing = viewModel::applyDrawing,
                    onSaveCopy = {
                        if (needsLegacyWritePermission()) {
                            savePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            viewModel.saveCopy()
                        }
                    },
                    onShare = {
                        viewModel.share { shareIntent ->
                            startActivity(Intent.createChooser(shareIntent, shareChooserTitle))
                        }
                    },
                    onShowMessageConsumed = viewModel::clearMessage,
                    onCopyAll = {},
                    onRerunOcr = viewModel::rerunOcr,
                    onLanguageSelected = viewModel::setLanguage,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.openIntent(intent)
    }

    private fun needsLegacyWritePermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED
}
