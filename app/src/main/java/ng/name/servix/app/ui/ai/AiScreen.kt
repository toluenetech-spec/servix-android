package ng.name.servix.app.ui.ai

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import ng.name.servix.app.ui.motion.Motion
import ng.name.servix.app.ui.motion.motionMs
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ng.name.servix.app.ui.a11y.heading
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import ng.name.servix.app.R
import ng.name.servix.app.data.model.AiUsage
import ng.name.servix.app.data.model.ServixApiException
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.ui.components.FilterChipPill
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.components.ServixSegmentedControl
import ng.name.servix.app.util.CustomTabLauncher
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.ServixFormatters

/**
 * Servix AI (plan §5): real chat layout — bubbles, streaming caret, suggestion chips when empty,
 * composer pinned above the nav bar with IME insets, thin usage bar in the top bar, rounded icon
 * buttons under answers, Budget guide as a segment.
 */
@Composable
fun AiScreen(user: UserDto?, state: AiUiState, viewModel: AiViewModel, onSignIn: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf("assistant") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("ai_screen"),
    ) {
        AiTopBar(
            state = state,
            tab = tab,
            onTab = { tab = it },
        )
        when {
            user == null -> Column(Modifier.padding(16.dp)) {
                ServixCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Sign in to use Servix AI.", style = MaterialTheme.typography.titleSmall, color = Charcoal)
                        ServixPrimaryButton(
                            text = stringResource(R.string.sign_in),
                            onClick = onSignIn,
                            testTag = "ai_sign_in_button",
                        )
                    }
                }
            }
            state.ownerId != user.id -> Text(
                text = "Loading Servix AI…",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
                modifier = Modifier.padding(16.dp),
            )
            state.usage?.enabled == false -> Text(
                text = "Servix AI helpers are not enabled on this account yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = Charcoal,
                modifier = Modifier.padding(16.dp),
            )
            tab == "assistant" -> AssistantChat(state = state, vm = viewModel, modifier = Modifier.weight(1f))
            else -> Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) { BudgetPanel(state, viewModel) }
        }
        Text(
            text = stringResource(R.string.ai_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = MutedGray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun AiTopBar(state: AiUiState, tab: String, onTab: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Servix AI",
                style = MaterialTheme.typography.titleLarge,
                color = DeepForest,
                modifier = Modifier.weight(1f).heading(),
            )
            state.usage?.let { usage ->
                Text(
                    text = usageLine(usage),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                    modifier = Modifier.testTag("ai_usage_label"),
                )
            }
        }
        // Thin usage bar: how much of this month's allowance is gone.
        val percent = state.usage?.percent?.div(100.0)?.toFloat()?.coerceIn(0f, 1f)
        if (percent != null) {
            LinearProgressIndicator(
                progress = { percent },
                modifier = Modifier.fillMaxWidth().heightIn(min = 3.dp).testTag("ai_usage_bar"),
                color = if (percent > 0.9f) ForestGreen else DeepForest,
                trackColor = SoftSand,
            )
        }
        ServixSegmentedControl(
            options = listOf(
                "assistant" to "Assistant",
                "pricing" to "Budget guide",
            ),
            selectedId = tab,
            onSelect = onTab,
        )
    }
}

private fun usageLine(usage: AiUsage): String {
    val format = NumberFormat.getIntegerInstance(Locale.US)
    return if (usage.allowed == null) {
        "${format.format(usage.used)} tokens used"
    } else {
        "${format.format(usage.used)} / ${format.format(usage.allowed)}"
    }
}

@Composable
private fun AssistantChat(state: AiUiState, vm: AiViewModel, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    LaunchedEffect(state.turns.size, state.streaming) {
        val target = state.turns.size + if (state.busy) 1 else 0
        if (target > 1) listState.animateScrollToItem(target - 1)
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("ai_messages"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.turns.isEmpty() && !state.busy) {
                item(key = "intro") {
                    ChatBubble(
                        text = lightMarkdown("Hi — I’m Servix AI. I look things up in real Servix data before I answer."),
                        mine = false,
                    )
                }
                item(key = "suggestions") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        SUGGESTIONS.forEach { prompt ->
                            FilterChipPill(
                                label = prompt,
                                onClick = { vm.send(prompt) },
                                modifier = Modifier.testTag("ai_suggestion"),
                            )
                        }
                    }
                }
            }
            items(state.turns, key = { it.id }) { turn ->
                Column(horizontalAlignment = if (turn.role == "user") Alignment.End else Alignment.Start) {
                    ChatBubble(text = lightMarkdown(turn.content), mine = turn.role == "user")
                    if (turn.role == "assistant") FeedbackActions(turn, vm, clipboard)
                }
            }
            if (state.busy) {
                item(key = "streaming") {
                    Column(horizontalAlignment = Alignment.Start) {
                        ChatBubble(
                            text = lightMarkdown(state.streaming.ifBlank { "Looking that up…" }),
                            mine = false,
                            testTag = "ai_streaming_bubble",
                            caret = true,
                        )
                        TextButton(onClick = vm::stop) { Text("Stop", style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
            state.chatError?.let { error ->
                item(key = "error") { AiError(error, onRetry = vm::retry) }
            }
        }

        // Composer pinned above the nav bar; imePadding keeps it above the keyboard.
        Surface(
            color = WarmIvory,
            border = BorderStroke(1.dp, BorderSand),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = vm::updateInput,
                    placeholder = { Text("Ask Servix AI…") },
                    modifier = Modifier.weight(1f).testTag("ai_question"),
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                )
                Surface(
                    onClick = { vm.send() },
                    enabled = !state.busy && state.input.isNotBlank(),
                    shape = CircleShape,
                    color = if (!state.busy && state.input.isNotBlank()) DeepForest else SoftSand,
                    modifier = Modifier.size(48.dp).testTag("ai_send"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send",
                            tint = if (!state.busy && state.input.isNotBlank()) WarmIvory else MutedGray,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

private val SUGGESTIONS = listOf(
    "Who can design a logo for a small bakery in Lagos?",
    "How does payment protection work on Servix?",
    "What does “delivered” mean on my booking?",
)

@Composable
private fun ChatBubble(text: AnnotatedString, mine: Boolean, testTag: String? = null, caret: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (mine) DeepForest else PureWhite,
            contentColor = if (mine) WarmIvory else Charcoal,
            border = if (mine) null else BorderStroke(1.dp, BorderSand),
            shape = if (mine) {
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
            } else {
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
            },
            modifier = Modifier
                .widthIn(max = 300.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        ) {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
                if (caret) BlinkingCaret()
            }
        }
    }
}

/** Streaming caret: 600 ms blink, no bounce; static when animations are off. */
@Composable
private fun BlinkingCaret() {
    val ms = motionMs(Motion.CARET_MS)
    val transition = rememberInfiniteTransition(label = "caret")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(ms, easing = LinearEasing), RepeatMode.Reverse),
        label = "caretAlpha",
    )
    Text(
        text = "▍",
        style = MaterialTheme.typography.bodyMedium,
        color = DeepForest,
        modifier = Modifier.graphicsLayer { this.alpha = if (ms == 0) 1f else alpha },
    )
}

/** Light Markdown only; rendered as text, never HTML. */
fun lightMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val source = text.replace(Regex("(?m)^\\s*[-*] "), "• ")
    var cursor = 0
    val bold = Regex("\\*\\*(.+?)\\*\\*", RegexOption.DOT_MATCHES_ALL)
    for (match in bold.findAll(source)) {
        append(source.substring(cursor, match.range.first))
        val start = length
        append(match.groupValues[1])
        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
        cursor = match.range.last + 1
    }
    append(source.substring(cursor))
}

/** Rounded icon buttons under each answer: copy, helpful, not helpful. */
@Composable
private fun FeedbackActions(turn: ChatTurn, vm: AiViewModel, clipboard: ClipboardManager) {
    var asking by remember(turn.id) { mutableStateOf(false) }
    var comment by rememberSaveable(turn.id) { mutableStateOf("") }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 6.dp),
    ) {
        IconPill(onClick = { clipboard.setText(AnnotatedString(turn.content)) }) {
            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy answer", tint = MutedGray, modifier = Modifier.size(16.dp))
        }
        when (turn.feedback) {
            "sent" -> Text("Thanks for the feedback.", style = MaterialTheme.typography.labelSmall, color = MutedGray)
            "sending" -> Text("Sending…", style = MaterialTheme.typography.labelSmall, color = MutedGray)
            else -> {
                IconPill(onClick = { vm.feedback(turn.id, "up") }) {
                    Icon(Icons.Rounded.ThumbUp, contentDescription = "Helpful", tint = MutedGray, modifier = Modifier.size(16.dp))
                }
                IconPill(onClick = { asking = true }) {
                    Icon(Icons.Rounded.ThumbDown, contentDescription = "Not helpful", tint = MutedGray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
    turn.feedbackError?.let { Text("Couldn't send feedback: $it", style = MaterialTheme.typography.labelSmall, color = MutedGray) }
    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text("What was wrong?") },
            text = {
                Column {
                    Text("Your question and this answer will be shared with the Servix team so we can fix it.")
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it.take(500) },
                        label = { Text("Comment (optional)") },
                    )
                }
            },
            confirmButton = { TextButton(onClick = { vm.feedback(turn.id, "down", comment); asking = false }) { Text("Send") } },
            dismissButton = { TextButton(onClick = { asking = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun IconPill(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = SoftSand,
        modifier = Modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun BudgetPanel(state: AiUiState, vm: AiViewModel) {
    var expanded by remember { mutableStateOf(false) }
    ServixCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Budget guide", style = MaterialTheme.typography.titleMedium, color = DeepForest)
            Text(
                "Based on the prices professionals actually list on Servix in that category. When Servix doesn’t have enough data yet, it says so instead of guessing.",
                style = MaterialTheme.typography.bodyMedium,
                color = Charcoal,
            )
            Text("Category", style = MaterialTheme.typography.labelMedium)
            Box {
                ServixSecondaryButton(
                    text = state.categories.find { it.id == state.category }?.name ?: "Choose a category",
                    onClick = { expanded = true },
                    enabled = state.categories.isNotEmpty() && !state.pricingBusy,
                    testTag = "ai_category",
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    state.categories.forEach { category ->
                        DropdownMenuItem(text = { Text(category.name) }, onClick = { vm.selectCategory(category.id); expanded = false })
                    }
                }
            }
            state.categoriesError?.let { Text("Categories could not be loaded: $it"); TextButton(onClick = vm::reload) { Text("Try again") } }
            if (state.categories.isEmpty() && state.categoriesError == null) Text("No categories available yet.")
            OutlinedTextField(
                value = state.days,
                onValueChange = vm::updateDays,
                label = { Text("Delivery (days, optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !state.pricingBusy,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.brief,
                onValueChange = vm::updateBrief,
                label = { Text("Describe the job (optional)") },
                enabled = !state.pricingBusy,
                placeholder = { Text("e.g. 5-page website with contact form and basic SEO") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            ServixPrimaryButton(
                text = "Get guidance",
                onClick = vm::getPricing,
                enabled = state.category.isNotBlank() && !state.pricingBusy,
                testTag = "ai_pricing_submit",
            )
            if (state.pricingBusy) Text("Checking Servix prices…")
            state.pricingError?.let { error -> AiError(error, onRetry = vm::getPricing) }
            state.pricing?.let { pricing ->
                if (pricing.basis == "servix_data" && pricing.rangeLow != null && pricing.rangeHigh != null) {
                    Text(
                        "${ServixFormatters.formatNaira(pricing.rangeLow)} – ${ServixFormatters.formatNaira(pricing.rangeHigh)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepForest,
                    )
                } else {
                    Text("Not enough Servix data yet", style = MaterialTheme.typography.titleSmall)
                }
                Text(lightMarkdown(pricing.summary))
                pricing.stats?.takeIf { !it.insufficient && it.median != null && it.min != null && it.max != null }?.let { stats ->
                    Text("Servix listings in this category: ${stats.count} gigs · median ${ServixFormatters.formatNaira(stats.median)} · from ${ServixFormatters.formatNaira(stats.min)} to ${ServixFormatters.formatNaira(stats.max)}.")
                }
                pricing.tips.forEach { Text("• $it") }
                Text("Ranges are clamped to real Servix prices — they are guidance, not a quote.", style = MaterialTheme.typography.bodySmall, color = MutedGray)
            }
        }
    }
}

@Composable
private fun AiError(error: ServixApiException, onRetry: () -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(error.message, style = MaterialTheme.typography.bodyMedium, color = Charcoal)
        Row {
            TextButton(onClick = onRetry) { Text("Try again") }
            if (error.code == "AI_QUOTA_EXCEEDED" || error.code == "PLAN_FEATURE") {
                TextButton(onClick = { CustomTabLauncher.launchWebPath(context, "/dashboard/plan") }) { Text("Manage plan on website") }
            }
        }
    }
}
