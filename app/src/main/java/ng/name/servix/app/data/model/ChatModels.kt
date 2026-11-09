package ng.name.servix.app.data.model

import kotlinx.serialization.Serializable

/**
 * Chat DTOs. Every field here exists in `api/docs/CHAT.md` and in the shapes returned by
 * `api/src/routes/community.ts` — nothing is invented. Defaults exist because the API omits
 * `null`s (`explicitNulls = false`) and because older rows may lack the optional delivery columns.
 */

/** `person` on a thread / connection: `{ id, fullName, professionalProfile }` (community.ts `who`). */
@Serializable
data class ChatPersonDto(
    val id: String = "",
    val fullName: String = "",
    val professionalProfile: ChatPersonProfileDto? = null,
)

@Serializable
data class ChatPersonProfileDto(
    val slug: String = "",
    val title: String? = null,
    val imageUrl: String? = null,
)

@Serializable
data class ChatBookingRefDto(
    val reference: String? = null,
    val serviceTitle: String? = null,
)

/** One message (`shapeMessage` in community.ts). */
@Serializable
data class ChatMessageDto(
    val id: String = "",
    val body: String = "",
    val senderId: String = "",
    val createdAt: String = "",
    val clientId: String? = null,
    /** `text | image | audio | file | deleted`. */
    val kind: String = "text",
    val attachment: ChatAttachmentDto? = null,
    val replyTo: ChatReplyDto? = null,
    val deletedAt: String? = null,
    val reactions: Map<String, List<String>> = emptyMap(),
)

@Serializable
data class ChatAttachmentDto(
    val url: String = "",
    val name: String? = null,
    val mime: String? = null,
    val size: Long? = null,
    val durationMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
data class ChatReplyDto(
    val id: String = "",
    val senderId: String? = null,
    val kind: String? = null,
    val body: String? = null,
    val attachmentName: String? = null,
)

/** One row of `GET /community/threads` (returns a bare array). */
@Serializable
data class ChatThreadDto(
    val id: String = "",
    val person: ChatPersonDto? = null,
    val booking: ChatBookingRefDto? = null,
    val bookingId: String? = null,
    val connectionId: String? = null,
    val lastMessage: String = "",
    val lastMessageKind: String? = null,
    val lastMessageAt: String? = null,
    val lastMessageSenderId: String? = null,
    val lastMessageId: String? = null,
    val unread: Boolean = false,
    val unreadCount: Int = 0,
    val myReadAt: String? = null,
    val otherReadAt: String? = null,
    val otherDeliveredAt: String? = null,
    val online: Boolean = false,
    val lastSeenAt: String? = null,
    val updatedAt: String? = null,
)

/** `GET /community/threads/:id/messages`. */
@Serializable
data class ChatMessagesResponse(
    val messages: List<ChatMessageDto> = emptyList(),
    val reactions: List<String> = emptyList(),
    val before: String? = null,
    val canSend: Boolean = false,
    val myReadAt: String? = null,
    val otherReadAt: String? = null,
    val otherDeliveredAt: String? = null,
    val online: Boolean = false,
    val lastSeenAt: String? = null,
    val otherUserId: String? = null,
)

@Serializable
data class SendChatMessageRequest(
    val clientId: String,
    /** May be empty when the message carries an attachment (`api/docs/CHAT.md:72-76`). */
    val body: String = "",
    val replyToId: String? = null,
    val attachment: ChatAttachmentInput? = null,
)

/** `POST /community/threads` accepts exactly one of these (community.ts 121-122). */
@Serializable
data class CreateChatThreadRequest(
    val bookingId: String? = null,
    val connectionId: String? = null,
)

@Serializable
data class CreateChatThreadResponse(val id: String = "")

@Serializable
data class ChatTypingRequest(val typing: Boolean)

/**
 * `GET /features` — the eleven flags from `api/src/lib/features.ts`. Defaults are `false`: an unknown
 * or failed flag must hide a gated area, never show one that the API will refuse with 503.
 */
@Serializable
data class FeaturesDto(
    val requests: Boolean = false,
    val compare: Boolean = false,
    val achievements: Boolean = false,
    val trust: Boolean = false,
    val projects: Boolean = false,
    val crm: Boolean = false,
    val packages: Boolean = false,
    val business: Boolean = false,
    val pricing: Boolean = false,
    val community: Boolean = false,
    val ai: Boolean = false,
)
