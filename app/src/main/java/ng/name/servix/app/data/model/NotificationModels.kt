package ng.name.servix.app.data.model

import kotlinx.serialization.Serializable

/**
 * GET /account/notifications (android/reference/api-routes/account.ts:45-51) serialises
 * `{ id, type, title, body, link, readAt, createdAt }`.
 */
@Serializable
data class NotificationDto(
    val id: String,
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val link: String? = null,
    val readAt: String? = null,
    val createdAt: String = "",
)

/** Same route: `{ unread, before, items }`, 30 per page, newest first. */
@Serializable
data class NotificationListResponse(
    val unread: Int = 0,
    val before: String? = null,
    val items: List<NotificationDto> = emptyList(),
)

/** GET /account/notifications/unread (account.ts:53-56) → `{ unread }`. */
@Serializable
data class UnreadCountResponse(
    val unread: Int = 0,
)

/** POST /account/notifications/read (account.ts:57-63): `{ ids }` or `{ all: true }`. */
@Serializable
data class MarkNotificationsReadRequest(
    val ids: List<String>? = null,
    val all: Boolean? = null,
)

@Serializable
data class MarkNotificationsReadResponse(
    val updated: Int = 0,
    val unread: Int = 0,
)
