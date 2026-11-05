package ng.name.servix.app.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ng.name.servix.app.BuildConfig
import ng.name.servix.app.R
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.repository.SessionState
import ng.name.servix.app.ui.auth.AuthUiState
import ng.name.servix.app.ui.auth.AuthViewModel
import ng.name.servix.app.ui.auth.ForgotPasswordScreen
import ng.name.servix.app.ui.auth.LoginScreen
import ng.name.servix.app.ui.auth.RegisterScreen
import ng.name.servix.app.ui.auth.VerifyEmailScreen
import ng.name.servix.app.ui.auth.WelcomeScreen
import ng.name.servix.app.ui.components.OfflineBanner
import ng.name.servix.app.ui.components.ProfessionalToolsWebCard
import ng.name.servix.app.ui.components.ServixBadge
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixLogoMark
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.components.SkeletonBox
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.Coral
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.CustomTabLauncher

private data class BottomTabItem(
    val route: ServixRoute,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val BOTTOM_TABS = listOf(
    BottomTabItem(
        route = ServixRoute.Home,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_tab_home",
    ),
    BottomTabItem(
        route = ServixRoute.Search,
        labelRes = R.string.nav_search,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
        testTag = "nav_tab_search",
    ),
    BottomTabItem(
        route = ServixRoute.Bookings,
        labelRes = R.string.nav_bookings,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        testTag = "nav_tab_bookings",
    ),
    BottomTabItem(
        route = ServixRoute.ServixAi,
        labelRes = R.string.nav_ai,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_tab_ai",
    ),
    BottomTabItem(
        route = ServixRoute.Account,
        labelRes = R.string.nav_account,
        selectedIcon = Icons.Filled.AccountCircle,
        unselectedIcon = Icons.Outlined.AccountCircle,
        testTag = "nav_tab_account",
    ),
)

@Composable
fun ServixAppShell(
    authViewModel: AuthViewModel,
    isOnline: Boolean,
) {
    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    val isAuthDestination = currentDest?.let { dest ->
        dest.hasRoute(ServixRoute.Welcome::class) ||
            dest.hasRoute(ServixRoute.Login::class) ||
            dest.hasRoute(ServixRoute.Register::class) ||
            dest.hasRoute(ServixRoute.VerifyEmail::class) ||
            dest.hasRoute(ServixRoute.ForgotPassword::class)
    } ?: false

    // When session initializes, route unauthenticated users to Welcome and authenticated users to Home
    if (sessionState is SessionState.Initializing) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = WarmIvory,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ServixLogoMark()
                Spacer(modifier = Modifier.height(24.dp))
                SkeletonBox(modifier = Modifier.widthIn(max = 220.dp).fillMaxWidth(0.6f), height = 16.dp)
            }
        }
        return
    }

    val startRoute: ServixRoute = when (sessionState) {
        is SessionState.Authenticated -> ServixRoute.Home
        else -> ServixRoute.Welcome
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = WarmIvory,
        topBar = {
            OfflineBanner(isOnline = isOnline)
        },
        bottomBar = {
            if (!isAuthDestination) {
                Column {
                    HorizontalDivider(color = BorderSand, thickness = 1.dp)
                    NavigationBar(
                        containerColor = PureWhite,
                        contentColor = Charcoal,
                        tonalElevation = 0.dp,
                    ) {
                        BOTTOM_TABS.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            val label = stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label,
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepForest,
                                    selectedTextColor = DeepForest,
                                    indicatorColor = SoftSand,
                                    unselectedIconColor = MutedGray,
                                    unselectedTextColor = MutedGray,
                                ),
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable<ServixRoute.Welcome> {
                WelcomeScreen(
                    onSignInClick = { navController.navigate(ServixRoute.Login) },
                    onCreateAccountClick = { navController.navigate(ServixRoute.Register) },
                    onBrowseFirstClick = {
                        navController.navigate(ServixRoute.Home) {
                            popUpTo(ServixRoute.Welcome) { inclusive = true }
                        }
                    }
                )
            }

            composable<ServixRoute.Login> {
                LoginScreen(
                    uiState = authUiState,
                    onEmailChange = authViewModel::updateLoginEmail,
                    onPasswordChange = authViewModel::updateLoginPassword,
                    onSubmit = {
                        authViewModel.submitLogin { needsVerification ->
                            val target = if (needsVerification) ServixRoute.VerifyEmail else ServixRoute.Home
                            navController.navigate(target) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onForgotPasswordClick = {
                        authViewModel.resetForgotState()
                        navController.navigate(ServixRoute.ForgotPassword)
                    },
                    onCreateAccountClick = {
                        navController.navigate(ServixRoute.Register) {
                            popUpTo(ServixRoute.Login) { inclusive = true }
                        }
                    },
                    onBackClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate(ServixRoute.Welcome) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable<ServixRoute.Register> {
                RegisterScreen(
                    uiState = authUiState,
                    onNameChange = authViewModel::updateRegisterName,
                    onEmailChange = authViewModel::updateRegisterEmail,
                    onPasswordChange = authViewModel::updateRegisterPassword,
                    onConfirmPasswordChange = authViewModel::updateRegisterConfirmPassword,
                    onSubmit = {
                        authViewModel.submitRegister {
                            navController.navigate(ServixRoute.VerifyEmail) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onSignInClick = {
                        navController.navigate(ServixRoute.Login) {
                            popUpTo(ServixRoute.Register) { inclusive = true }
                        }
                    },
                    onBackClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate(ServixRoute.Welcome) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable<ServixRoute.VerifyEmail> {
                val user = (sessionState as? SessionState.Authenticated)?.user
                VerifyEmailScreen(
                    userEmail = user?.email.orEmpty(),
                    isAlreadyVerified = user?.isEmailVerified == true,
                    uiState = authUiState,
                    onLoadMethod = authViewModel::loadVerificationMethod,
                    onOtpCodeChange = authViewModel::updateOtpCode,
                    onSubmitOtp = authViewModel::submitOtpCode,
                    onResend = authViewModel::resendVerificationEmail,
                    onCheckVerifiedStatus = authViewModel::checkEmailVerifiedStatus,
                    onContinueToHome = {
                        navController.navigate(ServixRoute.Home) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onSignOut = {
                        authViewModel.logout {
                            navController.navigate(ServixRoute.Welcome) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable<ServixRoute.ForgotPassword> {
                ForgotPasswordScreen(
                    uiState = authUiState,
                    onEmailChange = authViewModel::updateForgotEmail,
                    onSubmit = authViewModel::submitForgotPassword,
                    onBackToSignIn = {
                        if (!navController.popBackStack()) {
                            navController.navigate(ServixRoute.Login) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable<ServixRoute.Home> {
                val user = (sessionState as? SessionState.Authenticated)?.user
                Stage1HomeTab(
                    user = user,
                    onSignInClick = { navController.navigate(ServixRoute.Login) },
                    onCreateAccountClick = { navController.navigate(ServixRoute.Register) },
                    onVerifyEmailClick = { navController.navigate(ServixRoute.VerifyEmail) },
                    onExploreSearchClick = {
                        navController.navigate(ServixRoute.Search) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onSignOutClick = {
                        authViewModel.logout {
                            navController.navigate(ServixRoute.Welcome) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable<ServixRoute.Search> {
                Stage1TabPlaceholder(
                    eyebrow = "CATALOGUE",
                    title = "Find the right professional",
                    description = "Browse verified developers, designers, photographers, marketers, writers and consultants across Nigeria.",
                )
            }

            composable<ServixRoute.Bookings> {
                val user = (sessionState as? SessionState.Authenticated)?.user
                Stage1BookingsPlaceholder(
                    user = user,
                    onSignInClick = { navController.navigate(ServixRoute.Login) },
                )
            }

            composable<ServixRoute.ServixAi> {
                val user = (sessionState as? SessionState.Authenticated)?.user
                Stage1AiPlaceholder(
                    user = user,
                    onSignInClick = { navController.navigate(ServixRoute.Login) },
                )
            }

            composable<ServixRoute.Account> {
                val user = (sessionState as? SessionState.Authenticated)?.user
                Stage1AccountTab(
                    user = user,
                    onSignInClick = { navController.navigate(ServixRoute.Login) },
                    onCreateAccountClick = { navController.navigate(ServixRoute.Register) },
                    onVerifyEmailClick = { navController.navigate(ServixRoute.VerifyEmail) },
                    onSignOutClick = {
                        authViewModel.logout {
                            navController.navigate(ServixRoute.Welcome) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun Stage1HomeTab(
    user: UserDto?,
    onSignInClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onVerifyEmailClick: () -> Unit,
    onExploreSearchClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ServixLogoMark()
            if (user != null) {
                ServixBadge(
                    text = user.role.uppercase(),
                    containerColor = SoftSand,
                    contentColor = DeepForest,
                )
            }
        }

        if (user != null) {
            Text(
                text = "Welcome back, ${user.firstName}.",
                style = MaterialTheme.typography.headlineMedium,
                color = DeepForest,
                modifier = Modifier.testTag("home_greeting_text"),
            )

            if (!user.isEmailVerified) {
                ServixCard(
                    containerColor = SoftSand,
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.verify_email_banner_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = Charcoal,
                        )
                        ServixPrimaryButton(
                            text = stringResource(R.string.verify_email_cta),
                            onClick = onVerifyEmailClick,
                            testTag = "home_verify_email_button",
                        )
                    }
                }
            }

            ProfessionalToolsWebCard(role = user.role)
        }

        // Hero section matching HomePage.jsx
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DeepForest,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.hero_eyebrow).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmIvory.copy(alpha = 0.75f),
                )
                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.headlineLarge,
                    color = WarmIvory,
                )
                Text(
                    text = stringResource(R.string.hero_lead),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmIvory.copy(alpha = 0.88f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                ServixSecondaryButton(
                    text = "Explore Services",
                    onClick = onExploreSearchClick,
                    testTag = "home_explore_services_button",
                )
            }
        }

        if (user == null) {
            ServixCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Sign in to book and track services",
                        style = MaterialTheme.typography.titleMedium,
                        color = Charcoal,
                    )
                    Text(
                        text = stringResource(R.string.escrow_copy),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGray,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ServixPrimaryButton(
                            text = stringResource(R.string.sign_in),
                            onClick = onSignInClick,
                            modifier = Modifier.weight(1f),
                            testTag = "home_sign_in_button",
                        )
                        ServixSecondaryButton(
                            text = stringResource(R.string.create_account),
                            onClick = onCreateAccountClick,
                            modifier = Modifier.weight(1f),
                            testTag = "home_create_account_button",
                        )
                    }
                }
            }
        } else {
            ServixCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = user.fullName.ifBlank { user.email },
                                style = MaterialTheme.typography.titleMedium,
                                color = Charcoal,
                            )
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }
                        ServixBadge(
                            text = if (user.isEmailVerified) "Verified" else "Pending verification",
                            containerColor = if (user.isEmailVerified) ForestGreen.copy(alpha = 0.14f) else Coral.copy(alpha = 0.15f),
                            contentColor = if (user.isEmailVerified) DeepForest else Charcoal,
                        )
                    }
                    HorizontalDivider(color = SoftSand)
                    ServixSecondaryButton(
                        text = stringResource(R.string.sign_out),
                        onClick = onSignOutClick,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "home_sign_out_button",
                    )
                }
            }
        }
    }
}

@Composable
private fun Stage1TabPlaceholder(
    eyebrow: String,
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelSmall,
            color = ForestGreen,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = DeepForest,
        )
        ServixCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                )
            }
        }
    }
}

@Composable
private fun Stage1BookingsPlaceholder(
    user: UserDto?,
    onSignInClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "YOUR ACCOUNT",
            style = MaterialTheme.typography.labelSmall,
            color = ForestGreen,
        )
        Text(
            text = "My Bookings",
            style = MaterialTheme.typography.headlineMedium,
            color = DeepForest,
        )
        ServixCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (user == null) "Sign in to see your bookings." else "No bookings loaded yet.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Charcoal,
                )
                Text(
                    text = stringResource(R.string.escrow_copy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                )
                if (user == null) {
                    ServixPrimaryButton(
                        text = stringResource(R.string.sign_in),
                        onClick = onSignInClick,
                        testTag = "bookings_sign_in_button",
                    )
                }
            }
        }
    }
}

@Composable
private fun Stage1AiPlaceholder(
    user: UserDto?,
    onSignInClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "SERVIX AI",
            style = MaterialTheme.typography.labelSmall,
            color = ForestGreen,
        )
        Text(
            text = if (user != null) "Good to see you, ${user.firstName}." else "Your AI helper",
            style = MaterialTheme.typography.headlineMedium,
            color = DeepForest,
        )
        ServixCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Ask anything about professionals, prices or how Servix works, and get budget guidance from real Servix listings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal,
                )
                Text(
                    text = stringResource(R.string.ai_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                )
                if (user == null) {
                    ServixPrimaryButton(
                        text = stringResource(R.string.sign_in),
                        onClick = onSignInClick,
                        testTag = "ai_sign_in_button",
                    )
                }
            }
        }
    }
}

@Composable
private fun Stage1AccountTab(
    user: UserDto?,
    onSignInClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onVerifyEmailClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "YOUR ACCOUNT",
            style = MaterialTheme.typography.labelSmall,
            color = ForestGreen,
        )
        Text(
            text = stringResource(R.string.nav_account),
            style = MaterialTheme.typography.headlineMedium,
            color = DeepForest,
        )

        if (user == null) {
            ServixCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Sign in to manage your Servix account",
                        style = MaterialTheme.typography.titleMedium,
                        color = Charcoal,
                    )
                    Text(
                        text = "Access your saved professionals, bookings, payment history, notifications and plan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGray,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ServixPrimaryButton(
                            text = stringResource(R.string.sign_in),
                            onClick = onSignInClick,
                            modifier = Modifier.weight(1f),
                            testTag = "account_sign_in_button",
                        )
                        ServixSecondaryButton(
                            text = stringResource(R.string.create_account),
                            onClick = onCreateAccountClick,
                            modifier = Modifier.weight(1f),
                            testTag = "account_create_account_button",
                        )
                    }
                }
            }
        } else {
            ServixCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DeepForest,
                            modifier = Modifier.size(52.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = user.firstName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = WarmIvory,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Charcoal,
                            )
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }
                        ServixBadge(
                            text = user.role.uppercase(),
                            containerColor = SoftSand,
                            contentColor = DeepForest,
                        )
                    }

                    if (!user.isEmailVerified) {
                        HorizontalDivider(color = SoftSand)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Email verification needed",
                                style = MaterialTheme.typography.bodySmall,
                                color = Charcoal,
                                modifier = Modifier.weight(1f),
                            )
                            ServixPrimaryButton(
                                text = stringResource(R.string.verify_email_cta),
                                onClick = onVerifyEmailClick,
                                testTag = "account_verify_email_button",
                            )
                        }
                    }
                }
            }

            ProfessionalToolsWebCard(role = user.role)

            ServixSecondaryButton(
                text = stringResource(R.string.sign_out),
                onClick = onSignOutClick,
                modifier = Modifier.fillMaxWidth(),
                testTag = "account_sign_out_button",
            )
        }

        // Legal & About card
        ServixCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "About Servix",
                    style = MaterialTheme.typography.titleSmall,
                    color = Charcoal,
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} · API: api.servix.name.ng",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
}
