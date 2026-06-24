# EasyOCR

EasyOCR is a lightweight, privacy-first Android image editor for screenshot text copying. It is meant to appear in Android's image edit flow: take a screenshot, tap the screenshot preview, choose EasyOCR, then tap detected text blocks to copy them.

## Current Architecture

- `MainActivity` receives Android image intents and hosts the Compose editor.
- `ImageEditIntentHandler` extracts `content://` image URIs from `ACTION_EDIT`, `ACTION_VIEW`, and `ACTION_SEND`.
- `ImageLoader` decodes content URIs, downsamples very large images, and applies EXIF rotation.
- `OcrEngine` abstracts OCR; `MlKitOcrEngine` implements bundled, local ML Kit Text Recognition v2.
- `OcrRepository` caches OCR results for the current bitmap and language during the session.
- `ImageCanvasWithOcrOverlay` displays the image, handles pinch/pan, and maps taps to OCR blocks.
- `SaveImageUseCase` writes copies through MediaStore and shares through a `FileProvider`.

## OCR Engine Choice

EasyOCR uses ML Kit Text Recognition v2 with the bundled Latin recognizer dependency:

```kotlin
implementation("com.google.mlkit:text-recognition:16.0.1")
```

This is the practical choice for GrapheneOS because OCR runs on-device and the model is bundled with the app instead of being downloaded by Google Play Services at runtime. It does not require a Google account, cloud processing, or the `INTERNET` permission.

Tradeoffs:

- ML Kit's bundled recognizer increases APK size compared with Play Services delivery.
- The Latin recognizer covers English and German well, but the language selector is a recognition hint in this first version rather than separate OCR models.
- Tesseract is fully open source and very offline-friendly, but Android packaging and language data management add more size and maintenance burden.
- PaddleOCR can be strong, but Android integration is heavier for this focused screenshot-editor workflow.

## Privacy

OCR runs locally on your device. Images are not uploaded.

EasyOCR:

- does not request `INTERNET`
- does not include analytics
- does not include crash reporting
- does not include ads
- does not overwrite the original screenshot

The manifest only requests legacy `WRITE_EXTERNAL_STORAGE` for Android 8-9 save-copy support. Android 10 and newer use scoped storage through MediaStore.

## Intent Support

The manifest registers:

- `android.intent.action.EDIT` for `image/*`, `image/png`, `image/jpeg`, and `image/webp`
- `android.intent.action.VIEW` for the same image types
- `android.intent.action.SEND` for `image/*`

Incoming `content://` URIs are opened through `ContentResolver`. Persistable read permission is taken when the provider grants it.

## Build

Requirements:

- Android Studio with Android SDK Platform 36 installed
- JDK 17
- Gradle 9.4.1 or newer when building outside Android Studio

Open this folder in Android Studio and run the `app` configuration, or build from a terminal with an installed Gradle:

```bash
gradle assembleDebug
```

For a local signed release build, configure a private keystore through `local.properties`:

```properties
easyocr.release.storeFile=easyocr-release.jks
easyocr.release.storePassword=...
easyocr.release.keyAlias=easyocr
easyocr.release.keyPassword=...
```

Then run:

```bash
gradle test assembleRelease
```

## GrapheneOS Manual Test Checklist

1. Install the debug APK on a GrapheneOS device.
2. Disable network access for the app in system settings if desired; OCR should still work.
3. Take a screenshot.
4. Tap the screenshot preview.
5. Choose EasyOCR from the editor chooser.
6. Confirm the screenshot opens.
7. Confirm OCR starts automatically while offline.
8. Confirm subtle text overlays appear.
9. Pinch and pan the image, then tap a text block and confirm it copies.
10. Long-press a text block and confirm copy block, copy all, and full text actions appear.
11. Open full OCR text and confirm Copy all and Re-run OCR work.
12. Rotate left/right and confirm OCR reruns.
13. Zoom to a visible region, tap Crop, and confirm the image is cropped to that region.
14. Tap Save copy and confirm a new image appears in Photos or Files.
15. Tap Share and confirm Android shares a copied image URI.

## Notes

Android's screenshot preview/editor chooser differs a little by OS build and OEM. The important pieces for GrapheneOS are the `ACTION_EDIT` image intent filter, `image/*` MIME support, and content URI read handling, all of which are included here.

## License

EasyOCR is open source under the Apache License 2.0. See [LICENSE](LICENSE).
