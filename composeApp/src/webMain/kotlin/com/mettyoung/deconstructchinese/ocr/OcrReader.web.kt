package com.mettyoung.deconstructchinese.ocr

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual class OcrReader actual constructor() {
    actual fun recognizeText(imageBytes: ByteArray): Flow<OcrResult> =
        flowOf(OcrResult.Error("unsupported"))
}
