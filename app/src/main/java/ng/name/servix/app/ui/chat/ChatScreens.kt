package ng.name.servix.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ng.name.servix.app.data.model.ChatThreadDto
import ng.name.servix.app.data.realtime.ChatConnection
import ng.name.servix.app.ui.catalogue.ServixAvatarImage
import ng.name.servix.app.ui.components.EmptyState
import ng.name.servix.app.ui.components.ErrorCard
import ng.name.servix.app.ui.components.ServixBadge
import ng.name.servix.app.ui.components.ServixBottomSheet
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.components.ServixTextButton
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory

/** The Chats tab: pinned Servix AI row (when `features.ai`), then threads newest-activity first. */
@Composable
fun ChatsScreen(
    uiState: ChatsUiState,
    myUserId: String,
    onOpenThread: (String) -> Unit,
    onOpenAi: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(WarmIvory)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Chats", style = MaterialTheme.typography.headlineSmall, color = DeepForest)
            ConnectionChip(uiState.connection)
        }
        when {
            uiState.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ForestGreen)
            }
            uiState.error != null && uiState.threads.isEmpty() -> Box(Modifier.padding(20.dp)) {
                ErrorCard(message = uiState.error, onRetry = onRetry)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("chats_list"),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                if (uiState.aiEnabled) {
                    item(key = "ai") {
                        PinnedAiRow(onClick = onOpenAi)
                        HorizontalDivider(color = BorderSand.copy(alpha = 0.6f))
                    }
                }
                if (uiState.threads.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            title = "No chats yet",
                            body = "Open a booking and tap Message, or accept a connection — your conversations appear here.",
                            icon = Icons.Filled.Chat,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                }
                items(uiState.threads, key = { it.id }) { thread ->
                    ChatThreadRow(
                        thread = thread,
                        myUserId = myUserId,
                        onClick = { onOpenThread(thread.id) },
                    )
                    HorizontalDivider(color = BorderSand.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun ConnectionChip(connection: ChatConnection) {
    val (label, tone) = when (connection) {
        ChatConnection.LIVE -> "Live" to ForestGreen
        ChatConnection.CONNECTING -> "Connecting…" to MutedGray
        ChatConnection.RECONNECTING -> "Reconnecting…" to MutedGray
        ChatConnection.UNAVAILABLE -> "Chats unavailable" to MutedGray
        ChatConnection.IDLE -> "Offline" to MutedGray
    }
    ServixBadge(text = label, containerColor = SoftSand, contentColor = tone, modifier = Modifier.testTag("chat_connection"))
}

/** Servix AI pinned as the first conversation (`features.ai`), opening the assistant. */
@Composable
private fun PinnedAiRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("chat_row_ai"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(DeepForest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = WarmIvory, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Servix AI", style = MaterialTheme.typography.titleSmall, color = Charcoal)
            Text("Ask about pricing, scope or your bookings", style = MaterialTheme.typography.bodySmall, color = MutedGray)
        }
        ServixBadge(text = "Pinned", containerColor = SoftSand, contentColor = DeepForest)
    }
}

/**
 * One conversation. Header = avatar + name + presence line ("typing…" > "Online" > "Last seen …" >
 * service title); tapping it opens the contact card.
 */
@Composable
fun ConversationScreen(
    uiState: ConversationUiState,
    thread: ChatThreadDto?,
    onBack: () -> Unit,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: (String) -> Unit,
    onLoadOlder: () -> Unit,
    onOpenContactCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = thread?.person?.fullName.orEmpty().ifBlank { "Conversation" }
    val title = thread?.person?.professionalProfile?.title ?: thread?.booking?.serviceTitle
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.rows.size) {
        if (uiState.rows.isNotEmpty()) listState.animateScrollToItem(uiState.rows.lastIndex)
    }

    Column(modifier = modifier.fillMaxSize().background(WarmIvory).imePadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenContactCard)
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                .testTag("conversation_header"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("conversation_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepForest)
            }
            Box {
                ServixAvatarImage(
                    rawImageUrl = thread?.person?.professionalProfile?.imageUrl,
                    name = name,
                    size = 40.dp,
                )
                if (uiState.online) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ForestGreen),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = DeepForest, maxLines = 1)
                Text(
                    text = presenceLine(uiState, title),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.otherTyping) ForestGreen else MutedGray,
                    maxLines = 1,
                    modifier = Modifier.testTag("conversation_presence"),
                )
            }
            // Affordance for the contact card: the whole header is tappable, so say so.
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Contact details",
                tint = MutedGray,
                modifier = Modifier.size(20.dp).testTag("conversation_contact_cue"),
            )
        }
        HorizontalDivider(color = BorderSand)

        if (uiState.hasMore) {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                ServixTextButton(
                    text = if (uiState.loadingOlder) "Loading…" else "Load earlier messages",
                    onClick = onLoadOlder,
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ForestGreen)
                }
                uiState.error != null && uiState.rows.isEmpty() -> Box(Modifier.padding(20.dp)) {
                    ErrorCard(message = uiState.error, onRetry = { onLoadOlder() })
                }
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().testTag("conversation_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    // Bottom-anchored: a short conversation sits on the composer instead of floating
                    // at the top of an empty screen, and new rows / the IME never leave a gap.
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
                ) {
                    items(uiState.rows) { row ->
                        when (row) {
                            is ChatRow.Day -> ChatDaySeparator(row.label)
                            is ChatRow.Message -> ChatBubble(
                                row = row,
                                tick = uiState.tickFor(row.message),
                                failure = uiState.failureOf(row.message),
                                onRetry = { row.message.clientId?.let(onRetry) },
                            )
                        }
                    }
                }
            }
        }

        if (!uiState.canSend) {
            Text(
                text = "You can no longer message this person.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureWhite)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                TextField(
                    value = uiState.input,
                    onValueChange = onInput,
                    modifier = Modifier.weight(1f).testTag("composer_input"),
                    placeholder = { Text("Message", color = MutedGray) },
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SoftSand,
                        unfocusedContainerColor = SoftSand,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Charcoal,
                        unfocusedTextColor = Charcoal,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                )
                IconButton(
                    onClick = onSend,
                    enabled = uiState.input.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (uiState.input.isNotBlank()) DeepForest else SoftSand)
                        .testTag("composer_send"),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (uiState.input.isNotBlank()) WarmIvory else MutedGray,
                    )
                }
            }
        }
    }
}

private fun presenceLine(uiState: ConversationUiState, title: String?): String = when {
    uiState.otherTyping -> "typing…"
    uiState.online -> "Online"
    !uiState.lastSeenAt.isNullOrBlank() -> ChatTime.lastSeenLabel(uiState.lastSeenAt)
    !title.isNullOrBlank() -> title
    else -> "Connection"
}

/** Contact card sheet: identity, presence, shared media counts, block / report. */
@Composable
fun ChatContactCard(
    thread: ChatThreadDto,
    messages: List<ng.name.servix.app.data.model.ChatMessageDto>,
    onDismiss: () -> Unit,
    onViewProfile: (String) -> Unit,
    onBlock: () -> Unit,
) {
    val name = thread.person?.fullName.orEmpty().ifBlank { "Servix user" }
    val slug = thread.person?.professionalProfile?.slug
    val booking = thread.booking
    val photos = messages.count { it.attachment != null && it.kind == "image" }
    val files = messages.count { it.attachment != null && it.kind == "file" }
    val links = messages.sumOf { ChatText.linksIn(it.body).size }
    ServixBottomSheet(title = name, onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                ServixAvatarImage(
                    rawImageUrl = thread.person?.professionalProfile?.imageUrl,
                    name = name,
                    size = 72.dp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(name, style = MaterialTheme.typography.titleLarge, color = DeepForest)
                    Text(
                        thread.person?.professionalProfile?.title ?: thread.booking?.serviceTitle ?: "Servix member",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                    Text(
                        presenceLine(
                            ConversationUiState(online = thread.online, lastSeenAt = thread.lastSeenAt),
                            thread.person?.professionalProfile?.title,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (thread.online) ForestGreen else MutedGray,
                    )
                }
            }
            booking?.serviceTitle?.let { service ->
                ServixCard(containerColor = SoftSand) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Booking", style = MaterialTheme.typography.labelSmall, color = ForestGreen)
                        Text(service, style = MaterialTheme.typography.titleSmall, color = DeepForest)
                        booking?.reference?.let { reference ->
                            Text("Ref $reference", style = MaterialTheme.typography.bodySmall, color = MutedGray)
                        }
                    }
                }
            }
            Text(
                text = "Shared here · $photos photos · $files files · $links links",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
            if (!slug.isNullOrBlank()) {
                ServixSecondaryButton(text = "View full profile", onClick = { onViewProfile(slug) })
            }
            ServixTextButton(text = "Block this person", onClick = onBlock)
            Text(
                text = "Blocking stops new messages in this conversation. To report a message, use the report option in the web app.",
                style = MaterialTheme.typography.labelSmall,
                color = MutedGray,
            )
        }
    }
}

@Composable
fun ChatsSignedOut(onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(WarmIvory).padding(24.dp)) {
        Spacer(Modifier.height(24.dp))
        EmptyState(
            title = "Sign in to chat",
            body = "Message the professionals you book, and keep every conversation in one place.",
            actionLabel = "Sign in",
            onAction = onSignIn,
            icon = Icons.Filled.Chat,
        )
    }
}
