package com.mettyoung.deconstructchinese.ui

import androidx.compose.runtime.Composable

interface ImagePickerLauncher {
    fun launchCamera()
    fun launchGallery()
}

@Composable
expect fun rememberImagePickerLauncher(onImagePicked: (ByteArray) -> Unit): ImagePickerLauncher
