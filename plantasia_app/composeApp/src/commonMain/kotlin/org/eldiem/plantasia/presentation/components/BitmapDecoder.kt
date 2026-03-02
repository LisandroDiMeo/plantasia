package org.eldiem.plantasia.presentation.components

import androidx.compose.ui.graphics.ImageBitmap

expect fun decodeByteArrayToImageBitmap(bytes: ByteArray): ImageBitmap?
