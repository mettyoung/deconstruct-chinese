package com.mettyoung.deconstructchinese.ocr

import kotlinx.coroutines.flow.Flow

expect class OcrReader() {
    fun recognizeText(imageBytes: ByteArray): Flow<OcrResult>
}
