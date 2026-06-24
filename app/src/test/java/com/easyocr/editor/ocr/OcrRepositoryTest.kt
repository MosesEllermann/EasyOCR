package com.easyocr.editor.ocr

import android.graphics.Bitmap
import com.easyocr.editor.geometry.RectF2
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OcrRepositoryTest {
    @Test
    fun repositoryCachesCurrentBitmapResult() = runTest {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val engine = FakeOcrEngine()
        val repository = OcrRepository(engine)

        repository.recognize(bitmap, OcrLanguage.EnglishGerman)
        repository.recognize(bitmap, OcrLanguage.EnglishGerman)

        assertEquals(1, engine.calls)
    }

    private class FakeOcrEngine : OcrEngine {
        var calls = 0

        override suspend fun recognize(
            bitmap: Bitmap,
            language: OcrLanguage,
        ): OcrResult {
            calls += 1
            return OcrResult(
                fullText = "hello",
                blocks = listOf(
                    OcrTextBlock(
                        id = "1",
                        text = "hello",
                        boundingBox = RectF2(0f, 0f, 4f, 4f),
                    ),
                ),
            )
        }
    }
}
