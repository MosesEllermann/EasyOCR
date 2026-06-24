package com.easyocr.editor.intents

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Parcelable

data class ImageIntentInput(
    val uri: Uri,
    val flags: Int,
    val sourceAction: String?,
)

class ImageEditIntentHandler {
    fun extract(intent: Intent?): ImageIntentInput? {
        if (intent == null) return null
        val uri = when (intent.action) {
            Intent.ACTION_SEND -> intent.parcelableExtra(Intent.EXTRA_STREAM)
            Intent.ACTION_EDIT,
            Intent.ACTION_VIEW,
            -> intent.data ?: intent.parcelableExtra(Intent.EXTRA_STREAM)
            else -> intent.data
        } ?: return null

        return ImageIntentInput(
            uri = uri,
            flags = intent.flags,
            sourceAction = intent.action,
        )
    }

    fun supportsMimeType(mimeType: String?): Boolean =
        mimeType?.startsWith("image/") == true

    fun tryPersistReadPermission(
        contentResolver: ContentResolver,
        input: ImageIntentInput,
    ) {
        val persistable = input.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0
        val readable = input.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
        if (!persistable || !readable) return

        runCatching {
            contentResolver.takePersistableUriPermission(
                input.uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
}

private inline fun <reified T : Parcelable> Intent.parcelableExtra(key: String): T? {
    @Suppress("DEPRECATION")
    return getParcelableExtra(key)
}
