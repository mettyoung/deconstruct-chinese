package com.mettyoung.deconstructchinese.ocr

import kotlinx.cinterop.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.Vision.*
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class OcrReader actual constructor() {
    actual fun recognizeText(imageBytes: ByteArray, language: OcrLanguage): Flow<OcrResult> = flow {
        val nsData = imageBytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = imageBytes.size.toULong())
        }
        val uiImage = UIImage.imageWithData(nsData)
        val cgImage = uiImage?.CGImage
            ?: run { emit(OcrResult.Error("Failed to create image")); return@flow }

        val request = VNRecognizeTextRequest()
        request.recognitionLevel = VNRequestTextRecognitionLevelAccurate
        request.recognitionLanguages = when (language) {
            OcrLanguage.CHINESE -> listOf("zh-Hans", "zh-Hant")
            OcrLanguage.ENGLISH -> listOf("en-US")
        }

        val handler = VNImageRequestHandler(cgImage, options = emptyMap<Any?, Any?>())
        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val success = handler.performRequests(listOf(request), error = errorPtr.ptr)
            if (!success) {
                val msg = errorPtr.value?.localizedDescription ?: "Vision processing failed"
                emit(OcrResult.Error(msg))
                return@flow
            }
        }

        @Suppress("UNCHECKED_CAST")
        val observations = request.results as? List<VNRecognizedTextObservation>
        val text = observations
            ?.mapNotNull { obs -> (obs.topCandidates(1u).firstOrNull() as? VNRecognizedText)?.string }
            ?.joinToString("\n")
            ?: ""

        if (text.isNotBlank()) {
            emit(OcrResult.Success(text))
        } else {
            emit(OcrResult.Error("No text found in image"))
        }
    }
}
