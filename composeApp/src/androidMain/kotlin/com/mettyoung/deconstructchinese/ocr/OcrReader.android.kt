package com.mettyoung.deconstructchinese.ocr

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class OcrReader actual constructor() {
    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    actual fun recognizeText(imageBytes: ByteArray): Flow<OcrResult> = flow {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: run { emit(OcrResult.Error("Failed to decode image")); return@flow }
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        try {
            val text = suspendCancellableCoroutine { cont ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val extracted = visionText.textBlocks.joinToString("\n") { it.text }
                        cont.resume(extracted)
                    }
                    .addOnFailureListener { e ->
                        cont.resumeWithException(e)
                    }
            }
            if (text.isNotBlank()) {
                emit(OcrResult.Success(text))
            } else {
                emit(OcrResult.Error("No text found in image"))
            }
        } catch (e: Exception) {
            emit(OcrResult.Error(e.message ?: "OCR failed"))
        }
    }
}
