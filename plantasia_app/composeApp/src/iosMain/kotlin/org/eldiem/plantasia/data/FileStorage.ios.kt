package org.eldiem.plantasia.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.posix.memcpy

actual class FileStorage {
    private val documentsDir: String by lazy {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        )
        paths.first() as String
    }

    private fun filePath(fileName: String): String = "$documentsDir/$fileName"

    @OptIn(ExperimentalForeignApi::class)
    actual fun readText(fileName: String): String? {
        val path = filePath(fileName)
        return if (NSFileManager.defaultManager.fileExistsAtPath(path)) {
            NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) as? String
        } else null
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun writeText(fileName: String, content: String) {
        (content as NSString).writeToFile(filePath(fileName), atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun readBytes(fileName: String): ByteArray? {
        val path = filePath(fileName)
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return null
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        return ByteArray(data.length.toInt()).also { bytes ->
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun writeBytes(fileName: String, data: ByteArray) {
        val nsData = data.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), data.size.toULong())
        }
        nsData.writeToFile(filePath(fileName), atomically = true)
    }

    actual fun exists(fileName: String): Boolean =
        NSFileManager.defaultManager.fileExistsAtPath(filePath(fileName))
}
