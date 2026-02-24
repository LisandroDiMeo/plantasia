package org.eldiem.plantasia.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap

object ImageConverter {
    private const val IMAGE_WIDTH = 240
    private const val IMAGE_HEIGHT = 240
    const val EXPECTED_SIZE = IMAGE_WIDTH * IMAGE_HEIGHT * 2 // 115,200 bytes

    fun bitmapToRgb565(bitmap: ImageBitmap): ByteArray {
        val pixelMap = bitmap.toPixelMap(
            startX = 0, startY = 0,
            width = IMAGE_WIDTH, height = IMAGE_HEIGHT
        )
        val output = ByteArray(EXPECTED_SIZE)
        var offset = 0

        for (y in 0 until IMAGE_HEIGHT) {
            for (x in 0 until IMAGE_WIDTH) {
                val color = pixelMap[x, y]
                val r = (color.red * 31).toInt() and 0x1F
                val g = (color.green * 63).toInt() and 0x3F
                val b = (color.blue * 31).toInt() and 0x1F
                val rgb565 = (r shl 11) or (g shl 5) or b

                // Big-endian
                output[offset++] = (rgb565 shr 8).toByte()
                output[offset++] = (rgb565 and 0xFF).toByte()
            }
        }
        return output
    }
}
