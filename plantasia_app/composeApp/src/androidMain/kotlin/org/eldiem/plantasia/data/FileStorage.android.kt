package org.eldiem.plantasia.data

import android.content.Context
import java.io.File

actual class FileStorage(private val context: Context) {
    private fun file(fileName: String): File = File(context.filesDir, fileName)

    actual fun readText(fileName: String): String? {
        val f = file(fileName)
        return if (f.exists()) f.readText() else null
    }

    actual fun writeText(fileName: String, content: String) {
        file(fileName).writeText(content)
    }

    actual fun readBytes(fileName: String): ByteArray? {
        val f = file(fileName)
        return if (f.exists()) f.readBytes() else null
    }

    actual fun writeBytes(fileName: String, data: ByteArray) {
        file(fileName).writeBytes(data)
    }

    actual fun exists(fileName: String): Boolean = file(fileName).exists()
}
