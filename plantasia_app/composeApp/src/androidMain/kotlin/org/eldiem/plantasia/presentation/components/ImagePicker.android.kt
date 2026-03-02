package org.eldiem.plantasia.presentation.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream

@Composable
actual fun ImagePicker(
    show: Boolean,
    onImagePicked: (ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val original = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (original != null) {
                    val scaled = Bitmap.createScaledBitmap(original, 240, 240, true)
                    val outputStream = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    onImagePicked(outputStream.toByteArray())
                    if (scaled !== original) scaled.recycle()
                    original.recycle()
                } else {
                    onDismiss()
                }
            } catch (_: Exception) {
                onDismiss()
            }
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(show) {
        if (show) {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}
