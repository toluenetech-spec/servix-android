package ng.name.servix.app.ui.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import ng.name.servix.app.ui.theme.ServixButtonRadius
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.name.servix.app.R
import ng.name.servix.app.ui.components.ServixBadge
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixLogoMark
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.components.ServixTextField
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DangerRed
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.SurfaceContainerHigh
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.CustomTabLauncher


/**
 * Provider sign-in buttons (plan pass 3 b). Google/GitHub brand marks, Secondary style, above the
 * email form with an "or" divider; hidden entirely when the API reports no live providers
 * (GET /auth/oauth/config -> { providers }).
 */
@Composable
fun OAuthButtons(
    providers: List<String>,
    enabled: Boolean = true,
    disabledHint: String? = null,
    busy: Boolean = false,
    onProviderClick: (String) -> Unit,
) {
    if (providers.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        providers.forEach { provider ->
            val isGoogle = provider.equals("google", ignoreCase = true)
            val label = if (isGoogle) {
                stringResource(R.string.auth_continue_google)
            } else {
                stringResource(R.string.auth_continue_github)
            }
            Button(
                onClick = { onProviderClick(provider) },
                enabled = enabled && !busy,
                shape = RoundedCornerShape(ServixButtonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftSand,
                    contentColor = Charcoal,
                    disabledContainerColor = SoftSand.copy(alpha = 0.5f),
                    disabledContentColor = MutedGray,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag(if (isGoogle) "oauth_google_button" else "oauth_github_button"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ProviderMark(google = isGoogle)
                    Text(text = label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (!enabled && !disabledHint.isNullOrBlank()) {
            Text(
                text = disabledHint,
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
        }
    }
}

/** Officially recognised brand marks, drawn as vectors so no image assets are needed. */
@Composable
private fun ProviderMark(google: Boolean) {
    if (google) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = Color(0xFF4285F4), radius = r, center = c)
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 45f,
                sweepAngle = 90f,
                useCenter = true,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(r * 2, r * 2),
            )
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 135f,
                sweepAngle = 90f,
                useCenter = true,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(r * 2, r * 2),
            )
            drawArc(
                color = Color(0xFFEA4335),
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = true,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(r * 2, r * 2),
            )
            drawCircle(color = WarmIvory, radius = r * 0.55f, center = c)
        }
    } else {
        Canvas(modifier = Modifier.size(18.dp)) {
            drawCircle(color = Color(0xFF24292F), radius = size.minDimension / 2f)
        }
    }
}

/** "or" rule between provider buttons and the email form. */
@Composable
fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSand)
        Text(
            text = stringResource(R.string.auth_or_divider),
            style = MaterialTheme.typography.labelMedium,
            color = MutedGray,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSand)
    }
}

@Composable
fun WelcomeScreen(
    onSignInClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onBrowseFirstClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    providers: List<String> = emptyList(),
    onProviderClick: (String) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmIvory)
            .verticalScroll(rememberScrollState())
            // Design gutter: px-margin is 16 dp, and the hero sits under the status bar by space-xl.
            .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ServixLogoMark()

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.welcome_headline),
                style = MaterialTheme.typography.displaySmall,
                color = DeepForest,
                modifier = Modifier.testTag("welcome_hero_title"),
            )

            Text(
                text = stringResource(R.string.welcome_subtext),
                style = MaterialTheme.typography.bodyLarge,
                color = MutedGray,
                modifier = Modifier.testTag("welcome_subtext"),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ServixPrimaryButton(
                text = stringResource(R.string.welcome_create_account),
                onClick = onCreateAccountClick,
                modifier = Modifier.fillMaxWidth(),
                testTag = "welcome_create_account_button",
            )
            ServixSecondaryButton(
                text = stringResource(R.string.welcome_sign_in),
                onClick = onSignInClick,
                modifier = Modifier.fillMaxWidth(),
                testTag = "welcome_sign_in_button",
            )
            if (providers.isNotEmpty()) {
                OrDivider()
                OAuthButtons(providers = providers, onProviderClick = onProviderClick)
            }
            if (onBrowseFirstClick != null) {
                TextButton(
                    onClick = onBrowseFirstClick,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("welcome_browse_button"),
                ) {
                    Text(
                        text = stringResource(R.string.welcome_browse_first),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline,
                        ),
                        color = ForestGreen,
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthShellContainer(
    onBackClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmIvory)
            .imePadding()
            .verticalScroll(rememberScrollState())
            // One gutter for the whole auth flow: px-margin is 16 dp in every Stitch auth screen, and
            // the shell is shared by S3, S4, S5, S6 and S7, so they cannot drift apart.
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("auth_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Charcoal,
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
            ServixLogoMark()
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        ServixCard(
            modifier = Modifier.widthIn(max = 520.dp),
            containerColor = PureWhite,
            contentPadding = PaddingValues(24.dp),
        ) {
            content()
        }
    }
}

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onBackClick: () -> Unit,
    providers: List<String> = emptyList(),
    onProviderClick: (String) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    var showPassword by rememberSaveable { mutableStateOf(false) }

    AuthShellContainer(onBackClick = onBackClick) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
                color = DeepForest,
            )
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )

            if (providers.isNotEmpty()) {
                OAuthButtons(
                    providers = providers,
                    busy = uiState.oauthBusy,
                    onProviderClick = onProviderClick,
                )
                OrDivider()
            }

            ServixTextField(
                value = uiState.loginEmail,
                onValueChange = onEmailChange,
                label = stringResource(R.string.field_email),
                required = true,
                error = uiState.loginEmailError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                testTag = "login_email_input",
            )

            ServixTextField(
                value = uiState.loginPassword,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.field_password),
                required = true,
                error = uiState.loginPasswordError,
                isPassword = true,
                passwordVisible = showPassword,
                onTogglePasswordVisibility = { showPassword = !showPassword },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onSubmit()
                    }
                ),
                testTag = "login_password_input",
            )

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(
                    onClick = onForgotPasswordClick,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("forgot_password_button"),
                ) {
                    Text(
                        text = stringResource(R.string.forgot_password_link),
                        style = MaterialTheme.typography.labelLarge,
                        color = ForestGreen,
                    )
                }
            }

            if (!uiState.loginGeneralError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DangerRed.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = uiState.loginGeneralError,
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerRed,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            ServixPrimaryButton(
                text = stringResource(if (uiState.loginSubmitting) R.string.signing_in else R.string.sign_in),
                onClick = {
                    focusManager.clearFocus()
                    onSubmit()
                },
                loading = uiState.loginSubmitting,
                modifier = Modifier.fillMaxWidth(),
                testTag = "login_submit_button",
            )

            // s3_sign_in's trust pair: a pill that says what protects the account, and one honest
            // sentence about money — never the word the owner removed.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(shape = RoundedCornerShape(999.dp), color = SurfaceContainerHigh) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = ForestGreen,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Protected by two-step sign-in",
                            style = MaterialTheme.typography.labelMedium,
                            color = Charcoal,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.payment_protection_copy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                    textAlign = TextAlign.Center,
                )
            }

            HorizontalDivider(color = SoftSand)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.new_to_servix),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                )
                TextButton(
                    onClick = onCreateAccountClick,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("go_to_register_button"),
                ) {
                    Text(
                        text = stringResource(R.string.create_account),
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepForest,
                    )
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTermsAcceptedChange: (Boolean) -> Unit = {},
    onSubmit: () -> Unit,
    onSignInClick: () -> Unit,
    onBackClick: () -> Unit,
    providers: List<String> = emptyList(),
    onProviderClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showPassword by rememberSaveable { mutableStateOf(false) }

    val confirmHint = when {
        uiState.registerConfirmPassword.isEmpty() -> stringResource(R.string.confirm_password_hint_empty)
        uiState.registerPassword.isNotEmpty() && uiState.registerPassword == uiState.registerConfirmPassword ->
            stringResource(R.string.confirm_password_hint_match)
        else -> stringResource(R.string.confirm_password_hint_mismatch)
    }

    AuthShellContainer(onBackClick = onBackClick) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineMedium,
                color = DeepForest,
            )
            Text(
                text = stringResource(R.string.register_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )

            ServixTextField(
                value = uiState.registerFullName,
                onValueChange = onNameChange,
                label = stringResource(R.string.field_full_name),
                required = true,
                error = uiState.registerNameError,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                testTag = "register_name_input",
            )

            if (providers.isNotEmpty()) {
                OAuthButtons(
                    providers = providers,
                    enabled = uiState.registerTermsAccepted,
                    disabledHint = "Tick the terms below to continue with a provider.",
                    busy = uiState.oauthBusy,
                    onProviderClick = onProviderClick,
                )
                OrDivider()
            }

            ServixTextField(
                value = uiState.registerEmail,
                onValueChange = onEmailChange,
                label = stringResource(R.string.field_email),
                required = true,
                error = uiState.registerEmailError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                testTag = "register_email_input",
            )

            ServixTextField(
                value = uiState.registerPassword,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.field_password),
                required = true,
                hint = stringResource(R.string.password_hint),
                error = uiState.registerPasswordError,
                isPassword = true,
                passwordVisible = showPassword,
                onTogglePasswordVisibility = { showPassword = !showPassword },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                testTag = "register_password_input",
            )

            PasswordChecklist(rules = uiState.passwordRules)

            ServixTextField(
                value = uiState.registerConfirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = stringResource(R.string.field_confirm_password),
                required = true,
                hint = confirmHint,
                error = uiState.registerConfirmError,
                isPassword = true,
                passwordVisible = showPassword,
                onTogglePasswordVisibility = { showPassword = !showPassword },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (uiState.registerTermsAccepted) {
                            onSubmit()
                        }
                    }
                ),
                testTag = "register_confirm_password_input",
            )

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = SoftSand.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, BorderSand),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = uiState.registerTermsAccepted,
                        onCheckedChange = onTermsAcceptedChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = DeepForest,
                            uncheckedColor = MutedGray,
                            checkmarkColor = PureWhite,
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("register_terms_checkbox"),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.register_terms_checkbox_full),
                            style = MaterialTheme.typography.bodySmall,
                            color = Charcoal,
                            modifier = Modifier
                                .clickable { onTermsAcceptedChange(!uiState.registerTermsAccepted) }
                                .testTag("register_terms_label"),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.register_terms_link),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = TextDecoration.Underline,
                                ),
                                color = DeepForest,
                                modifier = Modifier
                                    .clickable {
                                        CustomTabLauncher.launchUrl(context, "https://www.servix.name.ng/terms")
                                    }
                                    .padding(vertical = 4.dp)
                                    .testTag("register_terms_link"),
                            )
                            Text(
                                text = stringResource(R.string.register_privacy_link),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = TextDecoration.Underline,
                                ),
                                color = DeepForest,
                                modifier = Modifier
                                    .clickable {
                                        CustomTabLauncher.launchUrl(context, "https://www.servix.name.ng/privacy")
                                    }
                                    .padding(vertical = 4.dp)
                                    .testTag("register_privacy_link"),
                            )
                        }
                    }
                }
            }

            if (!uiState.registerGeneralError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DangerRed.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = uiState.registerGeneralError,
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerRed,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            ServixPrimaryButton(
                text = stringResource(if (uiState.registerSubmitting) R.string.creating_account else R.string.create_account),
                onClick = {
                    focusManager.clearFocus()
                    onSubmit()
                },
                enabled = uiState.registerTermsAccepted && !uiState.registerSubmitting,
                loading = uiState.registerSubmitting,
                modifier = Modifier.fillMaxWidth(),
                testTag = "register_submit_button",
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable {
                        CustomTabLauncher.launchWebPath(context, "/professionals/join")
                    }
                    .padding(vertical = 6.dp)
                    .testTag("register_pro_apply_link"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.register_pro_apply_prompt),
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.Underline,
                    ),
                    color = ForestGreen,
                    textAlign = TextAlign.Center,
                )
            }

            HorizontalDivider(color = SoftSand)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.already_have_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                )
                TextButton(
                    onClick = onSignInClick,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("go_to_login_button"),
                ) {
                    Text(
                        text = stringResource(R.string.sign_in),
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepForest,
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordChecklist(rules: List<PasswordRuleStatus>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // s4's checklist: a Sand panel at radius 16, a filled disc per rule that turns Forest Green
            // the moment the rule is met. Colour is the only change of state, so nothing jumps.
            .background(SoftSand.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rules.forEach { rule ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = if (rule.met) "✓" else "○",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (rule.met) ForestGreen else MutedGray,
                )
                Text(
                    text = rule.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (rule.met) DeepForest else MutedGray,
                )
            }
        }
    }
}

@Composable
fun VerifyEmailScreen(
    userEmail: String,
    isAlreadyVerified: Boolean,
    uiState: AuthUiState,
    onLoadMethod: () -> Unit,
    onOtpCodeChange: (String) -> Unit,
    onSubmitOtp: () -> Unit,
    onResend: () -> Unit,
    onCheckVerifiedStatus: () -> Unit,
    onContinueToHome: () -> Unit,
    onSignOut: () -> Unit,
) {
    LaunchedEffect(Unit) {
        if (uiState.verificationMethod == null && !isAlreadyVerified && !uiState.emailConfirmed) {
            onLoadMethod()
        }
    }

    AuthShellContainer(onBackClick = null) {
        if (isAlreadyVerified || uiState.emailConfirmed) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = stringResource(R.string.verify_confirmed_title),
                    tint = ForestGreen,
                    modifier = Modifier.size(42.dp),
                )
                Text(
                    text = stringResource(R.string.verify_confirmed_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = DeepForest,
                )
                Text(
                    text = stringResource(R.string.verify_confirmed_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal,
                )
                ServixPrimaryButton(
                    text = stringResource(R.string.verify_continue_home),
                    onClick = onContinueToHome,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "verify_continue_button",
                )
            }
            return@AuthShellContainer
        }

        when {
            uiState.verificationMethodError -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Unable to connect",
                        style = MaterialTheme.typography.headlineMedium,
                        color = DeepForest,
                    )
                    Text(
                        text = "Your progress is safe. Check your connection, then try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGray,
                    )
                    ServixPrimaryButton(
                        text = stringResource(R.string.retry),
                        onClick = onLoadMethod,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "verify_retry_method_button",
                    )
                }
            }

            uiState.verificationMethod == null -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Preparing verification…",
                        style = MaterialTheme.typography.headlineSmall,
                        color = DeepForest,
                    )
                    Text(
                        text = "Checking how to verify your email securely.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGray,
                    )
                }
            }

            uiState.verificationMethod == "link" -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = ForestGreen.copy(alpha = 0.12f),
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.MailOutline,
                                contentDescription = stringResource(R.string.verify_link_title),
                                tint = ForestGreen,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.verify_link_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = DeepForest,
                    )
                    Text(
                        text = stringResource(R.string.verify_link_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGray,
                    )
                    if (!uiState.otpNotice.isNullOrBlank()) {
                        Text(
                            text = uiState.otpNotice,
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestGreen,
                        )
                    }
                    if (!uiState.otpError.isNullOrBlank()) {
                        Text(
                            text = uiState.otpError,
                            style = MaterialTheme.typography.bodySmall,
                            color = DangerRed,
                        )
                    }
                    ServixPrimaryButton(
                        text = stringResource(R.string.verify_check_status),
                        onClick = onCheckVerifiedStatus,
                        loading = uiState.otpBusy,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "verify_check_status_button",
                    )
                    ServixSecondaryButton(
                        text = if (uiState.resendWaitSeconds > 0) {
                            stringResource(R.string.verify_resend_cooldown, uiState.resendWaitSeconds)
                        } else {
                            stringResource(R.string.verify_resend_email)
                        },
                        onClick = onResend,
                        enabled = !uiState.otpBusy && uiState.resendWaitSeconds == 0,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "verify_resend_button",
                    )
                    TextButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("verify_link_sign_out_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.sign_out),
                            style = MaterialTheme.typography.labelLarge,
                            color = DangerRed,
                        )
                    }
                }
            }

            else -> {
                // 6-digit OTP verification screen matching EmailCodeVerification.jsx
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(R.string.verify_otp_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = DeepForest,
                    )
                    Text(
                        text = stringResource(R.string.verify_otp_lead, userEmail),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal,
                    )

                    SixDigitCodeField(
                        label = stringResource(R.string.verify_code_label),
                        code = uiState.otpCode,
                        onCodeChange = onOtpCodeChange,
                        error = uiState.otpError,
                        hint = stringResource(R.string.verify_code_hint),
                        onDone = onSubmitOtp,
                        testTag = "otp_code_input",
                    )

                    ServixPrimaryButton(
                        text = stringResource(if (uiState.otpBusy) R.string.verify_please_wait else R.string.verify_submit),
                        onClick = onSubmitOtp,
                        loading = uiState.otpBusy,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "otp_submit_button",
                    )

                    if (!uiState.otpNotice.isNullOrBlank()) {
                        Text(
                            text = uiState.otpNotice,
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestGreen,
                        )
                    }

                    ServixSecondaryButton(
                        text = if (uiState.resendWaitSeconds > 0) {
                            stringResource(R.string.verify_resend_cooldown, uiState.resendWaitSeconds)
                        } else {
                            stringResource(R.string.verify_send_new_code)
                        },
                        onClick = onResend,
                        enabled = !uiState.otpBusy && uiState.resendWaitSeconds == 0,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "otp_resend_button",
                    )

                    Text(
                        text = stringResource(R.string.verify_security_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("otp_sign_out_button"),
                        ) {
                            Text(
                                text = stringResource(R.string.sign_out),
                                style = MaterialTheme.typography.labelLarge,
                                color = DangerRed,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SixDigitCodeField(
    label: String,
    code: String,
    onCodeChange: (String) -> Unit,
    error: String?,
    hint: String,
    onDone: () -> Unit,
    testTag: String,
    isNumeric: Boolean = true,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label *",
            style = MaterialTheme.typography.labelLarge,
            color = Charcoal,
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            singleLine = true,
            isError = !error.isNullOrBlank(),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isNumeric) KeyboardType.NumberPassword else KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            textStyle = if (isNumeric) {
                MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center,
                    letterSpacing = 8.sp,
                )
            } else {
                MaterialTheme.typography.bodyLarge
            },
            shape = RoundedCornerShape(6.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
                focusedBorderColor = DeepForest,
                unfocusedBorderColor = BorderSand,
                errorBorderColor = DangerRed,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = DangerRed,
            )
        } else {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
        }
    }
}

/**
 * Mirrors reference/web/src/pages/auth/SecurityCheckPage.jsx:16-191 and
 * checks GET /auth/security/config -> { enabled } first.
 */
/** 0 Email, 1 Authenticator, 2 Recovery codes, 3 Done — derived from the security flow state. */
internal fun securityStepOf(uiState: AuthUiState): Int = when {
    uiState.securitySuccessMode != null -> 3
    uiState.securityRecoveryCodes.isNotEmpty() -> 2
    uiState.securityTotpSetup != null || uiState.securityUseRecovery -> 1
    else -> 0
}

/** Four-dot progress row with the step name (plan §5 security check). */
@Composable
fun SecurityStepProgress(current: Int) {
    val steps = listOf("Email", "Authenticator", "Recovery codes", "Done")
    val index = current.coerceIn(0, steps.lastIndex)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .testTag("security_step_progress"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Step ${index + 1} of ${steps.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MutedGray,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = steps[index],
                style = MaterialTheme.typography.labelLarge,
                color = DeepForest,
                modifier = Modifier.testTag("security_step_name"),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            steps.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= index) DeepForest else SoftSand),
                )
            }
        }
    }
}

@Composable
fun SecurityCheckScreen(
    uiState: AuthUiState,
    onLoadSecurityCheck: () -> Unit,
    onCodeChange: (String) -> Unit,
    onSubmitCode: () -> Unit,
    onResendEmailCode: () -> Unit,
    onBeginTotpSetup: () -> Unit,
    onClearTotpSetup: () -> Unit,
    onToggleUseRecovery: () -> Unit,
    onSavedRecoveryChange: (Boolean) -> Unit,
    onFinishSecurity: () -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmitResetPassword: () -> Unit,
    onCancelAndReturnLogin: () -> Unit,
) {
    val context = LocalContext.current
    var showResetPassword by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onLoadSecurityCheck()
    }

    AuthShellContainer(onBackClick = onCancelAndReturnLogin) {
        SecurityStepProgress(current = securityStepOf(uiState))
        if (uiState.securitySuccessMode == "reset") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Password updated",
                    tint = ForestGreen,
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    text = "Password updated",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DeepForest,
                )
                Text(
                    text = "Your other sessions have been signed out. Use your new password and your existing security method to sign in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal,
                )
                ServixPrimaryButton(
                    text = stringResource(R.string.back_to_sign_in),
                    onClick = onCancelAndReturnLogin,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "security_reset_back_login_button",
                )
            }
            return@AuthShellContainer
        }

        if (uiState.securityConfigEnabled == false) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.security_disabled_on_web),
                    style = MaterialTheme.typography.headlineSmall,
                    color = DeepForest,
                    modifier = Modifier.testTag("security_disabled_message"),
                )
                ServixPrimaryButton(
                    text = stringResource(R.string.open_servix_web),
                    onClick = { CustomTabLauncher.launchWebPath(context, "/login") },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "security_open_web_button",
                )
                ServixSecondaryButton(
                    text = stringResource(R.string.back_to_sign_in),
                    onClick = onCancelAndReturnLogin,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            return@AuthShellContainer
        }

        val flow = uiState.securityFlow
        val stage = flow?.next.orEmpty()
        val title = when {
            uiState.securityLoading -> stringResource(R.string.security_preparing)
            uiState.securityExpired -> stringResource(R.string.security_expired_title)
            stage == "email" -> stringResource(R.string.security_title_email)
            stage == "enroll" -> stringResource(R.string.security_title_enroll)
            stage == "factor" -> stringResource(R.string.security_title_factor)
            stage == "recovery" -> stringResource(R.string.security_title_recovery)
            stage == "reset_password" -> stringResource(R.string.security_title_reset)
            else -> stringResource(R.string.security_title_complete)
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = DeepForest,
                modifier = Modifier.testTag("security_title_text"),
            )

            if (!uiState.securityError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DangerRed.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = uiState.securityError,
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerRed,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (!uiState.securityNotice.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ForestGreen.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = uiState.securityNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepForest,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (uiState.securityLoading) {
                Text(
                    text = stringResource(R.string.security_checking_progress),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                )
                return@Column
            }

            if (uiState.securityExpired || uiState.securityLoadFailed || flow == null) {
                ServixPrimaryButton(
                    text = stringResource(R.string.security_start_again),
                    onClick = onCancelAndReturnLogin,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "security_start_again_button",
                )
                if (!uiState.securityExpired) {
                    ServixSecondaryButton(
                        text = stringResource(R.string.security_try_loading_again),
                        onClick = onLoadSecurityCheck,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "security_retry_load_button",
                    )
                }
                return@Column
            }

            when (stage) {
                "email" -> {
                    val prompt = if (flow.purpose == "reset") {
                        "If the account is eligible, a code has been queued for delivery to your email. Use the newest code within 10 minutes."
                    } else {
                        "A code has been queued for ${flow.email.orEmpty()}. Use the newest code within 10 minutes."
                    }
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal,
                    )

                    SixDigitCodeField(
                        label = stringResource(R.string.verify_code_label),
                        code = uiState.securityCode,
                        onCodeChange = onCodeChange,
                        error = null,
                        hint = "Paste or enter the six-digit code.",
                        onDone = onSubmitCode,
                        testTag = "security_email_code_input",
                    )

                    ServixPrimaryButton(
                        text = stringResource(R.string.verify_submit),
                        onClick = onSubmitCode,
                        loading = uiState.securityBusy,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "security_email_verify_button",
                    )

                    ServixSecondaryButton(
                        text = if (uiState.securityWaitSeconds > 0) {
                            "Resend in ${uiState.securityWaitSeconds}s"
                        } else {
                            stringResource(R.string.verify_send_new_code)
                        },
                        onClick = onResendEmailCode,
                        enabled = !uiState.securityBusy && uiState.securityWaitSeconds == 0,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "security_email_resend_button",
                    )
                }

                "enroll" -> {
                    Text(
                        text = "Choose one method. We’ll remember it and ask for it when you sign in. First, let’s prove it works.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal,
                    )
                    val setup = uiState.securityTotpSetup
                    if (setup == null) {
                        ServixCard(
                            modifier = Modifier
                                .clickable(enabled = !uiState.securityBusy) { onBeginTotpSetup() }
                                .testTag("security_choose_totp_button"),
                            containerColor = SoftSand.copy(alpha = 0.45f),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Google Authenticator",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DeepForest,
                                )
                                Text(
                                    text = "Set up once with a setup key or authenticator app. Then use the rotating six-digit code in your app—even offline.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedGray,
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Open Google Authenticator → tap + → Enter a setup key. Keep this setup key private.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Charcoal,
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SoftSand,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = setup.secret,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = DeepForest,
                                    modifier = Modifier.testTag("security_totp_secret_text"),
                                )
                                if (setup.uri.isNotBlank()) {
                                    Text(
                                        text = setup.uri,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                        ),
                                        color = MutedGray,
                                        modifier = Modifier.testTag("security_totp_uri_text"),
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ServixSecondaryButton(
                                        text = "Copy key",
                                        onClick = {
                                            copyToClipboard(context, "Servix setup key", setup.secret)
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (setup.uri.startsWith("otpauth://")) {
                                        ServixSecondaryButton(
                                            text = "Open app",
                                            onClick = {
                                                runCatching {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(setup.uri)))
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }

                        SixDigitCodeField(
                            label = "Google Authenticator code",
                            code = uiState.securityCode,
                            onCodeChange = onCodeChange,
                            error = null,
                            hint = "Paste or enter the six-digit code.",
                            onDone = onSubmitCode,
                            testTag = "security_totp_enroll_input",
                        )

                        ServixPrimaryButton(
                            text = "Confirm authenticator",
                            onClick = onSubmitCode,
                            loading = uiState.securityBusy,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "security_totp_enroll_submit",
                        )

                        TextButton(
                            onClick = onClearTotpSetup,
                            enabled = !uiState.securityBusy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Choose a different method",
                                style = MaterialTheme.typography.labelLarge,
                                color = MutedGray,
                            )
                        }
                    }
                }

                "factor" -> {
                    val prompt = when {
                        uiState.securityUseRecovery -> "Enter one of the recovery codes you saved during setup."
                        flow.method == "passkey" -> "Use your authenticator app or a recovery code"
                        else -> "Open Google Authenticator and enter the current code for Servix."
                    }
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal,
                    )

                    SixDigitCodeField(
                        label = if (uiState.securityUseRecovery) "Recovery code" else "Google Authenticator code",
                        code = uiState.securityCode,
                        onCodeChange = onCodeChange,
                        error = null,
                        hint = if (uiState.securityUseRecovery) "Each recovery code works only once." else "Paste or enter the six-digit code.",
                        onDone = onSubmitCode,
                        testTag = "security_factor_code_input",
                        isNumeric = !uiState.securityUseRecovery,
                    )
                    ServixPrimaryButton(
                        text = "Verify security code",
                        onClick = onSubmitCode,
                        loading = uiState.securityBusy,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "security_factor_submit_button",
                    )

                    TextButton(
                        onClick = onToggleUseRecovery,
                        enabled = !uiState.securityBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (uiState.securityUseRecovery) {
                                "Use my enrolled security method"
                            } else {
                                "Lost access? Use a recovery code"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = ForestGreen,
                        )
                    }
                }

                "recovery" -> {
                    Text(
                        text = "Your security method is confirmed. Save these recovery codes in a password manager or a safe offline place before continuing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal,
                    )
                    Text(
                        text = "Each code works once. Never send these codes to support or include them in screenshots.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SoftSand,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            uiState.securityRecoveryCodes.forEach { recoveryCode ->
                                Text(
                                    text = recoveryCode,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = DeepForest,
                                )
                            }
                        }
                    }

                    ServixSecondaryButton(
                        text = "Copy recovery codes",
                        onClick = {
                            copyToClipboard(
                                context,
                                "Servix recovery codes",
                                uiState.securityRecoveryCodes.joinToString("\n"),
                            )
                        },
                        enabled = uiState.securityRecoveryCodes.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSavedRecoveryChange(!uiState.securitySavedRecovery) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = uiState.securitySavedRecovery,
                            onCheckedChange = onSavedRecoveryChange,
                            colors = CheckboxDefaults.colors(checkedColor = DeepForest),
                            modifier = Modifier.testTag("security_saved_recovery_checkbox"),
                        )
                        Text(
                            text = "I have saved my recovery codes somewhere private.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Charcoal,
                        )
                    }

                    ServixPrimaryButton(
                        text = "Finish securely",
                        onClick = onFinishSecurity,
                        enabled = !uiState.securityBusy && uiState.securitySavedRecovery && uiState.securityRecoveryCodes.isNotEmpty(),
                        loading = uiState.securityBusy,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "security_finish_recovery_button",
                    )
                }

                "finish" -> {
                    Text(
                        text = "Your email and enrolled security method have been verified.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal,
                    )
                    ServixPrimaryButton(
                        text = "Finish signing in",
                        onClick = onFinishSecurity,
                        loading = uiState.securityBusy,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "security_finish_button",
                    )
                }

                "reset_password" -> {
                    Text(
                        text = "Your email and existing security method are verified. Choose a unique password. Your enrolled security method will stay unchanged.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal,
                    )
                    ServixTextField(
                        value = uiState.securityNewPassword,
                        onValueChange = onNewPasswordChange,
                        label = "New password",
                        required = true,
                        isPassword = true,
                        passwordVisible = showResetPassword,
                        onTogglePasswordVisibility = { showResetPassword = !showResetPassword },
                        testTag = "security_new_password_input",
                    )
                    PasswordChecklist(rules = uiState.securityResetPasswordRules)
                    val confirmHint = when {
                        uiState.securityConfirmPassword.isEmpty() -> "Enter the new password again."
                        uiState.securityNewPassword == uiState.securityConfirmPassword -> "✓ Passwords match"
                        else -> "Passwords do not match yet"
                    }
                    ServixTextField(
                        value = uiState.securityConfirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = "Confirm password",
                        required = true,
                        hint = confirmHint,
                        isPassword = true,
                        passwordVisible = showResetPassword,
                        onTogglePasswordVisibility = { showResetPassword = !showResetPassword },
                        testTag = "security_confirm_password_input",
                    )
                    ServixPrimaryButton(
                        text = "Save new password",
                        onClick = onSubmitResetPassword,
                        loading = uiState.securityBusy,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "security_save_password_button",
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}

@Composable
fun ForgotPasswordScreen(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackToSignIn: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    AuthShellContainer(onBackClick = onBackToSignIn) {
        if (uiState.forgotSent) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.forgot_sent_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = DeepForest,
                )
                Text(
                    text = stringResource(R.string.forgot_sent_body, uiState.forgotEmail.trim()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal,
                )
                ServixSecondaryButton(
                    text = stringResource(R.string.back_to_sign_in),
                    onClick = onBackToSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "forgot_back_to_login_button",
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.forgot_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = DeepForest,
                )
                Text(
                    text = stringResource(R.string.forgot_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                )
                ServixTextField(
                    value = uiState.forgotEmail,
                    onValueChange = onEmailChange,
                    label = stringResource(R.string.field_email),
                    required = true,
                    error = uiState.forgotError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onSubmit()
                        }
                    ),
                    testTag = "forgot_email_input",
                )
                ServixPrimaryButton(
                    text = stringResource(if (uiState.forgotSubmitting) R.string.forgot_sending else R.string.forgot_submit),
                    onClick = {
                        focusManager.clearFocus()
                        onSubmit()
                    },
                    loading = uiState.forgotSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "forgot_submit_button",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.remembered_it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGray,
                    )
                    TextButton(
                        onClick = onBackToSignIn,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("forgot_return_login_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.back_to_sign_in),
                            style = MaterialTheme.typography.labelLarge,
                            color = DeepForest,
                        )
                    }
                }
            }
        }
    }
}
