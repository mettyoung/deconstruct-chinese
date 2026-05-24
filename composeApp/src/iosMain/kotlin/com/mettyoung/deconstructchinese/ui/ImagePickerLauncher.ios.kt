package com.mettyoung.deconstructchinese.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.UIKit.*
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
private class IosImagePickerDelegate(
    private val onImagePicked: (ByteArray) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        image?.let { uiImage ->
            val nsData: NSData? = UIImageJPEGRepresentation(uiImage, 0.9)
            nsData?.let { data ->
                val length = data.length.toInt()
                val bytes = ByteArray(length)
                if (length > 0 && data.bytes != null) {
                    bytes.usePinned { pinned ->
                        memcpy(pinned.addressOf(0), data.bytes, data.length)
                    }
                }
                onImagePicked(bytes)
            }
        }
        picker.dismissViewControllerAnimated(true, completion = null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

private class IosImagePickerLauncher(
    private val onImagePicked: (ByteArray) -> Unit
) : ImagePickerLauncher {
    private var delegate: IosImagePickerDelegate? = null

    override fun launchCamera() =
        launch(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)

    override fun launchGallery() =
        launch(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)

    private fun launch(sourceType: UIImagePickerControllerSourceType) {
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        delegate = IosImagePickerDelegate(onImagePicked)
        val picker = UIImagePickerController()
        picker.sourceType = sourceType
        picker.delegate = delegate
        rootVC.presentViewController(picker, animated = true, completion = null)
    }
}

@Composable
actual fun rememberImagePickerLauncher(onImagePicked: (ByteArray) -> Unit): ImagePickerLauncher {
    val currentCallback = rememberUpdatedState(onImagePicked)
    return remember { IosImagePickerLauncher { bytes -> currentCallback.value(bytes) } }
}
