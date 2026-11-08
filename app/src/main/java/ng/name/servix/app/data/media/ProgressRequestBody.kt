package ng.name.servix.app.data.media

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException

/**
 * Raw upload body that reports how much has been written, so the bubble can show a ring instead of
 * a spinner that never moves (`POST /uploads` streams the file through the API).
 */
class ProgressRequestBody(
    private val bytes: ByteArray,
    private val contentType: MediaType?,
    private val onProgress: (Float) -> Unit,
) : RequestBody() {

    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = bytes.size.toLong()

    override fun writeTo(sink: BufferedSink) {
        val total = bytes.size.coerceAtLeast(1)
        var written = 0
        val chunk = 8 * 1024
        while (written < bytes.size) {
            val end = minOf(written + chunk, bytes.size)
            sink.write(bytes, written, end - written)
            written = end
            onProgress((written.toFloat() / total).coerceIn(0f, 1f))
        }
        if (bytes.isEmpty()) throw IOException("The file is empty.")
    }
}
