package org.eldiem.plantasia.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.*
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ImagePicker(
    show: Boolean,
    onImagePicked: (ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    val delegate = remember {
        object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                picker.dismissViewControllerAnimated(true, null)
                val result = didFinishPicking.firstOrNull() as? PHPickerResult
                if (result == null) {
                    onDismiss()
                    return
                }
                result.itemProvider.loadDataRepresentationForTypeIdentifier(
                    UTTypeImage.identifier
                ) { data, _ ->
                    if (data != null) {
                        val image = UIImage(data = data)
                        val size = CGSizeMake(240.0, 240.0)
                        UIGraphicsBeginImageContextWithOptions(size, false, 1.0)
                        image.drawInRect(CGRectMake(0.0, 0.0, 240.0, 240.0))
                        val scaledImage = UIGraphicsGetImageFromCurrentImageContext()
                        UIGraphicsEndImageContext()
                        val pngData = scaledImage?.let { UIImagePNGRepresentation(it) }
                        if (pngData != null) {
                            val bytes = ByteArray(pngData.length.toInt())
                            bytes.usePinned { pinned ->
                                memcpy(pinned.addressOf(0), pngData.bytes, pngData.length)
                            }
                            onImagePicked(bytes)
                        } else {
                            onDismiss()
                        }
                    } else {
                        onDismiss()
                    }
                }
            }
        }
    }

    LaunchedEffect(show) {
        if (show) {
            val config = PHPickerConfiguration()
            config.selectionLimit = 1
            config.filter = PHPickerFilter.imagesFilter
            val picker = PHPickerViewController(configuration = config)
            picker.delegate = delegate
            val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
            rootVC?.presentViewController(picker, animated = true, completion = null)
        }
    }
}
