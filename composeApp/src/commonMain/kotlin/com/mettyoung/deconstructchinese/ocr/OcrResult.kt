package com.mettyoung.deconstructchinese.ocr

sealed class OcrResult {
    data class Success(val text: String) : OcrResult()
    data class Error(val message: String) : OcrResult()
}
