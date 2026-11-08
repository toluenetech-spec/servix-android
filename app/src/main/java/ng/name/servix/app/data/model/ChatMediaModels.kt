package ng.name.servix.app.data.model

import kotlinx.serialization.Serializable

/**
 * `POST /uploads?kind=chat-image|chat-audio|chat-file` → 201
 * `{ url, key, kind, fileName, contentType, size }` (`api/src/routes/uploads.ts:79-99`).
 *
 * The `key` is what a chat message stores; the `url` is only for showing the file we just sent.
 */
@Serializable
data class UploadedFile(
    val url: String = "",
    val key: String = "",
    val kind: String = "",
    val fileName: String = "",
    val contentType: String = "",
    val size: Long = 0L,
)

/**
 * Attachment as the message endpoint accepts it (`attachmentSchema`, community.ts:18-22):
 * `{ key, name, mime, size, durationMs?, width?, height? }` and nothing else — the schema is
 * `.strict()`, so sending `url` would be a 422. `kind` is derived server-side from the key prefix.
 */
@Serializable
data class ChatAttachmentInput(
    val key: String,
    val name: String,
    val mime: String,
    val size: Long,
    val durationMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
)

/** `POST /community/threads/:id/messages/:mid/react { emoji }` (community.ts:236). */
@Serializable
data class ChatReactionRequest(val emoji: String)

/** Upload kinds the API accepts for messaging (`api/src/lib/storage.ts:41`). */
object ChatUploadKind {
    const val IMAGE = "chat-image"
    const val AUDIO = "chat-audio"
    const val FILE = "chat-file"

    /** `uploadRules()` limits, `api/docs/CHAT.md:69-71`. */
    const val IMAGE_MAX_BYTES = 10L * 1024 * 1024
    const val AUDIO_MAX_BYTES = 10L * 1024 * 1024
    const val FILE_MAX_BYTES = 20L * 1024 * 1024

    val IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
    val AUDIO_TYPES = setOf(
        "audio/webm", "audio/mp4", "audio/mpeg", "audio/ogg", "audio/wav", "audio/aac", "audio/x-m4a", "audio/m4a",
    )
    val FILE_TYPES = setOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain",
        "text/csv",
    )

    fun maxBytes(kind: String): Long = when (kind) {
        IMAGE -> IMAGE_MAX_BYTES
        AUDIO -> AUDIO_MAX_BYTES
        else -> FILE_MAX_BYTES
    }

    fun allowed(kind: String): Set<String> = when (kind) {
        IMAGE -> IMAGE_TYPES
        AUDIO -> AUDIO_TYPES
        else -> FILE_TYPES
    }
}
