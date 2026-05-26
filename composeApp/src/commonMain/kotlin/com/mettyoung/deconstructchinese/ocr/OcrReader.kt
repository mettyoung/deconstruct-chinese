package com.mettyoung.deconstructchinese.ocr

import kotlinx.coroutines.flow.Flow

expect class OcrReader() {
    fun recognizeText(imageBytes: ByteArray, language: OcrLanguage): Flow<OcrResult>
}
