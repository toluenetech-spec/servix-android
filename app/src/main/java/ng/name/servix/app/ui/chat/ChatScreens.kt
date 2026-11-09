package ng.name.servix.app.ui.chat

import androidx.compose.foundation.background
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import ng.name.servix.app.data.model.ChatMessageDto
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
import ng.name.servix.app.ui.theme.Coral
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
    onSendImage: (ByteArray, String, String?) -> Unit = { _, _, _ -> },
    onSendFile: (ByteArray, String) -> Unit = { _, _ -> },
    onSendVoice: (ByteArray, String, Long) -> Unit = { _, _, _ -> },
    onCancelReply: () -> Unit = {},
    onOpenActions: (ChatMessageDto) -> Unit = {},
    onDismissActions: () -> Unit = {},
    onReply: (ChatMessageDto) -> Unit = {},
    onToggleReaction: (ChatMessageDto, String) -> Unit = { _, _ -> },
    onDeleteMessage: (ChatMessageDto) -> Unit = {},
    onOpenViewer: (String) -> Unit = {},
    onCloseViewer: () -> Unit = {},
    onOpenAttachment: (String) -> Unit = {},
    onSystemMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val name = thread?.person?.fullName.orEmpty().ifBlank { "Conversation" }
    val title = thread?.person?.professionalProfile?.title ?: thread?.booking?.serviceTitle
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember { VoicePlayer() }
    val recorder = remember { VoiceRecorder(context) }
    var playingUrl by remember { mutableStateOf<String?>(null) }
    var positionMs by remember { mutableStateOf(0L) }
    var recording by remember { mutableStateOf(false) }
    var recordMs by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        player.setListener { url, isPlaying, position ->
            playingUrl = if (isPlaying) url else null
            positionMs = position
        }
        onDispose {
            player.stop()
            recorder.cancel()
        }
    }

    LaunchedEffect(uiState.rows.size) {
        if (uiState.rows.isNotEmpty()) listState.animateScrollToItem(uiState.rows.lastIndex)
    }
    LaunchedEffect(recording) {
        while (recording) {
            recorder.tick()
            recordMs = recorder.elapsedMs
            if (recordMs >= MAX_VOICE_MS) {
                recording = false
                recorder.stop()?.let { (bytes, duration) ->
                    onSendVoice(bytes, "voice-note.m4a", duration)
                }
            }
            kotlinx.coroutines.delay(120)
        }
    }

    // System pickers hand bytes + the file name over; the type is sniffed from the bytes before upload.
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = readBytes(context, uri)
            if (bytes != null) onSendImage(bytes, fileName(context, uri, "photo.jpg"), uri.toString())
            else onSystemMessage("That photo could not be read.")
        }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = readBytes(context, uri)
            if (bytes != null) onSendFile(bytes, fileName(context, uri, "file"))
            else onSystemMessage("That file could not be read.")
        }
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
                                myUserId = uiState.myUserId,
                                contactName = name,
                                uploadProgress = row.message.clientId?.let { uiState.uploadProgress[it] },
                                audio = AudioPlayback(
                                    playing = playingUrl != null &&
                                        playingUrl == row.message.attachment?.url,
                                    positionMs = positionMs,
                                    onToggle = {
                                        row.message.attachment?.url?.takeIf { it.isNotBlank() }?.let { url ->
                                            player.toggle(url)
                                        }
                                    },
                                ),
                                onRetry = { row.message.clientId?.let(onRetry) },
                                onToggleReaction = { emoji -> onToggleReaction(row.message, emoji) },
                                onOpenAttachment = { url ->
                                    if (row.message.kind == "image") onOpenViewer(url) else onOpenAttachment(url)
                                },
                                onQuoteTap = { id ->
                                    val index = uiState.rows.indexOfFirst {
                                        it is ChatRow.Message && it.message.id == id
                                    }
                                    if (index >= 0) {
                                        scope.launch { listState.animateScrollToItem(index) }
                                    }
                                },
                                onLongPress = { onOpenActions(row.message) },
                            )
                        }
                    }
                }
            }
        }

        uiState.replyTo?.let { quoted ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftSand)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("composer_reply_banner"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "Replying to ${if (quoted.senderId == uiState.myUserId) "yourself" else name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForestGreen,
                    )
                    Text(
                        text = if (quoted.kind == "text") quoted.body else replyPreviewText(
                            ng.name.servix.app.data.model.ChatReplyDto(kind = quoted.kind, body = quoted.body),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Charcoal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onCancelReply, modifier = Modifier.testTag("composer_reply_cancel")) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel reply", tint = MutedGray, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (recording) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftSand)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("composer_recording"),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Coral))
                Text("Recording ${formatDuration(recordMs)} · release to send", style = MaterialTheme.typography.bodySmall, color = DeepForest)
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
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                IconButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.testTag("composer_attach_image"),
                ) {
                    Icon(Icons.Filled.Image, contentDescription = "Send a photo", tint = DeepForest, modifier = Modifier.size(22.dp))
                }
                IconButton(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.testTag("composer_attach_file"),
                ) {
                    Icon(Icons.Filled.AttachFile, contentDescription = "Send a file", tint = DeepForest, modifier = Modifier.size(22.dp))
                }
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
                if (uiState.input.isBlank()) {
                    // Hold to record; release sends. Nothing is uploaded until the release.
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (recording) Coral else SoftSand)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        if (recorder.start()) {
                                            recording = true
                                            recordMs = 0L
                                            tryAwaitRelease()
                                            recording = false
                                            recorder.stop()?.let { (bytes, duration) ->
                                                onSendVoice(bytes, "voice-note.m4a", duration)
                                            }
                                        }
                                    },
                                )
                            }
                            .testTag("composer_record"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = "Hold to record a voice note",
                            tint = if (recording) WarmIvory else DeepForest,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                } else {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DeepForest)
                            .testTag("composer_send"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = WarmIvory)
                    }
                }
            }
        }
    }

    if (uiState.viewerUrl != null) {
        ImageViewer(url = uiState.viewerUrl, onClose = onCloseViewer)
    }
    uiState.actionTarget?.let { target ->
        ChatActionSheet(
            message = target,
            myUserId = uiState.myUserId,
            reactions = uiState.reactions,
            onReact = { emoji -> onToggleReaction(target, emoji) },
            onReply = { onReply(target) },
            onDelete = { onDeleteMessage(target) },
            onDismiss = onDismissActions,
        )
    }
}

/** Photos down to 8 MB: the API takes 10 MB, and decoding a larger bitmap just to re-encode is waste. */
private const val MAX_VOICE_MS = 5 * 60 * 1000L

private fun readBytes(context: android.content.Context, uri: android.net.Uri): ByteArray? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
}.getOrNull()

private fun fileName(context: android.content.Context, uri: android.net.Uri, fallback: String): String {
    val fromResolver = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()
    return fromResolver?.takeIf { it.isNotBlank() } ?: fallback
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
