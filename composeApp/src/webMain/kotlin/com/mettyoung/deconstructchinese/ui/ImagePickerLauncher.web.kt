package com.mettyoung.deconstructchinese.ui

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImagePickerLauncher(onImagePicked: (ByteArray) -> Unit): ImagePickerLauncher =
    object : ImagePickerLauncher {
        override fun launchCamera() {}
        override fun launchGallery() {}
    }
