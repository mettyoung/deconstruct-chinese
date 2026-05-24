package com.mettyoung.deconstructchinese.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberImagePickerLauncher(onImagePicked: (ByteArray) -> Unit): ImagePickerLauncher {
    val context = LocalContext.current
    val currentCallback = rememberUpdatedState(onImagePicked)

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes() ?: return@let
            currentCallback.value(bytes)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val stream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            currentCallback.value(stream.toByteArray())
        }
    }

    return remember {
        object : ImagePickerLauncher {
            override fun launchCamera() = cameraLauncher.launch(null)
            override fun launchGallery() = galleryLauncher.launch("image/*")
        }
    }
}
