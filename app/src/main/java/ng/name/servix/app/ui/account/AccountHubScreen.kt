package ng.name.servix.app.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ng.name.servix.app.ui.components.ServixBottomSheet
import ng.name.servix.app.data.model.AccountPaymentsResponse
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ng.name.servix.app.ui.a11y.heading
import ng.name.servix.app.ui.liveness.ForegroundRefresh
import ng.name.servix.app.data.repository.retrySessionRead
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import ng.name.servix.app.BuildConfig
import ng.name.servix.app.R
import ng.name.servix.app.data.model.SavedProfessionalItemDto
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.repository.AccountRepository
import ng.name.servix.app.ui.catalogue.EmptyStateCard
import ng.name.servix.app.ui.catalogue.ServixAvatarImage
import ng.name.servix.app.ui.components.ErrorStateCard
import ng.name.servix.app.ui.components.ServixBadge
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.components.ServixTextButton
import ng.name.servix.app.ui.components.ServixTextField
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.updates.UpdateCard
import ng.name.servix.app.updates.UpdateUiState
import ng.name.servix.app.util.CustomTabLauncher
import ng.name.servix.app.util.MediaCacheBust

/** Panels reachable from the Account list. */
enum class AccountPanel { NONE, PROFILE, SAVED, PLAN, SECURITY, DEVICE, UPDATES, LEGAL }

/**
 * Account hub (plan §5): 64 dp avatar header + grouped settings list. Every panel is read-only or
 * website-managed except Profile (PATCH /account/profile) and Saved (GET/DELETE /account/saved).
 */
@Composable
fun AccountHubScreen(
    user: UserDto?,
    accountRepository: AccountRepository,
    updateState: UpdateUiState,
    city: String?,
    unreadNotifications: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onCreateAccountClick: () -> Unit = {},
    onVerifyEmailClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    /** Re-reads GET /me and republishes the shared session state after a profile write. */
    onRefreshSessionUser: suspend () -> Unit = {},
    /** Opening a payment row in the read-only history. */
    onOpenBooking: (String) -> Unit = {},
) {
    var panel by remember { mutableStateOf(AccountPanel.NONE) }
    var paymentsOpen by remember { mutableStateOf(false) }
    var payments by remember { mutableStateOf<AccountPaymentsResponse?>(null) }
    ForegroundRefresh(Triple(user?.id, paymentsOpen, updateState.completedChecks)) {
        if (paymentsOpen && user != null) {
            retrySessionRead({ true }) { accountRepository.getPayments() }.onSuccess { payments = it }
        }
    }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResourceText(R.string.nav_account),
            style = MaterialTheme.typography.headlineMedium,
            color = DeepForest,
            modifier = Modifier.heading(),
        )

        if (user == null) {
            ServixCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sign in to manage your Servix account", style = MaterialTheme.typography.titleMedium, color = Charcoal)
                    Text(
                        "Saved professionals, notifications, plan and security all live here once you sign in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGray,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ServixPrimaryButton(
                            text = stringResourceText(R.string.sign_in),
                            onClick = onSignInClick,
                            modifier = Modifier.weight(1f),
                            testTag = "account_sign_in_button",
                        )
                        ServixSecondaryButton(
                            text = stringResourceText(R.string.create_account),
                            onClick = onCreateAccountClick,
                            modifier = Modifier.weight(1f),
                            testTag = "account_create_account_button",
                        )
                    }
                }
            }
            AccountLegalCard()
            return@Column
        }

        AccountHeader(user = user, onVerifyEmailClick = onVerifyEmailClick)

        if (panel == AccountPanel.NONE) {
            AccountGroup(title = "Account") {
                AccountRow("Profile", "Name and photo", "account_row_profile") { panel = AccountPanel.PROFILE }
                AccountRow("Saved", "Professionals you kept", "account_row_saved") { panel = AccountPanel.SAVED }
                AccountRow("Payments", "Every charge and refund, read-only", "account_row_payments") { paymentsOpen = true }
                AccountRow("Plan", "What your account includes", "account_row_plan") { panel = AccountPanel.PLAN }
                if (user.role == "professional" || user.role == "admin") {
                    AccountRow("Professional tools", "Services and availability on the website", "account_row_protools") {
                        CustomTabLauncher.launchWebPath(context, "/dashboard/services")
                    }
                }
                AccountRow("Security", "Sign-in protection", "account_row_security") { panel = AccountPanel.SECURITY }
            }
            AccountGroup(title = "This device") {
                AccountRow("This device", "Sessions and app version", "account_row_device") { panel = AccountPanel.DEVICE }
                AccountRow("Updates", "App version ${BuildConfig.VERSION_NAME}", "account_row_updates") { panel = AccountPanel.UPDATES }
            }
            AccountGroup(title = "Legal") {
                AccountRow("Terms and privacy", "Both documents in one place", "account_row_legal") { panel = AccountPanel.LEGAL }
                AccountRow("Delete account", "Email support — deletions are confirmed by a person", "account_row_delete") {
                    CustomTabLauncher.launchUrl(context, "mailto:toluenetech@gmail.com?subject=Delete%20my%20Servix%20account")
                }
            }
            ServixSecondaryButton(
                text = stringResourceText(R.string.sign_out),
                onClick = onSignOutClick,
                modifier = Modifier.fillMaxWidth(),
                testTag = "account_sign_out_button",
            )
        } else {
            ServixTextButton(text = "← Back to account") { panel = AccountPanel.NONE }
            when (panel) {
                AccountPanel.PROFILE -> ProfilePanel(
                    user = user,
                    repository = accountRepository,
                    onRefreshSessionUser = onRefreshSessionUser,
                )
                AccountPanel.SAVED -> SavedPanel(repository = accountRepository, refreshKey = updateState.completedChecks)
                AccountPanel.PLAN -> PlanPanel(repository = accountRepository, refreshKey = updateState.completedChecks)
                AccountPanel.SECURITY -> SecurityPanel(repository = accountRepository, refreshKey = updateState.completedChecks)
                AccountPanel.DEVICE -> DeviceCard(repository = accountRepository, userId = user.id, city = city)
                AccountPanel.UPDATES -> {
                    UpdateCard(updateState)
                    if (updateState.enabled) {
                        ServixSecondaryButton(
                            text = "Check for updates",
                            onClick = { updateState.check(force = true) },
                            testTag = "check_updates",
                        )
                    }
                }
                AccountPanel.LEGAL -> AccountLegalCard()
                AccountPanel.NONE -> Unit
            }
        }
    }

    if (paymentsOpen) {
        ServixBottomSheet(title = "Payments", onDismiss = { paymentsOpen = false }) {
            AccountPaymentsSheetContent(
                payments = payments,
                onOpenBooking = { bookingId ->
                    paymentsOpen = false
                    onOpenBooking(bookingId)
                },
                onDismiss = { paymentsOpen = false },
            )
        }
    }
}

@Composable
private fun stringResourceText(id: Int): String = androidx.compose.ui.res.stringResource(id)

@Composable
private fun AccountHeader(user: UserDto, onVerifyEmailClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("account_header"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ServixAvatarImage(rawImageUrl = user.avatarUrl, name = user.fullName, size = 64.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(user.fullName, style = MaterialTheme.typography.titleLarge, color = DeepForest, modifier = Modifier.testTag("account_name"))
            Text(user.email, style = MaterialTheme.typography.bodySmall, color = MutedGray)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ServixBadge(text = user.role.replaceFirstChar { it.uppercase() }, containerColor = SoftSand, contentColor = DeepForest)
                if (!user.emailVerified) {
                    ServixBadge(text = "Verify email", containerColor = SoftSand, contentColor = Charcoal)
                }
            }
        }
    }
    if (!user.emailVerified) {
        ServixPrimaryButton(
            text = stringResourceText(R.string.verify_email_cta),
            onClick = onVerifyEmailClick,
            testTag = "account_verify_email_button",
        )
    }
}

@Composable
internal fun AccountGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = ForestGreen, modifier = Modifier.padding(start = 4.dp))
        ServixCard(contentPadding = PaddingValues(vertical = 4.dp)) {
            Column { content() }
        }
    }
}

@Composable
internal fun AccountRow(
    label: String,
    detail: String,
    testTag: String,
    badge: Int = 0,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = Charcoal)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MutedGray)
        }
        if (badge > 0) {
            Box(
                modifier = Modifier.size(8.dp).background(ForestGreen, CircleShape),
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MutedGray, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = BorderSand.copy(alpha = 0.6f))
}

@Composable
private fun ProfilePanel(
    user: UserDto,
    repository: AccountRepository,
    onRefreshSessionUser: suspend () -> Unit,
) {
    var name by remember(user.id) { mutableStateOf(user.fullName) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    // Start from the account the session already holds, so an existing photo shows immediately.
    var avatarUrl by remember(user.id, user.avatarUrl) { mutableStateOf(user.avatarUrl) }
    var uploading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val invalid = name.trim().length < 2
    val context = LocalContext.current

    // Photo picker: POST /uploads?kind=avatar (5 MB image cap) then PATCH /account/profile { avatarUrl }.
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            uploading = true
            error = null
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: "image/jpeg"
            val bytes = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            when {
                bytes == null -> error = "That photo could not be read. Try a different one."
                bytes.size > 5 * 1024 * 1024 -> error = "Choose a photo under 5 MB."
                !mime.startsWith("image/") -> error = "Choose a JPEG, PNG or WebP photo."
                else -> repository.uploadAvatar(
                    fileName = "avatar.${mime.substringAfter('/').replace("jpeg", "jpg")}",
                    contentType = mime,
                    bytes = bytes,
                ).fold(
                    onSuccess = { url ->
                        repository.updateProfile(avatarUrl = url).fold(
                            onSuccess = {
                                avatarUrl = url
                                message = "Photo updated."
                                // Same URL, new bytes: key Coil off a fresh stamp so every avatar
                                // (header, Home, this panel) reloads instead of hitting its cache.
                                MediaCacheBust.bump(url)
                                // Then re-read /me so the shared session carries the new avatar and
                                // Home + the Account header update without a restart.
                                onRefreshSessionUser()
                            },
                            onFailure = { error = it.message ?: "We could not save that photo." },
                        )
                    },
                    onFailure = { error = it.message ?: "The upload did not finish. Try again." },
                )
            }
            uploading = false
        }
    }

    ServixCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Profile", style = MaterialTheme.typography.titleMedium, color = DeepForest)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ServixAvatarImage(rawImageUrl = avatarUrl, name = user.fullName, size = 56.dp)
                ServixSecondaryButton(
                    text = if (uploading) "Uploading…" else "Change photo",
                    onClick = {
                        pickImage.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                    enabled = !uploading,
                    testTag = "profile_change_photo",
                )
            }
            Text("JPEG, PNG or WebP up to 5 MB.", style = MaterialTheme.typography.bodySmall, color = MutedGray)
            Text("Your name is what professionals see on your bookings.", style = MaterialTheme.typography.bodySmall, color = MutedGray)
            ServixTextField(
                value = name,
                onValueChange = { name = it.take(200); error = null; message = null },
                label = "Full name",
                required = true,
                error = if (invalid && name.isNotEmpty()) "Use at least 2 characters." else null,
                testTag = "profile_full_name",
            )
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ForestGreen) }
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ng.name.servix.app.ui.theme.DangerRed) }
            ServixPrimaryButton(
                text = "Save name",
                enabled = !saving && !invalid && name.trim() != user.fullName,
                loading = saving,
                onClick = {
                    saving = true
                    scope.launch {
                        repository.updateProfile(name.trim()).fold(
                            onSuccess = { message = "Saved. Your name is up to date." },
                            onFailure = { error = it.message ?: "We could not save that." },
                        )
                        saving = false
                    }
                },
                testTag = "profile_save",
            )
        }
    }
}

@Composable
private fun SavedPanel(repository: AccountRepository, refreshKey: Int) {
    var items by remember { mutableStateOf<List<SavedProfessionalItemDto>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var retryKey by remember { mutableStateOf(0) }

    ForegroundRefresh(refreshKey to retryKey) {
        retrySessionRead({ true }) { repository.getSaved() }.fold(
            onSuccess = {
                items = it
                error = null
            },
            onFailure = { if (items == null) error = if ((it as? ng.name.servix.app.data.model.ServixApiException)?.status == 401)
                "We could not load your saved professionals. Please try again." else it.message ?: "We could not load your saved professionals." },
        )
    }

    when {
        error != null -> ErrorStateCard(message = error!!, onRetry = { error = null; retryKey++ })
        items == null -> Text("Loading saved professionals…", style = MaterialTheme.typography.bodySmall, color = MutedGray)
        items!!.isEmpty() -> EmptyStateCard(
            title = "Nothing saved yet",
            message = "Tap Save on a professional profile and they will appear here.",
        )
        else -> items!!.forEach { item ->
            ServixCard(modifier = Modifier.testTag("saved_row_${item.professional.slug}")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ServixAvatarImage(rawImageUrl = item.professional.imageUrl, name = item.professional.name, size = 44.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.professional.name, style = MaterialTheme.typography.titleSmall, color = Charcoal)
                        Text(item.professional.title, style = MaterialTheme.typography.bodySmall, color = MutedGray)
                    }
                    ServixTextButton(text = "Remove") {
                        scope.launch {
                            repository.unsave(item.professional.slug).onSuccess {
                                items = items?.filterNot { saved -> saved.professional.slug == item.professional.slug }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanPanel(repository: AccountRepository, refreshKey: Int) {
    var plan by remember { mutableStateOf<JsonElement?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    ForegroundRefresh(refreshKey) {
        retrySessionRead({ true }) { repository.billingPlan() }.fold(
            onSuccess = {
                plan = it
                error = null
            },
            onFailure = { if (plan == null) error = if ((it as? ng.name.servix.app.data.model.ServixApiException)?.status == 401)
                "We could not load your plan. Please try again." else it.message ?: "We could not load your plan." },
        )
    }

    val obj = plan as? JsonObject
    PlanCard(
        label = obj?.get("label")?.jsonPrimitiveOrNull() ?: obj?.get("current")?.jsonPrimitiveOrNull() ?: "—",
        source = obj?.get("source")?.jsonPrimitiveOrNull(),
        expiresAt = obj?.get("expiresAt")?.jsonPrimitiveOrNull(),
        error = error,
        onManage = { CustomTabLauncher.launchWebPath(context, "/dashboard/plan") },
    )
}

/** Plan card: read-only in the app; upgrades and receipts stay on the website. */
@Composable
fun PlanCard(
    label: String,
    source: String? = null,
    expiresAt: String? = null,
    error: String? = null,
    onManage: () -> Unit = {},
) {
    ServixCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Plan", style = MaterialTheme.typography.titleMedium, color = DeepForest)
            Text(
                label,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = DeepForest,
                modifier = Modifier.testTag("plan_label"),
            )
            Text(
                text = source?.let { "Source: $it" } ?: "Read-only in the app",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
            expiresAt?.let { Text("Renews or ends $it", style = MaterialTheme.typography.bodySmall, color = MutedGray) }
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ng.name.servix.app.ui.theme.DangerRed) }
            Text(
                "Upgrades, downgrades and receipts are handled on the website.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
            ServixSecondaryButton(
                text = "Manage on the website",
                onClick = onManage,
                testTag = "plan_manage_website",
            )
        }
    }
}

@Composable
private fun SecurityPanel(repository: AccountRepository, refreshKey: Int) {
    var lines by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ForegroundRefresh(refreshKey) {
        retrySessionRead({ true }) { repository.getSecuritySummary() }.fold(
            onSuccess = { dto ->
                lines = securityLines(dto)
                error = null
            },
            onFailure = { if (!loaded) error = if ((it as? ng.name.servix.app.data.model.ServixApiException)?.status == 401)
                "We could not load your security summary. Please try again." else it.message ?: "We could not load your security summary." },
        )
        loaded = true
    }

    ServixCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Security", style = MaterialTheme.typography.titleMedium, color = DeepForest)
            if (!loaded) Text("Loading…", style = MaterialTheme.typography.bodySmall, color = MutedGray)
            lines.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = Charcoal)
                    Text(value, style = MaterialTheme.typography.bodyMedium, color = MutedGray)
                }
            }
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ng.name.servix.app.ui.theme.DangerRed) }
            Text(
                "Changing your password, two-factor or passkeys happens on the website.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
            ServixSecondaryButton(
                text = "Manage on the website",
                onClick = { CustomTabLauncher.launchWebPath(context, "/dashboard/security") },
                testTag = "security_manage_website",
            )
        }
    }
}

/** Only fields the API actually returns — nothing invented. */
internal fun securityLines(dto: ng.name.servix.app.data.model.SecuritySummaryDto): List<Pair<String, String>> =
    buildList {
        dto.method?.takeIf { it.isNotBlank() }?.let { add("Sign-in method" to it) }
        add("Recovery codes left" to dto.recoveryCodesRemaining.toString())
        add("Passkeys" to dto.passkeys.toString())
    }

private fun JsonElement.jsonPrimitiveOrNull(): String? =
    runCatching { jsonPrimitive.content }.getOrNull()?.takeIf { it.isNotBlank() }

@Composable
private fun AccountLegalCard() {
    val context = LocalContext.current
    ServixCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Legal", style = MaterialTheme.typography.titleSmall, color = Charcoal)
            Text(
                "Version ${BuildConfig.VERSION_NAME} · API: api.servix.name.ng",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ServixSecondaryButton(
                    text = "Privacy Policy",
                    onClick = { CustomTabLauncher.launchWebPath(context, "/privacy") },
                    modifier = Modifier.weight(1f),
                    testTag = "account_privacy_button",
                )
                ServixSecondaryButton(
                    text = "Terms of Service",
                    onClick = { CustomTabLauncher.launchWebPath(context, "/terms") },
                    modifier = Modifier.weight(1f),
                    testTag = "account_terms_button",
                )
            }
        }
    }
}
