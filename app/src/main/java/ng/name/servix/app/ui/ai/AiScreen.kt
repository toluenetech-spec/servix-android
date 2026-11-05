package ng.name.servix.app.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import androidx.compose.foundation.layout.size
import ng.name.servix.app.R
import ng.name.servix.app.data.model.AiMeter
import ng.name.servix.app.data.model.AiUsage
import ng.name.servix.app.data.model.ServixApiException
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.components.ServixTextField
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.util.CustomTabLauncher
import ng.name.servix.app.util.ServixFormatters
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AiScreen(user: UserDto?, state: AiUiState, viewModel: AiViewModel, onSignIn: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf("assistant") }
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).testTag("ai_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(if (user != null) "Good to see you, ${user.firstName}." else "Your AI helper", style = MaterialTheme.typography.headlineMedium, color = DeepForest)
        Text("Ask anything about professionals, prices or how Servix works, and get budget guidance from real Servix listings.", style = MaterialTheme.typography.bodyMedium, color = Charcoal)
        Text("Private to your account · never books, pays or edits anything for you", style = MaterialTheme.typography.bodySmall, color = MutedGray)
        if (user == null) {
            ServixCard { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sign in to use Servix AI.")
                ServixPrimaryButton(text = stringResource(R.string.sign_in), onClick = onSignIn, testTag = "ai_sign_in_button")
            } }
        } else if (state.ownerId != user.id) {
            Text("Loading Servix AI…")
        } else {
            state.usage?.let { UsageCard(it) }
            if (state.usageError != null && state.usage == null) {
                Text("AI usage could not be loaded: ${state.usageError}", style = MaterialTheme.typography.bodySmall, color = MutedGray)
                TextButton(onClick = viewModel::reload) { Text("Try again") }
            }
            if (state.usage?.enabled == false) {
                ServixCard { Text("Servix AI helpers are not enabled on this account yet.") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (tab == "assistant") ServixPrimaryButton("Assistant", onClick = { tab = "assistant" }) else ServixSecondaryButton("Assistant", onClick = { tab = "assistant" })
                    if (tab == "pricing") ServixPrimaryButton("Budget guide", onClick = { tab = "pricing" }) else ServixSecondaryButton("Budget guide", onClick = { tab = "pricing" })
                }
                if (tab == "assistant") AssistantPanel(state, viewModel, clipboard) else BudgetPanel(state, viewModel)
            }
        }
        Text(stringResource(R.string.ai_disclaimer), style = MaterialTheme.typography.bodySmall, color = MutedGray)
    }
}

@Composable
private fun UsageCard(usage: AiUsage) {
    ServixCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Your Servix AI this month", style = MaterialTheme.typography.titleMedium, color = DeepForest)
        Meter(if (usage.subject == "org") "Team AI token pool" else "${usage.planLabel ?: ""} plan AI tokens".trim(), usage.used, usage.allowed, usage.percent, usage.level)
        usage.member?.takeIf { it.allowed != null }?.let { Meter("Your personal cap", it.used, it.allowed, it.percent, it.level) }
        Text("Tokens are counted on the server for every AI request (prompt + answer). Failed requests are never charged.", style = MaterialTheme.typography.bodySmall, color = MutedGray)
    } }
}

@Composable
private fun Meter(label: String, used: Long, allowed: Long?, percent: Double?, level: String?) {
    val format = NumberFormat.getIntegerInstance(Locale.US)
    Text(if (allowed == null) "$label: ${format.format(used)} used" else "$label: ${format.format(used)} / ${format.format(allowed)} used", style = MaterialTheme.typography.bodySmall)
    if (percent != null) LinearProgressIndicator(progress = { (percent / 100.0).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
    if (level in listOf("warning", "critical", "exhausted")) Text(if (level == "exhausted") "Your AI allowance is used up." else "Your AI allowance is running low.", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun AssistantPanel(state: AiUiState, vm: AiViewModel, clipboard: ClipboardManager) {
    ServixCard { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Assistant", style = MaterialTheme.typography.titleMedium, color = DeepForest)
        Text("Ask in plain language. Servix AI looks things up in real Servix data before it answers.", style = MaterialTheme.typography.bodyMedium)
        if (state.turns.isEmpty() && !state.busy) {
            Text("Hi — I’m Servix AI.")
            listOf("Who can design a logo for a small bakery in Lagos?", "How does payment protection work on Servix?", "What does “delivered” mean on my booking?").forEach { prompt ->
                TextButton(onClick = { vm.send(prompt) }) { Text(prompt) }
            }
        }
        state.turns.forEach { turn ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                Text(if (turn.role == "user") "You" else "Servix AI", style = MaterialTheme.typography.labelMedium, color = ForestGreen)
                Text(lightMarkdown(turn.content), style = MaterialTheme.typography.bodyMedium, color = Charcoal)
                if (turn.role == "assistant") FeedbackActions(turn, vm, clipboard)
            }
        }
        if (state.busy) {
            Text(if (state.streaming.isBlank()) "Looking that up…" else state.streaming, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = vm::stop) { Text("Stop") }
        }
        state.chatError?.let { error -> AiError(error, onRetry = vm::retry) }
        OutlinedTextField(value = state.input, onValueChange = vm::updateInput, label = { Text("Your question") }, placeholder = { Text("Ask Servix AI…") }, modifier = Modifier.fillMaxWidth().testTag("ai_question"), minLines = 2, maxLines = 5)
        ServixPrimaryButton(text = "Send", enabled = !state.busy && state.input.isNotBlank(), onClick = { vm.send() }, testTag = "ai_send")
    } }
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

@Composable
private fun FeedbackActions(turn: ChatTurn, vm: AiViewModel, clipboard: ClipboardManager) {
    var asking by remember(turn.id) { mutableStateOf(false) }
    var comment by rememberSaveable(turn.id) { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = { clipboard.setText(AnnotatedString(turn.content)) }) {
            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy answer", tint = MutedGray, modifier = Modifier.size(20.dp))
        }
        if (turn.feedback == "sent") Text("Thanks for the feedback.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 14.dp))
        else if (turn.feedback == "sending") Text("Sending…", modifier = Modifier.padding(top = 14.dp))
        else {
            IconButton(onClick = { vm.feedback(turn.id, "up") }) {
                Icon(Icons.Rounded.ThumbUp, contentDescription = "Helpful", tint = MutedGray, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { asking = true }) {
                Icon(Icons.Rounded.ThumbDown, contentDescription = "Not helpful", tint = MutedGray, modifier = Modifier.size(20.dp))
            }
        }
    }
    turn.feedbackError?.let { Text("Couldn't send feedback: $it", style = MaterialTheme.typography.bodySmall) }
    if (asking) AlertDialog(
        onDismissRequest = { asking = false },
        title = { Text("What was wrong?") },
        text = { Column {
            Text("Your question and this answer will be shared with the Servix team so we can fix it.")
            OutlinedTextField(value = comment, onValueChange = { comment = it.take(500) }, label = { Text("Comment (optional)") })
        } },
        confirmButton = { TextButton(onClick = { vm.feedback(turn.id, "down", comment); asking = false }) { Text("Send") } },
        dismissButton = { TextButton(onClick = { asking = false }) { Text("Cancel") } },
    )
}

@Composable
private fun BudgetPanel(state: AiUiState, vm: AiViewModel) {
    var expanded by remember { mutableStateOf(false) }
    ServixCard { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Budget guide", style = MaterialTheme.typography.titleMedium, color = DeepForest)
        Text("Based on the prices professionals actually list on Servix in that category. When Servix doesn’t have enough data yet, it says so instead of guessing.", style = MaterialTheme.typography.bodyMedium)
        Text("Category", style = MaterialTheme.typography.labelMedium)
        androidx.compose.foundation.layout.Box {
            ServixSecondaryButton(text = state.categories.find { it.id == state.category }?.name ?: "Choose a category", onClick = { expanded = true }, enabled = state.categories.isNotEmpty() && !state.pricingBusy, testTag = "ai_category")
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.categories.forEach { category -> DropdownMenuItem(text = { Text(category.name) }, onClick = { vm.selectCategory(category.id); expanded = false }) }
            }
        }
        state.categoriesError?.let { Text("Categories could not be loaded: $it"); TextButton(onClick = vm::reload) { Text("Try again") } }
        if (state.categories.isEmpty() && state.categoriesError == null) Text("No categories available yet.")
        OutlinedTextField(value = state.days, onValueChange = vm::updateDays, label = { Text("Delivery (days, optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !state.pricingBusy, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = state.brief, onValueChange = vm::updateBrief, label = { Text("Describe the job (optional)") }, enabled = !state.pricingBusy, placeholder = { Text("e.g. 5-page website with contact form and basic SEO") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        ServixPrimaryButton("Get guidance", onClick = vm::getPricing, enabled = state.category.isNotBlank() && !state.pricingBusy, testTag = "ai_pricing_submit")
        if (state.pricingBusy) Text("Checking Servix prices…")
        state.pricingError?.let { error -> AiError(error, onRetry = vm::getPricing) }
        state.pricing?.let { pricing ->
            if (pricing.basis == "servix_data" && pricing.rangeLow != null && pricing.rangeHigh != null) {
                Text("${ServixFormatters.formatNaira(pricing.rangeLow)} – ${ServixFormatters.formatNaira(pricing.rangeHigh)}", style = MaterialTheme.typography.titleLarge, color = DeepForest)
            } else Text("Not enough Servix data yet", style = MaterialTheme.typography.titleSmall)
            Text(lightMarkdown(pricing.summary))
            pricing.stats?.takeIf { !it.insufficient && it.median != null && it.min != null && it.max != null }?.let { stats ->
                Text("Servix listings in this category: ${stats.count} gigs · median ${ServixFormatters.formatNaira(stats.median)} · from ${ServixFormatters.formatNaira(stats.min)} to ${ServixFormatters.formatNaira(stats.max)}.")
            }
            pricing.tips.forEach { Text("• $it") }
            Text("Ranges are clamped to real Servix prices — they are guidance, not a quote.", style = MaterialTheme.typography.bodySmall, color = MutedGray)
        }
    } }
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
