package com.easyocr.editor.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
        ) {
            Text("Privacy")
            Text(
                text = "OCR runs locally on your device. Images are not uploaded.\n\n" +
                    "EasyOCR does not request internet access, does not use analytics, " +
                    "does not include ads, and does not send screenshots to a server.",
                modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
            )
        }
    }
}
