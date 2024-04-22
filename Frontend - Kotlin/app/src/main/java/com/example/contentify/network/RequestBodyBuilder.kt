package com.example.contentify.network

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

class RequestBodyBuilder(
    private val contentResolver: ContentResolver
) {
    fun build(uri: Uri): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType? =
                contentResolver.getType(uri)?.toMediaTypeOrNull()

            override fun writeTo(sink: BufferedSink) {
                contentResolver.openInputStream(uri)?.source()?.use(sink::writeAll)
            }

            override fun contentLength(): Long =
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeColumnIndex: Int = cursor.getColumnIndex(OpenableColumns.SIZE)
                    cursor.moveToFirst()
                    cursor.getLong(sizeColumnIndex)
                } ?: super.contentLength()
        }
    }
}
