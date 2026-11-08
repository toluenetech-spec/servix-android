package ng.name.servix.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ng.name.servix.app.ui.ai.AiUiState
import ng.name.servix.app.ui.components.ServixBadge
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.Coral
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory

/**
 * Servix AI pinned as a conversation. It reuses the existing `ai/assistant/stream` client through
 * [AiUiState] and the same bubble language as chat — but it is a model, not a person: the header says
 * so, there are no read ticks (nobody read anything), and a failed turn offers a retry.
 */
@Composable
fun AiThreadScreen(
    state: AiUiState,
    onBack: () -> Unit,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.turns.size, state.streaming) {
        val count = state.turns.size
        if (count > 0) listState.animateScrollToItem(count)
    }
    Column(modifier = modifier.fillMaxSize().background(WarmIvory).imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("ai_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepForest)
            }
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(DeepForest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = WarmIvory, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Servix AI", style = MaterialTheme.typography.titleMedium, color = DeepForest)
                Text(
                    "Assistant — not a person. No message is sent to anyone else.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                )
            }
            ServixBadge(text = "AI", containerColor = SoftSand, contentColor = DeepForest, modifier = Modifier.testTag("ai_badge"))
        }
        HorizontalDivider(color = BorderSand)

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("ai_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
            ) {
                if (state.turns.isEmpty() && state.streaming.isBlank()) {
                    item(key = "empty") {
                        Text(
                            text = "Ask about pricing, scope or how to choose between professionals.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedGray,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                }
                items(state.turns, key = { it.id }) { turn ->
                    AiRow(text = turn.content, mine = turn.role == "user")
                }
                if (state.streaming.isNotBlank()) {
                    item(key = "streaming") { AiRow(text = state.streaming, mine = false) }
                }
                if (state.busy && state.streaming.isBlank()) {
                    item(key = "busy") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                color = ForestGreen,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                            Text("Thinking…", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                        }
                    }
                }
                state.chatError?.let { failure ->
                    item(key = "error") {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = failure.message.ifBlank { "The assistant did not answer." },
                                style = MaterialTheme.typography.labelSmall,
                                color = Coral,
                            )
                            Text(
                                text = "Not answered · Tap to retry",
                                style = MaterialTheme.typography.labelSmall,
                                color = Coral,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SoftSand)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("ai_retry")
                                    .let { base ->
                                        base.then(Modifier)
                                    },
                            )
                        }
                        // Retry is a real button, not styled text pretending to be one.
                        androidx.compose.material3.TextButton(onClick = onRetry) { Text("Try again") }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(PureWhite).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            TextField(
                value = state.input,
                onValueChange = onInput,
                modifier = Modifier.weight(1f).testTag("ai_composer_input"),
                placeholder = { Text("Ask Servix AI", color = MutedGray) },
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
                enabled = state.input.isNotBlank() && !state.busy,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (state.input.isNotBlank() && !state.busy) DeepForest else SoftSand)
                    .testTag("ai_composer_send"),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (state.input.isNotBlank() && !state.busy) WarmIvory else MutedGray,
                )
            }
        }
    }
}

@Composable
private fun AiRow(text: String, mine: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (mine) DeepForest else PureWhite)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag(if (mine) "ai_row_mine" else "ai_row_assistant"),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (mine) WarmIvory else Charcoal,
            )
        }
    }
}
