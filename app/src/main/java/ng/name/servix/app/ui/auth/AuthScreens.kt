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
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import ng.name.servix.app.ui.theme.ServixButtonRadius
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockClock
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import ng.name.servix.app.ui.components.ServixButtonStyle
import ng.name.servix.app.ui.components.ServixButton
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import ng.name.servix.app.ui.theme.ErrorContainer

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Path
import ng.name.servix.app.ui.theme.ServixMotion
import ng.name.servix.app.ui.theme.reducedMotion
import androidx.compose.ui.unit.sp
import ng.name.servix.app.R
import ng.name.servix.app.ui.components.ServixBadge
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixLogoMark
import ng.name.servix.app.ui.components.ServixField
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.components.ServixTextField
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DangerRed
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.OnErrorContainer
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.SurfaceContainerHigh
import ng.name.servix.app.ui.theme.SurfaceContainerLow
import ng.name.servix.app.ui.theme.ServixType
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

/** Provider vectors reproduce the two marks in the corrected registration export. */
@Composable
private fun ProviderMark(google: Boolean) {
    Image(
        painter = painterResource(if (google) R.drawable.provider_google else R.drawable.provider_github),
        contentDescription = null,
        modifier = Modifier.size(16.dp),
    )
}

/** The API reports provider ids ("google", "github"); the button shows the product's own name. */
fun providerDisplayName(id: String): String = when (id.lowercase()) {
    "google" -> "Google"
    "github" -> "GitHub"
    "apple" -> "Apple"
    else -> id.replaceFirstChar { it.uppercase() }
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
    // Rebuilt to corrected/s2_welcome_minimal.html: a grabber, one centred hero with a soft glow behind
    // the wordmark, and two 54 dp buttons pinned to the bottom. No card, no scroll, no illustration.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmIvory)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 4.dp)
                .background(SoftSand, CircleShape),
        )

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // The HTML's hero: a soft ring behind a Forest-Green shield with a check, pulsing gently on a
            // two-second cycle, then the SERVIX wordmark beneath. With animations off it is a still mark.
            val pulse = if (reducedMotion()) 1f else {
                val transition = rememberInfiniteTransition(label = "shieldPulse")
                transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(tween(2000, easing = ServixMotion.ContentEasing), RepeatMode.Reverse),
                    label = "shieldScale",
                ).value
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .scale(pulse)
                            .background(SoftSand.copy(alpha = 0.55f), CircleShape),
                    )
                    Canvas(modifier = Modifier.size(64.dp).testTag("welcome_shield")) {
                        val w = size.width
                        val h = size.height
                        // Shield outline with a check, drawn rather than approximated by an icon.
                        val shield = Path().apply {
                            moveTo(w * 0.5f, h * 0.06f)
                            lineTo(w * 0.92f, h * 0.24f)
                            lineTo(w * 0.92f, h * 0.52f)
                            cubicTo(w * 0.92f, h * 0.78f, w * 0.72f, h * 0.92f, w * 0.5f, h * 0.97f)
                            cubicTo(w * 0.28f, h * 0.92f, w * 0.08f, h * 0.78f, w * 0.08f, h * 0.52f)
                            lineTo(w * 0.08f, h * 0.24f)
                            close()
                        }
                        drawPath(shield, color = ForestGreen)
                        val check = Path().apply {
                            moveTo(w * 0.30f, h * 0.50f)
                            lineTo(w * 0.45f, h * 0.65f)
                            lineTo(w * 0.72f, h * 0.34f)
                        }
                        drawPath(
                            check,
                            color = WarmIvory,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = w * 0.09f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round,
                            ),
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 20.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = "SERVIX",
                        style = ServixType.headlineLg.copy(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 30.sp),
                        color = DeepForest,
                        modifier = Modifier.testTag("welcome_wordmark"),
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 2.dp, bottom = 5.dp)
                            .size(7.dp)
                            .background(ForestGreen, CircleShape),
                    )
                }
            }

            Text(
                text = stringResource(R.string.welcome_headline),
                style = ServixType.headlineLgMobile,
                color = DeepForest,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp).testTag("welcome_hero_title"),
            )
            Text(
                text = stringResource(R.string.welcome_subtext),
                style = ServixType.bodyMd,
                color = MutedGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp).testTag("welcome_subtext"),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ServixButton(
                text = stringResource(R.string.welcome_create_account),
                onClick = onCreateAccountClick,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                testTag = "welcome_create_account_button",
            )
            ServixButton(
                text = stringResource(R.string.welcome_sign_in),
                onClick = onSignInClick,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                style = ServixButtonStyle.Secondary,
                testTag = "welcome_sign_in_button",
            )
            if (providers.isNotEmpty()) {
                OrDivider()
                providers.forEach { provider ->
                    ServixButton(
                        text = "Continue with ${providerDisplayName(provider)}",
                        onClick = { onProviderClick(provider) },
                        style = ServixButtonStyle.Text,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "welcome_oauth_$provider",
                    )
                }
            }
            onBrowseFirstClick?.let { browse ->
                ServixButton(
                    text = stringResource(R.string.welcome_browse_first),
                    onClick = browse,
                    style = ServixButtonStyle.Text,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "welcome_browse_first_button",
                )
            }
        }
    }
}

/**
 * Shared frame for the auth screens that still use it (register, email code, the security stages,
 * forgot password). S2 and S3 own their own headers now, per their HTML.
 */
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
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp).testTag("auth_back_button"),
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
        }
        Column(modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp)) {
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

    // Rebuilt to corrected/s3_sign_in.html, top to bottom: header, eyebrow card, headline block,
    // email, password + forgot, sign-in, "or continue with", providers, footer, trust pair.
    // Behaviour is unchanged: the same state fields, the same callbacks, the same test tags.
    Column(modifier = Modifier.fillMaxSize().background(WarmIvory)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(48.dp).testTag("auth_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Charcoal,
                )
            }
            Text(
                text = stringResource(R.string.sign_in),
                style = ServixType.titleLg,
                color = Charcoal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier.size(32.dp).background(DeepForest, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = WarmIvory,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.size(12.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Eyebrow card: shield tile, SERVIX wordmark, one honest promise.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceContainerLow, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).background(ForestGreen, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = WarmIvory,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("SERVIX", style = ServixType.labelMd.copy(letterSpacing = 1.2.sp), color = ForestGreen)
                    Text(
                        text = stringResource(R.string.login_tagline),
                        style = ServixType.bodySm,
                        color = MutedGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.login_title),
                    style = ServixType.headlineLgMobile,
                    color = Charcoal,
                )
                Text(
                    text = stringResource(R.string.login_subtitle),
                    style = ServixType.bodyMd,
                    color = MutedGray,
                )
            }

            ServixField(
                value = uiState.loginEmail,
                onValueChange = onEmailChange,
                label = stringResource(R.string.field_email),
                placeholder = "olumide@example.ng",
                leadingIcon = Icons.Outlined.MailOutline,
                error = uiState.loginEmailError,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                testTag = "login_email_input",
            )

            ServixField(
                value = uiState.loginPassword,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.field_password),
                leadingIcon = Icons.Outlined.Lock,
                error = uiState.loginPasswordError,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = {
                    focusManager.clearFocus()
                    onSubmit()
                },
                trailing = {
                    IconButton(
                        onClick = { showPassword = !showPassword },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = stringResource(
                                if (showPassword) R.string.hide_password else R.string.show_password,
                            ),
                            tint = MutedGray,
                        )
                    }
                },
                testTag = "login_password_input",
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = stringResource(R.string.forgot_password_link),
                    style = ServixType.labelMd,
                    color = ForestGreen,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(onClick = onForgotPasswordClick)
                        .padding(top = 14.dp)
                        .testTag("forgot_password_button"),
                )
            }

            if (!uiState.loginGeneralError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ErrorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = uiState.loginGeneralError,
                        style = ServixType.bodySm,
                        color = OnErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            ServixButton(
                text = stringResource(if (uiState.loginSubmitting) R.string.signing_in else R.string.sign_in),
                onClick = {
                    focusManager.clearFocus()
                    onSubmit()
                },
                loading = uiState.loginSubmitting,
                trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                modifier = Modifier.fillMaxWidth(),
                testTag = "login_submit_button",
            )

            // Providers keep today's behaviour: the same onProviderClick with the API's own names.
            if (providers.isNotEmpty()) {
                OrDivider()
                providers.forEach { provider ->
                    ServixButton(
                        text = "Continue with ${providerDisplayName(provider)}",
                        onClick = { onProviderClick(provider) },
                        style = ServixButtonStyle.Secondary,
                        enabled = !uiState.oauthBusy,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "login_oauth_$provider",
                    )
                }
            }

            HorizontalDivider(color = BorderSand)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.new_to_servix),
                    style = ServixType.bodyMd,
                    color = MutedGray,
                )
                TextButton(
                    onClick = onCreateAccountClick,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("go_to_register_button"),
                ) {
                    Text(
                        text = stringResource(R.string.create_account),
                        style = ServixType.labelLg,
                        color = ForestGreen,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
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
                            style = ServixType.labelMd,
                            color = Charcoal,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.payment_protection_copy),
                    style = ServixType.bodySm,
                    color = MutedGray,
                    textAlign = TextAlign.Center,
                )
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
    var showConfirmation by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(WarmIvory)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(44.dp).testTag("auth_back_button")) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = Charcoal)
            }
            Text("Register", style = ServixType.titleLg, color = Charcoal, modifier = Modifier.weight(1f))
            Box(Modifier.size(32.dp).background(DeepForest, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.padding(top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    Modifier.padding(bottom = 4.dp).background(SoftSand, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(10.dp).background(ForestGreen, CircleShape))
                    Text("Servix", style = ServixType.titleMd.copy(fontWeight = FontWeight.Bold), color = DeepForest)
                }
                Text("Create your account", style = ServixType.headlineMd, color = Charcoal)
                Text("Join Servix and get started with trusted professionals.", style = ServixType.bodyMd, color = MutedGray)
            }
            RegisterField(uiState.registerFullName, onNameChange, "Full name", "e.g. Olumide Adeleke", Icons.Outlined.Person,
                uiState.registerNameError, "register_name_input", KeyboardType.Text)
            RegisterField(uiState.registerEmail, onEmailChange, "Email address", "olumide@example.ng", Icons.Outlined.MailOutline,
                uiState.registerEmailError, "register_email_input", KeyboardType.Email)
            RegisterField(uiState.registerPassword, { onPasswordChange(it.take(200)) }, "Password", "Create a strong password", Icons.Outlined.Lock,
                uiState.registerPasswordError, "register_password_input", KeyboardType.Password, showPassword, { showPassword = !showPassword })
            RegisterField(uiState.registerConfirmPassword, { onConfirmPasswordChange(it.take(200)) }, "Confirm password", "Re-enter your password", Icons.Outlined.LockClock,
                uiState.registerConfirmError, "register_confirm_password_input", KeyboardType.Password, showConfirmation, { showConfirmation = !showConfirmation },
                onDone = { if (uiState.canSubmitRegistration) { focusManager.clearFocus(); onSubmit() } })
            RegisterPasswordChecklist(uiState.passwordRules)
            Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(
                        checked = uiState.registerTermsAccepted, onCheckedChange = onTermsAcceptedChange,
                        colors = CheckboxDefaults.colors(checkedColor = ForestGreen, uncheckedColor = MutedGray, checkmarkColor = PureWhite),
                        modifier = Modifier.size(48.dp).testTag("register_terms_checkbox"),
                    )
                    val terms = androidx.compose.ui.text.buildAnnotatedString {
                        append("I agree to the Servix ")
                        pushStringAnnotation("url", "https://www.servix.name.ng/terms")
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = ForestGreen, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline))
                        append("Terms of Service"); pop(); pop()
                        append(" and ")
                        pushStringAnnotation("url", "https://www.servix.name.ng/privacy")
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = ForestGreen, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline))
                        append("Privacy Policy"); pop(); pop(); append(".")
                    }
                    androidx.compose.foundation.text.ClickableText(
                        text = terms, style = ServixType.bodySm.copy(color = Charcoal), modifier = Modifier.padding(top = 8.dp),
                        onClick = { offset -> terms.getStringAnnotations("url", offset, offset).firstOrNull()?.let { CustomTabLauncher.launchUrl(context, it.item) } },
                    )
                }
                Text("You must be at least 18 years of age to register and book services on Servix.",
                    style = ServixType.bodySm, color = ng.name.servix.app.ui.theme.BorderStrongSand, modifier = Modifier.padding(start = 48.dp))
            }
            uiState.registerGeneralError?.let {
                Text(it, style = ServixType.bodySm, color = DangerRed, modifier = Modifier.fillMaxWidth().background(ErrorContainer, RoundedCornerShape(16.dp)).padding(12.dp))
            }
            ServixButton(
                text = stringResource(if (uiState.registerSubmitting) R.string.creating_account else R.string.create_account),
                onClick = { focusManager.clearFocus(); onSubmit() }, enabled = uiState.canSubmitRegistration,
                loading = uiState.registerSubmitting, trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), testTag = "register_submit_button",
            )
            if (providers.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(Modifier.weight(1f), color = SoftSand)
                    Text("or continue with", style = ServixType.labelMd, color = ng.name.servix.app.ui.theme.BorderStrongSand)
                    HorizontalDivider(Modifier.weight(1f), color = SoftSand)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    providers.forEach { provider ->
                        Button(
                            onClick = { onProviderClick(provider) }, enabled = uiState.registerTermsAccepted && !uiState.oauthBusy,
                            shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = SoftSand, contentColor = Charcoal),
                            contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("oauth_${provider.lowercase()}_button"),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ProviderMark(google = provider.equals("google", true))
                                Text(providerDisplayName(provider), style = ServixType.labelLg)
                            }
                        }
                    }
                }
                if (!uiState.registerTermsAccepted) Text("Tick the terms above to continue with a provider.", style = ServixType.bodySm, color = MutedGray)
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("Already have an account?", style = ServixType.bodyMd, color = MutedGray)
                TextButton(onClick = onSignInClick, modifier = Modifier.heightIn(min = 48.dp).testTag("go_to_login_button")) {
                    Text("Sign in", style = ServixType.labelLg.copy(textDecoration = TextDecoration.Underline), color = DeepForest)
                }
            }
        }
    }
}

@Composable
private fun RegisterField(
    value: String, onValueChange: (String) -> Unit, label: String, placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, error: String?, tag: String,
    keyboardType: KeyboardType, visible: Boolean = false, onToggle: (() -> Unit)? = null, onDone: () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = ServixType.labelLg, color = Charcoal)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).background(PureWhite, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = ng.name.servix.app.ui.theme.BorderStrongSand, modifier = Modifier.size(22.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, style = ServixType.bodyLg, color = ng.name.servix.app.ui.theme.BorderStrongSand, maxLines = 1)
                androidx.compose.foundation.text.BasicTextField(
                    value = value, onValueChange = onValueChange, singleLine = true,
                    textStyle = ServixType.bodyLg.copy(color = Charcoal),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = if (onToggle != null && tag.contains("confirm")) ImeAction.Done else ImeAction.Next),
                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                    visualTransformation = if (onToggle != null && !visible) PasswordVisualTransformation() else VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth().testTag(tag),
                )
            }
            if (onToggle != null) IconButton(onClick = onToggle, modifier = Modifier.size(48.dp)) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Hide $label" else "Show $label", tint = ng.name.servix.app.ui.theme.BorderStrongSand, modifier = Modifier.size(20.dp))
            }
        }
        error?.let { Text(it, style = ServixType.bodySm, color = DangerRed) }
    }
}

@Composable
private fun RegisterPasswordChecklist(rules: List<PasswordRuleStatus>) {
    val labels = listOf("At least 8 chars", "One uppercase", "One lowercase", "One number", "Special character")
    Column(Modifier.fillMaxWidth().background(SurfaceContainerLow, RoundedCornerShape(20.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Password requirements", style = ServixType.labelMd.copy(fontWeight = FontWeight.SemiBold), color = Charcoal)
            val count = rules.count { it.met }
            Text("$count of 5 met", style = ServixType.labelMd, color = if (count == 5) DeepForest else MutedGray,
                modifier = Modifier.background(if (count == 5) SoftSand else if (count > 0) BorderSand else SurfaceContainerHigh, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp).testTag("password_rules_count"))
        }
        rules.chunked(2).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { col, rule ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(if (rule.met) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = if (rule.met) "Met" else "Not met", tint = if (rule.met) ForestGreen else ng.name.servix.app.ui.theme.BorderStrongSand, modifier = Modifier.size(16.dp))
                        Text(labels[rowIndex * 2 + col], style = ServixType.bodySm, color = if (rule.met) Charcoal else MutedGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
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
    onRegisterAgain: () -> Unit = onSignOut,
) {
    LaunchedEffect(Unit) {
        if (uiState.verificationMethod == null && !isAlreadyVerified && !uiState.emailConfirmed) {
            onLoadMethod()
        }
    }

    if (!isAlreadyVerified && !uiState.emailConfirmed && !uiState.verificationMethodError &&
        uiState.verificationMethod != null && uiState.verificationMethod != "link") {
        VerifyOtpStitchScreen(userEmail, uiState, onOtpCodeChange, onSubmitOtp, onResend, onSignOut, onRegisterAgain)
        return
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

            else -> Unit // OTP mode is rendered by VerifyOtpStitchScreen above.

        }
    }
}

@Composable
private fun VerifyOtpStitchScreen(
    userEmail: String,
    uiState: AuthUiState,
    onOtpCodeChange: (String) -> Unit,
    onSubmitOtp: () -> Unit,
    onResend: () -> Unit,
    onSignOut: () -> Unit,
    onRegisterAgain: () -> Unit,
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(WarmIvory)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSignOut, modifier = Modifier.size(44.dp).testTag("auth_back_button")) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = Charcoal)
            }
            Text("Verification", style = ServixType.titleLg, color = Charcoal, modifier = Modifier.weight(1f))
            Box(Modifier.size(32.dp).background(DeepForest, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.padding(top = 16.dp, bottom = 24.dp).size(96.dp).background(SoftSand, CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(64.dp).background(SoftSand, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MailOutline, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(36.dp))
                }
                Box(Modifier.align(Alignment.TopEnd).size(28.dp).background(ForestGreen, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                }
            }
            Text("Verify your email address", style = ServixType.headlineLgMobile, color = Charcoal, textAlign = TextAlign.Center)
            Text("We sent a 6-digit confirmation code to", style = ServixType.bodyMd, color = MutedGray,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            Text(userEmail, style = ServixType.titleMd, color = Charcoal, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp, bottom = 32.dp))
            Column(
                Modifier.fillMaxWidth().background(SurfaceContainerLow, RoundedCornerShape(20.dp)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Keep the existing paste-capable code field, labels, errors and callbacks.
                SixDigitCodeField(
                    label = stringResource(R.string.verify_code_label), code = uiState.otpCode, onCodeChange = onOtpCodeChange,
                    error = uiState.otpError, hint = stringResource(R.string.verify_code_hint), onDone = onSubmitOtp, testTag = "otp_code_input",
                )
                if (!uiState.otpNotice.isNullOrBlank()) Text(uiState.otpNotice, style = ServixType.bodySm, color = ForestGreen)
                ServixSecondaryButton(
                    text = if (uiState.resendWaitSeconds > 0) stringResource(R.string.verify_resend_cooldown, uiState.resendWaitSeconds)
                        else stringResource(R.string.verify_send_new_code),
                    onClick = onResend, enabled = !uiState.otpBusy && uiState.resendWaitSeconds == 0,
                    modifier = Modifier.fillMaxWidth(), testTag = "otp_resend_button",
                )
                TextButton(onClick = onRegisterAgain, enabled = !uiState.otpBusy, modifier = Modifier.heightIn(min = 48.dp).testTag("verify_register_again_button")) {
                    Text("Wrong address? Register again", style = ServixType.labelLg, color = ForestGreen, textAlign = TextAlign.Center)
                }
                ServixPrimaryButton(
                    text = stringResource(if (uiState.otpBusy) R.string.verify_please_wait else R.string.verify_submit),
                    onClick = onSubmitOtp, loading = uiState.otpBusy, modifier = Modifier.fillMaxWidth(), testTag = "otp_submit_button",
                )
                ServixSecondaryButton(
                    text = "Open Mail App", onClick = {
                        val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try { context.startActivity(intent) } catch (_: android.content.ActivityNotFoundException) {
                            android.widget.Toast.makeText(context, "No email app found on this device.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.fillMaxWidth(), testTag = "verify_open_mail_button",
                )
            }
            Text(stringResource(R.string.verify_security_footer), style = ServixType.bodySm, color = MutedGray,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp))
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
/** Zero-based progress from the six server stages, matching new/s6a–s6f. */
internal fun securityStepOf(uiState: AuthUiState): Int = when {
    uiState.securitySuccessMode != null -> 3
    else -> when (uiState.securityFlow?.next) {
        "email" -> 0
        "enroll" -> 1
        "factor" -> 2
        "recovery", "reset_password", "finish" -> 3
        else -> 0
    }
}

@Composable
fun SecurityStepProgress(current: Int) {
    val index = current.coerceIn(0, 3)
    Row(
        modifier = Modifier.fillMaxWidth().testTag("security_step_progress")
            .semantics { contentDescription = "Step ${index + 1} of 4" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(4) { i ->
            Box(Modifier.weight(1f).height(6.dp).background(if (i <= index) DeepForest else BorderSand, CircleShape))
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
    LaunchedEffect(Unit) { onLoadSecurityCheck() }

    val flow = uiState.securityFlow
    val stage = flow?.next.orEmpty()
    val title = when {
        uiState.securitySuccessMode == "reset" -> "Password updated"
        uiState.securityConfigEnabled == false -> stringResource(R.string.security_disabled_on_web)
        uiState.securityLoading -> stringResource(R.string.security_preparing)
        uiState.securityExpired -> stringResource(R.string.security_expired_title)
        stage == "email" -> stringResource(R.string.security_title_email)
        stage == "enroll" -> stringResource(R.string.security_title_enroll)
        stage == "factor" -> stringResource(R.string.security_title_factor)
        stage == "recovery" -> stringResource(R.string.security_title_recovery)
        stage == "reset_password" -> stringResource(R.string.security_title_reset)
        else -> stringResource(R.string.security_title_complete)
    }
    val lead = when {
        uiState.securitySuccessMode == "reset" -> "Your other sessions have been signed out. Use your new password and your existing security method to sign in."
        uiState.securityLoading -> stringResource(R.string.security_checking_progress)
        flow == null || uiState.securityExpired || uiState.securityLoadFailed || uiState.securityConfigEnabled == false -> ""
        stage == "email" && flow.purpose == "reset" -> "If the account is eligible, a code has been queued for delivery to your email. Use the newest code within 10 minutes."
        stage == "email" -> "A code has been queued for ${flow.email.orEmpty()}. Use the newest code within 10 minutes."
        stage == "enroll" -> "Add Google Authenticator so only you can sign in."
        stage == "factor" && uiState.securityUseRecovery -> "Enter one of the recovery codes you saved during setup."
        stage == "factor" && flow.method == "passkey" -> "Use your authenticator app or a recovery code"
        stage == "factor" -> "Enter the code from your authenticator app."
        stage == "recovery" -> "Save these recovery codes somewhere private. Each code works once."
        stage == "reset_password" -> "Use something you don't use anywhere else."
        stage == "finish" -> "Thanks for keeping your account safe."
        else -> ""
    }
    val normalStage = !uiState.securityLoading && !uiState.securityExpired && !uiState.securityLoadFailed &&
        uiState.securityConfigEnabled != false && flow != null && uiState.securitySuccessMode != "reset"

    Column(Modifier.fillMaxSize().background(WarmIvory).padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancelAndReturnLogin, modifier = Modifier.size(48.dp).testTag("auth_back_button")) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = Charcoal)
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
            Row(
                Modifier.background(SoftSand.copy(alpha = 0.4f), CircleShape).padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(6.dp).background(ForestGreen, CircleShape))
                Text("Servix · Account security", style = ServixType.labelMd, color = DeepForest)
            }
            Text(title, style = ServixType.headlineLgMobile, color = Charcoal, modifier = Modifier.padding(top = 4.dp).testTag("security_title_text"))
            if (lead.isNotEmpty()) Text(lead, style = ServixType.bodyMd, color = MutedGray, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(16.dp))
            SecurityStepProgress(securityStepOf(uiState))
            uiState.securityError?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = ServixType.bodySm, color = DangerRed, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).background(ErrorContainer, RoundedCornerShape(16.dp)).padding(12.dp))
            }
            uiState.securityNotice?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = ServixType.bodySm, color = DeepForest, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).background(SoftSand, RoundedCornerShape(16.dp)).padding(12.dp))
            }
            if (normalStage) {
                Column(Modifier.fillMaxWidth().padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (stage) {
                        "email" -> {
                            SecurityCodeInput("6-digit code", uiState.securityCode, onCodeChange, onSubmitCode, "security_email_code_input")
                            TextButton(onClick = onResendEmailCode, enabled = !uiState.securityBusy && uiState.securityWaitSeconds == 0,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("security_email_resend_button")) {
                                Text(if (uiState.securityWaitSeconds > 0) "Resend in ${uiState.securityWaitSeconds}s" else stringResource(R.string.verify_send_new_code),
                                    style = ServixType.bodySm, color = if (uiState.securityWaitSeconds > 0) MutedGray else ForestGreen)
                            }
                        }
                        "enroll" -> {
                            val setup = uiState.securityTotpSetup
                            Column(Modifier.fillMaxWidth().background(PureWhite, RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = DeepForest, modifier = Modifier.size(24.dp))
                                    Text("Google Authenticator", style = ServixType.titleMd, color = Charcoal)
                                }
                                if (setup == null) {
                                    Text("Set up once with a setup key or authenticator app. Then use the rotating six-digit code in your app—even offline.", style = ServixType.bodySm, color = MutedGray)
                                    ServixButton("Set up authenticator", onBeginTotpSetup, enabled = !uiState.securityBusy,
                                        modifier = Modifier.fillMaxWidth(), testTag = "security_choose_totp_button")
                                } else {
                                    Text("Open Google Authenticator and tap +. Choose Enter a setup key. Paste the key below, then enter the 6-digit code.", style = ServixType.bodySm, color = MutedGray)
                                    Row(Modifier.fillMaxWidth().background(SoftSand, RoundedCornerShape(16.dp)).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(setup.secret, style = ServixType.bodyMd.copy(fontFamily = FontFamily.Monospace), color = DeepForest,
                                            modifier = Modifier.weight(1f).testTag("security_totp_secret_text"))
                                        TextButton(onClick = { copyToClipboard(context, "Servix setup key", setup.secret) }) { Text("Copy key", style = ServixType.labelLg, color = DeepForest) }
                                    }
                                    if (setup.uri.startsWith("otpauth://")) {
                                        SecuritySecondaryButton("Open app", onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(setup.uri))) } }, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                            if (setup != null) SecurityCodeInput("6-digit code", uiState.securityCode, onCodeChange, onSubmitCode, "security_totp_enroll_input")
                        }
                        "factor" -> SecurityCodeInput(if (uiState.securityUseRecovery) "Recovery code" else "6-digit code", uiState.securityCode,
                            onCodeChange, onSubmitCode, "security_factor_code_input", numeric = !uiState.securityUseRecovery)
                        "recovery" -> {
                            Column(Modifier.fillMaxWidth().background(PureWhite, RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.securityRecoveryCodes.chunked(2).forEach { pair ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        pair.forEach { code -> Text(code, style = ServixType.bodyMd.copy(fontFamily = FontFamily.Monospace), color = DeepForest, modifier = Modifier.weight(1f)) }
                                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                            SecuritySecondaryButton("Copy recovery codes", onClick = { copyToClipboard(context, "Servix recovery codes", uiState.securityRecoveryCodes.joinToString("\n")) },
                                enabled = uiState.securityRecoveryCodes.isNotEmpty(), modifier = Modifier.fillMaxWidth())
                            Row(Modifier.fillMaxWidth().clickable { onSavedRecoveryChange(!uiState.securitySavedRecovery) }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = uiState.securitySavedRecovery, onCheckedChange = onSavedRecoveryChange,
                                    colors = CheckboxDefaults.colors(checkedColor = DeepForest), modifier = Modifier.testTag("security_saved_recovery_checkbox"))
                                Text("I have saved my recovery codes somewhere private.", style = ServixType.bodyMd, color = Charcoal)
                            }
                        }
                        "reset_password" -> {
                            ServixField(uiState.securityNewPassword, onNewPasswordChange, "New password", placeholder = "Create a password",
                                leadingIcon = Icons.Outlined.Lock, keyboardType = KeyboardType.Password,
                                visualTransformation = if (showResetPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailing = { IconButton(onClick = { showResetPassword = !showResetPassword }) {
                                    Icon(if (showResetPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (showResetPassword) "Hide password" else "Show password", tint = MutedGray)
                                } }, testTag = "security_new_password_input")
                            ServixField(uiState.securityConfirmPassword, onConfirmPasswordChange, "Confirm password", placeholder = "Repeat your password",
                                leadingIcon = Icons.Outlined.Lock, keyboardType = KeyboardType.Password,
                                visualTransformation = if (showResetPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                testTag = "security_confirm_password_input")
                            SecurityPasswordRules(uiState)
                        }
                        "finish" -> Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = DeepForest, modifier = Modifier.size(72.dp))
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                uiState.securitySuccessMode == "reset" -> SecurityPrimaryButton(stringResource(R.string.back_to_sign_in), onCancelAndReturnLogin,
                    modifier = Modifier.fillMaxWidth(), testTag = "security_reset_back_login_button")
                uiState.securityConfigEnabled == false -> {
                    SecurityPrimaryButton(stringResource(R.string.open_servix_web), { CustomTabLauncher.launchWebPath(context, "/login") },
                        modifier = Modifier.fillMaxWidth(), testTag = "security_open_web_button")
                    SecuritySecondaryButton(stringResource(R.string.back_to_sign_in), onCancelAndReturnLogin, modifier = Modifier.fillMaxWidth())
                }
                uiState.securityLoading -> Unit
                uiState.securityExpired || uiState.securityLoadFailed || flow == null -> {
                    SecurityPrimaryButton(stringResource(R.string.security_start_again), onCancelAndReturnLogin, modifier = Modifier.fillMaxWidth(), testTag = "security_start_again_button")
                    if (!uiState.securityExpired) SecuritySecondaryButton(stringResource(R.string.security_try_loading_again), onLoadSecurityCheck,
                        modifier = Modifier.fillMaxWidth(), testTag = "security_retry_load_button")
                }
                stage == "email" -> SecurityPrimaryButton(stringResource(R.string.verify_submit), onSubmitCode, loading = uiState.securityBusy,
                    modifier = Modifier.fillMaxWidth(), testTag = "security_email_verify_button")
                stage == "enroll" && uiState.securityTotpSetup != null -> {
                    SecurityPrimaryButton("Confirm authenticator", onSubmitCode, loading = uiState.securityBusy, modifier = Modifier.fillMaxWidth(), testTag = "security_totp_enroll_submit")
                    TextButton(onClick = onClearTotpSetup, enabled = !uiState.securityBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Choose a different method", style = ServixType.labelLg, color = DeepForest)
                    }
                }
                stage == "factor" -> {
                    SecurityPrimaryButton("Verify security code", onSubmitCode, loading = uiState.securityBusy, modifier = Modifier.fillMaxWidth(), testTag = "security_factor_submit_button")
                    TextButton(onClick = onToggleUseRecovery, enabled = !uiState.securityBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("security_recovery_toggle")) {
                        Text(if (uiState.securityUseRecovery) "Use my enrolled security method" else "Lost access? Use a recovery code", style = ServixType.labelLg, color = DeepForest)
                    }
                    TextButton(onClick = { CustomTabLauncher.launchWebPath(context, "/contact") }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Contact Servix support", style = ServixType.labelLg, color = MutedGray)
                    }
                }
                stage == "recovery" -> SecurityPrimaryButton("Finish securely", onFinishSecurity,
                    enabled = !uiState.securityBusy && uiState.securitySavedRecovery && uiState.securityRecoveryCodes.isNotEmpty(),
                    loading = uiState.securityBusy, modifier = Modifier.fillMaxWidth(), testTag = "security_finish_recovery_button")
                stage == "reset_password" -> SecurityPrimaryButton("Save new password", onSubmitResetPassword, loading = uiState.securityBusy,
                    modifier = Modifier.fillMaxWidth(), testTag = "security_save_password_button")
                stage == "finish" -> SecurityPrimaryButton("Finish signing in", onFinishSecurity, loading = uiState.securityBusy,
                    modifier = Modifier.fillMaxWidth(), testTag = "security_finish_button")
            }
        }
    }
}

@Composable
private fun SecurityPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, loading: Boolean = false, testTag: String? = null) {
    Button(onClick = onClick, enabled = enabled && !loading, shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DeepForest, contentColor = PureWhite),
        modifier = modifier.heightIn(min = 52.dp).let { if (testTag != null) it.testTag(testTag) else it }) {
        if (loading) androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PureWhite)
        else Text(text, style = ServixType.labelLg)
    }
}

@Composable
private fun SecuritySecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, testTag: String? = null) {
    Button(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, ng.name.servix.app.ui.theme.BorderStrongSand),
        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = DeepForest),
        modifier = modifier.heightIn(min = 52.dp).let { if (testTag != null) it.testTag(testTag) else it }) {
        Text(text, style = ServixType.labelLg)
    }
}

@Composable
private fun SecurityCodeInput(label: String, code: String, onChange: (String) -> Unit, onDone: () -> Unit, tag: String, numeric: Boolean = true) {
    ServixField(value = code, onValueChange = onChange, label = label, placeholder = if (numeric) "••• •••" else "Recovery code",
        leadingIcon = Icons.Outlined.Lock, keyboardType = if (numeric) KeyboardType.NumberPassword else KeyboardType.Ascii,
        imeAction = ImeAction.Done, onImeAction = onDone, testTag = tag)
}

@Composable
private fun SecurityPasswordRules(state: AuthUiState) {
    val rules = state.securityResetPasswordRules.associateBy { it.id }
    val displayRules = mutableListOf("At least 8 characters" to (rules["length"]?.met == true))
    if (state.securityFlow?.passwordPolicy != "basic") {
        displayRules.add("Upper and lower case" to (rules["uppercase"]?.met == true && rules["lowercase"]?.met == true))
        displayRules.add("A number" to (rules["number"]?.met == true))
        displayRules.add("A symbol" to (rules["symbol"]?.met == true))
    }
    displayRules.add("Passwords match" to (state.securityNewPassword.isNotEmpty() && state.securityNewPassword == state.securityConfirmPassword))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        displayRules.forEach { (label, met) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (met) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (met) "Met" else "Not met", tint = if (met) ForestGreen else MutedGray, modifier = Modifier.size(18.dp))
                Text(label, style = ServixType.bodySm, color = if (met) Charcoal else MutedGray)
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
    Column(Modifier.fillMaxSize().background(WarmIvory)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackToSignIn, modifier = Modifier.size(44.dp).testTag("auth_back_button")) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = Charcoal)
            }
            Text("Password Recovery", style = ServixType.titleLg, color = Charcoal, modifier = Modifier.weight(1f))
            Box(Modifier.size(32.dp).background(DeepForest, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(80.dp).background(SurfaceContainerHigh, CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(56.dp).background(SoftSand, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(30.dp))
                    }
                    Box(Modifier.align(Alignment.BottomEnd).size(28.dp).background(ForestGreen, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                    }
                }
                Row(Modifier.padding(top = 16.dp, bottom = 8.dp).background(SurfaceContainerLow, CircleShape).padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                    Text("Account recovery", style = ServixType.labelMd, color = ForestGreen)
                }
                Text("Reset your password", style = ServixType.headlineLgMobile, color = Charcoal, textAlign = TextAlign.Center)
                Text("Enter your account email to begin secure password recovery.", style = ServixType.bodyMd, color = MutedGray,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp).widthIn(max = 320.dp))
            }
            if (uiState.forgotSent) {
                Row(Modifier.fillMaxWidth().background(SoftSand, RoundedCornerShape(24.dp)).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(32.dp).background(ForestGreen, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.MailOutline, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.forgot_sent_title), style = ServixType.titleMd, color = DeepForest)
                        Text(stringResource(R.string.forgot_sent_body, uiState.forgotEmail.trim()), style = ServixType.bodySm, color = ForestGreen)
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Email address", style = ServixType.labelLg, color = Charcoal)
                            Text("Required", style = ServixType.labelMd, color = MutedGray)
                        }
                        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).background(SurfaceContainerLow, RoundedCornerShape(24.dp)).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.MailOutline, contentDescription = null, tint = MutedGray, modifier = Modifier.size(20.dp))
                            Box(Modifier.weight(1f)) {
                                if (uiState.forgotEmail.isEmpty()) Text("olumide@example.ng", style = ServixType.bodyLg, color = MutedGray.copy(alpha = 0.7f))
                                androidx.compose.foundation.text.BasicTextField(
                                    value = uiState.forgotEmail, onValueChange = onEmailChange, singleLine = true,
                                    textStyle = ServixType.bodyLg.copy(color = Charcoal),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); onSubmit() }),
                                    modifier = Modifier.fillMaxWidth().testTag("forgot_email_input"),
                                )
                            }
                        }
                        uiState.forgotError?.let { Text(it, style = ServixType.bodySm, color = DangerRed) }
                    }
                    ServixButton(text = if (uiState.forgotSubmitting) stringResource(R.string.forgot_sending) else "Send reset link",
                        onClick = { focusManager.clearFocus(); onSubmit() }, loading = uiState.forgotSubmitting,
                        modifier = Modifier.fillMaxWidth(), testTag = "forgot_submit_button")
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Remember your password?", style = ServixType.bodyMd, color = MutedGray)
                TextButton(onClick = onBackToSignIn, modifier = Modifier.heightIn(min = 48.dp).testTag(if (uiState.forgotSent) "forgot_back_to_login_button" else "forgot_return_login_button")) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = DeepForest, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Back to sign in", style = ServixType.labelLg.copy(textDecoration = TextDecoration.Underline), color = DeepForest)
                }
            }
        }
    }
}
