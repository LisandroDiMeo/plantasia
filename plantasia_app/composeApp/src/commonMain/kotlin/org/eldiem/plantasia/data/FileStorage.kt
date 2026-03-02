package org.eldiem.plantasia.data

expect class FileStorage {
    fun readText(fileName: String): String?
    fun writeText(fileName: String, content: String)
    fun readBytes(fileName: String): ByteArray?
    fun writeBytes(fileName: String, data: ByteArray)
    fun exists(fileName: String): Boolean
}
