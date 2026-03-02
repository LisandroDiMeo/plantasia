package org.eldiem.plantasia.presentation.components

import androidx.compose.runtime.Composable

@Composable
expect fun ImagePicker(
    show: Boolean,
    onImagePicked: (ByteArray) -> Unit,
    onDismiss: () -> Unit
)
