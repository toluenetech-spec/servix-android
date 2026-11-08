package ng.name.servix.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ng.name.servix.app.data.media.MimeSniffer
import ng.name.servix.app.data.media.ProgressRequestBody
import ng.name.servix.app.data.model.ChatAttachmentInput
import ng.name.servix.app.data.model.ChatUploadKind
import ng.name.servix.app.data.model.ServixApiException
import ng.name.servix.app.data.model.UploadedFile
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.ServixApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull

/**
 * `POST /uploads` for chat media. Two rules come straight from `api/docs/CHAT.md:66-76`:
 * the body is the raw file with its real content type, and the type must be on the per-kind list.
 *
 * Nothing here trusts what the picker claims: [MimeSniffer] decides the type from the bytes, and the
 * upload is refused locally (with the API's own wording) before spending a request when the file is
 * empty, too large or simply not something the API accepts.
 */
class UploadRepository(private val api: ServixApiService) {

    /** Thrown with a user-facing message; the bubble shows it verbatim. */
    class UploadRejected(message: String) : Exception(message)

    suspend fun upload(
        kind: String,
        bytes: ByteArray,
        fileName: String,
        onProgress: (Float) -> Unit = {},
    ): Result<UploadedFile> = withContext(Dispatchers.IO) {
        val detected = MimeSniffer.detect(bytes, fileName)
            ?: return@withContext Result.failure(
                UploadRejected(unsupportedMessage(kind)),
            )
        val allowed = ChatUploadKind.allowed(kind)
        if (detected.mime !in allowed) {
            return@withContext Result.failure(UploadRejected(unsupportedMessage(kind)))
        }
        if (bytes.isEmpty()) {
            return@withContext Result.failure(UploadRejected("That file is empty."))
        }
        val max = ChatUploadKind.maxBytes(kind)
        if (bytes.size > max) {
            return@withContext Result.failure(UploadRejected("That file is too large. Allowed: ${limitLabel(kind)}"))
        }
        val safeName = safeFileName(fileName, detected.extension)
        try {
            val body = ProgressRequestBody(bytes, detected.mime.toMediaTypeOrNull(), onProgress)
            Result.success(api.uploadChatFile(kind, safeName, detected.mime, body))
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    /** The attachment the message endpoint expects, built from the upload (`.strict()` schema). */
    fun attachmentFor(
        uploaded: UploadedFile,
        name: String,
        bytes: Long,
        durationMs: Long? = null,
        width: Int? = null,
        height: Int? = null,
    ): ChatAttachmentInput = ChatAttachmentInput(
        key = uploaded.key,
        name = name,
        mime = uploaded.contentType,
        size = if (uploaded.size > 0) uploaded.size else bytes,
        durationMs = durationMs,
        width = width,
        height = height,
    )

    private fun safeFileName(raw: String, extension: String): String {
        val cleaned = raw.substringAfterLast('/').trim().take(200)
        if (cleaned.isEmpty()) return "upload.$extension"
        return cleaned
    }

    private fun unsupportedMessage(kind: String): String = when (kind) {
        ChatUploadKind.IMAGE -> "Unsupported file type. Allowed: JPEG, PNG, WebP or GIF photos."
        ChatUploadKind.AUDIO -> "Unsupported file type. Allowed: voice notes."
        else -> "Unsupported file type. Allowed: PDF, Word, Excel, PowerPoint, text or CSV files."
    }

    private fun limitLabel(kind: String): String = when (kind) {
        ChatUploadKind.IMAGE -> "JPEG, PNG, WebP or GIF photos up to 10 MB"
        ChatUploadKind.AUDIO -> "voice notes up to 10 MB"
        else -> "PDF, Word, Excel, PowerPoint, text or CSV files up to 20 MB"
    }

    private fun asServix(t: Throwable) = t as? ServixApiException
}
