package com.easyocr.editor.intents

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImageEditIntentHandlerTest {
    private val handler = ImageEditIntentHandler()

    @Test
    fun extractsEditDataUri() {
        val uri = Uri.parse("content://screenshots/current.png")
        val result = handler.extract(
            Intent(Intent.ACTION_EDIT).apply {
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )

        assertNotNull(result)
        assertEquals(uri, result?.uri)
        assertEquals(Intent.ACTION_EDIT, result?.sourceAction)
    }

    @Test
    fun extractsSendStreamUri() {
        val uri = Uri.parse("content://gallery/image.jpg")
        val result = handler.extract(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
            },
        )

        assertEquals(uri, result?.uri)
    }

    @Test
    fun imageMimeTypesAreSupported() {
        assertTrue(handler.supportsMimeType("image/webp"))
        assertTrue(handler.supportsMimeType("image/png"))
    }

    @Test
    fun persistPermissionIsBestEffort() {
        val uri = Uri.parse("content://screenshots/current.png")
        handler.tryPersistReadPermission(
            contentResolver = RuntimeEnvironment.getApplication().contentResolver,
            input = ImageIntentInput(
                uri = uri,
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                sourceAction = Intent.ACTION_EDIT,
            ),
        )
    }
}
