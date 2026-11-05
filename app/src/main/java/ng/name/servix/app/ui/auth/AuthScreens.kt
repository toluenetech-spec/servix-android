package ng.name.servix.app.ui.auth

import androidx.compose.foundation.BorderStroke
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
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.CustomTabLauncher

@Composable
fun WelcomeScreen(
    onSignInClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onBrowseFirstClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmIvory)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
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

            Spacer(modifier = Modifier.height(12.dp))

            ServixBadge(
                text = stringResource(R.string.hero_eyebrow).uppercase(),
                containerColor = SoftSand,
                contentColor = DeepForest,
            )

            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.displaySmall,
                color = DeepForest,
                modifier = Modifier.testTag("welcome_hero_title"),
            )

            Text(
                text = stringResource(R.string.hero_lead),
                style = MaterialTheme.typography.bodyLarge,
                color = MutedGray,
            )

            ServixCard(
                containerColor = PureWhite,
                contentPadding = PaddingValues(18.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SoftSand,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = "Escrow protection",
                                tint = DeepForest,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Servix payment protection",
                            style = MaterialTheme.typography.titleSmall,
                            color = Charcoal,
                        )
                        Text(
                            text = stringResource(R.string.escrow_copy),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ServixPrimaryButton(
                text = stringResource(R.string.sign_in),
                onClick = onSignInClick,
                modifier = Modifier.fillMaxWidth(),
                testTag = "welcome_sign_in_button",
            )
            ServixSecondaryButton(
                text = stringResource(R.string.create_account),
                onClick = onCreateAccountClick,
                modifier = Modifier.fillMaxWidth(),
                testTag = "welcome_create_account_button",
            )
            if (onBrowseFirstClick != null) {
                TextButton(
                    onClick = onBrowseFirstClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("welcome_browse_button"),
                ) {
                    Text(
                        text = stringResource(R.string.continue_browsing),
                        style = MaterialTheme.typography.labelLarge,
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
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
    onSubmit: () -> Unit,
    onSignInClick: () -> Unit,
    onBackClick: () -> Unit,
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

            // Live Password Requirements checklist matching reference/web/src/pages/auth/PasswordRequirements.jsx
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftSand.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                uiState.passwordRules.forEach { rule ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (rule.met) "✓" else "○",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (rule.met) DeepForest else MutedGray,
                        )
                        Text(
                            text = rule.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (rule.met) DeepForest else MutedGray,
                        )
                    }
                }
            }

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
                        onSubmit()
                    }
                ),
                testTag = "register_confirm_password_input",
            )

            Text(
                text = stringResource(R.string.register_terms_note),
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )

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
                loading = uiState.registerSubmitting,
                modifier = Modifier.fillMaxWidth(),
                testTag = "register_submit_button",
            )

            // Confirmed user choice: customer-only registration with small link to apply on www.servix.name.ng
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
                        onClick = onContinueToHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.verify_continue_home),
                            style = MaterialTheme.typography.labelLarge,
                            color = DeepForest,
                        )
                    }
                }
            }

            else -> {
                // 6-digit OTP verification screen matching EmailCodeVerification.jsx
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(R.string.verify_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = ForestGreen,
                    )
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

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${stringResource(R.string.verify_code_label)} *",
                            style = MaterialTheme.typography.labelLarge,
                            color = Charcoal,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = uiState.otpCode,
                            onValueChange = onOtpCodeChange,
                            singleLine = true,
                            isError = !uiState.otpError.isNullOrBlank(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { onSubmitOtp() }),
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                textAlign = TextAlign.Center,
                                letterSpacing = 8.sp,
                            ),
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
                                .testTag("otp_code_input"),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (!uiState.otpError.isNullOrBlank()) {
                            Text(
                                text = uiState.otpError,
                                style = MaterialTheme.typography.bodySmall,
                                color = DangerRed,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.verify_code_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }
                    }

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
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(
                            onClick = onContinueToHome,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("otp_skip_for_now_button"),
                        ) {
                            Text(
                                text = "Verify later",
                                style = MaterialTheme.typography.labelLarge,
                                color = MutedGray,
                            )
                        }
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
