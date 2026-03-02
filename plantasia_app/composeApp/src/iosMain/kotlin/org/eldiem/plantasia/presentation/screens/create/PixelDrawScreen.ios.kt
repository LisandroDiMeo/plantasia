package org.eldiem.plantasia.presentation.screens.create

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.*
import platform.Foundation.NSData
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual fun encodeBitmapToPng(bitmap: ImageBitmap): ByteArray {
    val width = bitmap.width
    val height = bitmap.height
    val pixelMap = bitmap.toPixelMap()

    val rgbaBytes = ByteArray(width * height * 4)
    var offset = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = pixelMap[x, y]
            rgbaBytes[offset++] = (color.red * 255).toInt().toByte()
            rgbaBytes[offset++] = (color.green * 255).toInt().toByte()
            rgbaBytes[offset++] = (color.blue * 255).toInt().toByte()
            rgbaBytes[offset++] = (color.alpha * 255).toInt().toByte()
        }
    }

    val colorSpace = CGColorSpaceCreateDeviceRGB()
    val data = rgbaBytes.usePinned { pinned ->
        val context = CGBitmapContextCreate(
            data = pinned.addressOf(0),
            width = width.toULong(),
            height = height.toULong(),
            bitsPerComponent = 8u,
            bytesPerRow = (width * 4).toULong(),
            space = colorSpace,
            bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
        )
        val cgImage = CGBitmapContextCreateImage(context)
        val uiImage = UIImage(cGImage = cgImage)
        val pngData = UIImagePNGRepresentation(uiImage)
        if (pngData != null) {
            ByteArray(pngData.length.toInt()).also { bytes ->
                bytes.usePinned { outPinned ->
                    memcpy(outPinned.addressOf(0), pngData.bytes, pngData.length)
                }
            }
        } else {
            ByteArray(0)
        }
    }
    return data
}
