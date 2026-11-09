package ng.name.servix.app.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.core.content.ContextCompat
import ng.name.servix.app.data.model.OAuthReturn
import ng.name.servix.app.notifications.NotificationPrefs
import ng.name.servix.app.notifications.NotificationsWorker
import ng.name.servix.app.ui.account.AccountHubScreen
import ng.name.servix.app.ui.notifications.NotificationsScreen
import ng.name.servix.app.ui.notifications.NotificationsViewModel
import ng.name.servix.app.util.OAuthReturnBus
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import ng.name.servix.app.ui.motion.Motion
import ng.name.servix.app.ui.motion.rememberSuccessTick
import ng.name.servix.app.ui.motion.motionMs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ng.name.servix.app.location.storedCity
import ng.name.servix.app.BuildConfig
import ng.name.servix.app.R
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.repository.AiRepository
import ng.name.servix.app.data.repository.AccountRepository
import ng.name.servix.app.data.repository.BookingRepository
import ng.name.servix.app.data.repository.CatalogueRepository
import ng.name.servix.app.data.repository.SessionState
import ng.name.servix.app.ui.ai.AiScreen
import ng.name.servix.app.ui.ai.AiViewModel
import ng.name.servix.app.ui.account.DeviceCard
import ng.name.servix.app.ui.auth.AuthViewModel
import ng.name.servix.app.ui.auth.ForgotPasswordScreen
import ng.name.servix.app.ui.auth.LoginScreen
import ng.name.servix.app.ui.auth.PostAuthDestination
import ng.name.servix.app.ui.auth.RegisterScreen
import ng.name.servix.app.ui.auth.SecurityCheckScreen
import ng.name.servix.app.ui.auth.VerifyEmailScreen
import ng.name.servix.app.ui.auth.WelcomeScreen
import ng.name.servix.app.ui.bookings.BookingDetailScreen
import ng.name.servix.app.ui.bookings.BookingDetailViewModel
import ng.name.servix.app.ui.bookings.BookingsScreen
import ng.name.servix.app.ui.bookings.BookingsViewModel
import ng.name.servix.app.ui.catalogue.ProfessionalProfileScreen
import ng.name.servix.app.ui.catalogue.ProfessionalProfileViewModel
import ng.name.servix.app.ui.catalogue.SearchMode
import ng.name.servix.app.ui.explore.ExploreScreen
import ng.name.servix.app.ui.catalogue.SearchViewModel
import ng.name.servix.app.ui.catalogue.ServiceDetailScreen
import ng.name.servix.app.ui.catalogue.ServiceDetailViewModel
import ng.name.servix.app.ui.components.OfflineBanner
import ng.name.servix.app.ui.components.ServixBadge
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixLogoMark
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.components.SkeletonBox
import ng.name.servix.app.ui.chat.ChatContactCard
import ng.name.servix.app.ui.chat.AiThreadScreen
import ng.name.servix.app.ui.chat.ChatsScreen
import ng.name.servix.app.ui.chat.ChatsSignedOut
import ng.name.servix.app.ui.chat.ChatsViewModel
import ng.name.servix.app.ui.chat.ConversationScreen
import ng.name.servix.app.ui.chat.ConversationViewModel
import ng.name.servix.app.data.realtime.ChatEventStream
import ng.name.servix.app.data.repository.ChatRepository
import ng.name.servix.app.data.repository.FeaturesRepository
import ng.name.servix.app.data.repository.UploadRepository
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.outlined.Chat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ng.name.servix.app.ui.home.HomeTabScreen
import ng.name.servix.app.ui.home.HomeWeek
import ng.name.servix.app.ui.home.HomeViewModel
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.updates.UpdateCard
import ng.name.servix.app.updates.UpdateUiState
import ng.name.servix.app.updates.rememberUpdateUiState
import ng.name.servix.app.util.AppLinkBus
import ng.name.servix.app.util.CustomTabLauncher

private data class BottomTabItem(
    val route: ServixRoute,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private fun homeTab() = BottomTabItem(
    route = ServixRoute.Home,
    labelRes = R.string.nav_home,
    selectedIcon = Icons.Filled.Home,
    unselectedIcon = Icons.Outlined.Home,
    testTag = "nav_tab_home",
)

private fun exploreTab() = BottomTabItem(
    route = ServixRoute.Search,
    labelRes = R.string.nav_explore,
    selectedIcon = Icons.Filled.Search,
    unselectedIcon = Icons.Outlined.Search,
    testTag = "nav_tab_search",
)

private fun bookingsTab() = BottomTabItem(
    route = ServixRoute.Bookings,
    labelRes = R.string.nav_bookings,
    selectedIcon = Icons.Filled.CalendarMonth,
    unselectedIcon = Icons.Outlined.CalendarMonth,
    testTag = "nav_tab_bookings",
)

private fun aiTab() = BottomTabItem(
    route = ServixRoute.ServixAi,
    labelRes = R.string.nav_ai,
    selectedIcon = Icons.Filled.AutoAwesome,
    unselectedIcon = Icons.Outlined.AutoAwesome,
    testTag = "nav_tab_ai",
)

private fun accountTab() = BottomTabItem(
    route = ServixRoute.Account,
    labelRes = R.string.nav_account,
    selectedIcon = Icons.Filled.AccountCircle,
    unselectedIcon = Icons.Outlined.AccountCircle,
    testTag = "nav_tab_account",
)

private fun chatsTab() = BottomTabItem(
    route = ServixRoute.Chats,
    labelRes = R.string.nav_chats,
    selectedIcon = Icons.Filled.Chat,
    unselectedIcon = Icons.Outlined.Chat,
    testTag = "nav_tab_chats",
)

/**
 * Release 1.1 IA: `Home · Explore · Chats · Bookings · Account`.
 *
 * Chats is the centre item and only exists when `GET /features` says `community` is on — no empty tab.
 * Guests get `Home · Explore · Account` only: they have no threads and no bookings, and a tab whose
 * only content is a sign-in wall is a dead end (approved in `docs/ia/IA_AUDIT.md`, decision 6).
 */
private fun bottomTabsFor(signedIn: Boolean, chatsEnabled: Boolean): List<BottomTabItem> = buildList {
    add(homeTab())
    add(exploreTab())
    if (signedIn && chatsEnabled) add(chatsTab())
    if (signedIn) add(bookingsTab())
    add(accountTab())
}

@Composable
fun ServixAppShell(
    authViewModel: AuthViewModel,
    catalogueRepository: CatalogueRepository,
    bookingRepository: BookingRepository,
    accountRepository: AccountRepository,
    aiRepository: AiRepository,
    chatRepository: ChatRepository,
    uploadRepository: UploadRepository,
    featuresRepository: FeaturesRepository,
    chatEventStream: ChatEventStream,
    isOnline: Boolean,
) {
    val context = LocalContext.current
    val cityFlow = remember(context) { storedCity(context) }
    val city by cityFlow.collectAsStateWithLifecycle(initialValue = null)
    val updateState = rememberUpdateUiState()
    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    // Pending destination to return to when a guest signs in from the Booking Sheet
    var pendingPostAuthRoute by remember { mutableStateOf<ServixRoute?>(null) }

    val aiViewModel: AiViewModel = viewModel(factory = AiViewModel.factory(aiRepository, catalogueRepository))
    val currentUserId = (sessionState as? SessionState.Authenticated)?.user?.id
    LaunchedEffect(currentUserId) { aiViewModel.setUser(currentUserId) }

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(catalogueRepository, accountRepository, bookingRepository)
    )
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.factory(catalogueRepository)
    )
    val bookingsViewModel: BookingsViewModel = viewModel(
        factory = BookingsViewModel.factory(bookingRepository)
    )

    // Release 1.1: flags decide what exists at all; the chat store owns the unread number.
    val featuresState by featuresRepository.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { featuresRepository.refresh() }
    val sessionUserId = (sessionState as? SessionState.Authenticated)?.user?.id
    val sessionRole = (sessionState as? SessionState.Authenticated)?.user?.role
    LaunchedEffect(sessionUserId, sessionRole) {
        homeViewModel.clearProfessionalHome()
        if (sessionUserId != null && sessionRole == "professional") homeViewModel.loadProfessionalHome(sessionUserId)
    }
    val chatsViewModel: ChatsViewModel = viewModel(
        factory = ChatsViewModel.factory(chatRepository, featuresRepository, chatEventStream) { sessionUserId }
    )
    val chatsState by chatsViewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val shellScope = rememberCoroutineScope()
    // Chat entry point from a booking; the API creates or returns the one thread per booking.
    fun openChatForBooking(bookingId: String) {
        shellScope.launch {
            chatRepository.startFromBooking(bookingId).onSuccess { threadId ->
                navController.navigate(ServixRoute.ChatConversation(threadId))
            }
        }
    }
    DisposableEffect(lifecycleOwner, sessionUserId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (sessionUserId != null) chatsViewModel.startStream()
                Lifecycle.Event.ON_STOP -> chatsViewModel.stopStream()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && sessionUserId != null) {
            chatsViewModel.startStream()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            chatsViewModel.stopStream()
        }
    }

    fun navigateAfterAuthComplete() {
        val target = pendingPostAuthRoute ?: ServixRoute.Home
        pendingPostAuthRoute = null
        navController.navigate(target) {
            popUpTo(0) { inclusive = true }
        }
    }

    fun openExplore(query: String?, categorySlug: String?) {
        searchViewModel.applyExternalNavigation(query = query, categorySlug = categorySlug)
        navController.navigate(ServixRoute.Search) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val isAuthDestination = currentDest?.let { dest ->
        dest.hasRoute(ServixRoute.Welcome::class) ||
            dest.hasRoute(ServixRoute.Login::class) ||
            dest.hasRoute(ServixRoute.Register::class) ||
            dest.hasRoute(ServixRoute.VerifyEmail::class) ||
            dest.hasRoute(ServixRoute.SecurityCheck::class) ||
            dest.hasRoute(ServixRoute.ForgotPassword::class)
    } ?: false

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

    val isAuthenticated = sessionState is SessionState.Authenticated
    var unreadNotifications by remember { mutableStateOf(0) }
    // One light tick when a sign-in (email, provider or security finish) lands.
    val signInTick = rememberSuccessTick()
    var lastAuthState by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(isAuthenticated) {
        if (lastAuthState == false && isAuthenticated) signInTick()
        lastAuthState = isAuthenticated
    }

    LaunchedEffect(isAuthenticated, currentDest) {
        unreadNotifications = if (isAuthenticated) {
            accountRepository.getUnreadNotificationCount().getOrNull()?.unread ?: 0
        } else {
            0
        }
    }
    val startRoute: ServixRoute = if (isAuthenticated) {
        ServixRoute.Home
    } else {
        ServixRoute.Welcome
    }

    val shellContext = LocalContext.current
    val notificationPrefs = remember { NotificationPrefs(shellContext) }
    var showNotifRationale by remember { mutableStateOf(false) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        notificationPrefs.permissionAsked = true
        showNotifRationale = false
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            NotificationsWorker.schedule(shellContext)
            val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(shellContext, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (needsAsk && !notificationPrefs.permissionAsked) showNotifRationale = true
        } else {
            NotificationsWorker.cancel(shellContext)
            notificationPrefs.clear()
        }
    }

    LaunchedEffect(Unit) { authViewModel.loadOAuthProviders() }

    LaunchedEffect(Unit) {
        OAuthReturnBus.returns.collect { result ->
            authViewModel.handleOAuthReturn(result.ticket, result.error) { destination ->
                val route = when (destination) {
                    PostAuthDestination.SECURITY_CHECK -> ServixRoute.SecurityCheck
                    PostAuthDestination.VERIFY_EMAIL -> ServixRoute.VerifyEmail
                    else -> ServixRoute.Home
                }
                navController.navigate(route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        AppLinkBus.links.collect { target ->
            when {
                target.bookingId != null -> navController.navigate(ServixRoute.BookingDetail(target.bookingId!!))
                target.notifications -> navController.navigate(ServixRoute.Notifications)
            }
        }
    }

    if (showNotifRationale) {
        AlertDialog(
            onDismissRequest = {
                notificationPrefs.permissionAsked = true
                showNotifRationale = false
            },
            title = { Text(stringResource(R.string.notifications_rationale_title)) },
            text = { Text(stringResource(R.string.notifications_rationale_body)) },
            confirmButton = {
                TextButton(onClick = {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(stringResource(R.string.notifications_rationale_allow)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    notificationPrefs.permissionAsked = true
                    showNotifRationale = false
                }) { Text(stringResource(R.string.notifications_rationale_later)) }
            },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = WarmIvory,
        topBar = {
            OfflineBanner(isOnline = isOnline)
        },
        bottomBar = {
            if (!isAuthDestination) {
                val visibleTabs = bottomTabsFor(
                    signedIn = sessionState is SessionState.Authenticated,
                    chatsEnabled = featuresState.flags.community,
                )
                val tabs = visibleTabs.map { tab ->
                    val isChats = tab.route is ServixRoute.Chats
                    ServixTab(
                        label = stringResource(tab.labelRes),
                        selectedIcon = tab.selectedIcon,
                        unselectedIcon = tab.unselectedIcon,
                        testTag = tab.testTag,
                        // One source: Σ unreadCount from GET /community/threads, kept fresh by the stream.
                        badgeCount = if (isChats) chatsState.unreadTotal else null,
                        prominent = isChats,
                    )
                }
                val selectedIndex = visibleTabs.indexOfFirst { tab ->
                    when (tab.route) {
                        is ServixRoute.Search -> {
                            currentDest?.hasRoute(ServixRoute.Search::class) == true ||
                                currentDest?.hasRoute(ServixRoute.ProfessionalProfile::class) == true ||
                                currentDest?.hasRoute(ServixRoute.ServiceDetail::class) == true
                        }
                        is ServixRoute.Bookings -> {
                            currentDest?.hasRoute(ServixRoute.Bookings::class) == true ||
                                currentDest?.hasRoute(ServixRoute.BookingDetail::class) == true
                        }
                        is ServixRoute.Chats -> {
                            currentDest?.hasRoute(ServixRoute.Chats::class) == true ||
                                currentDest?.hasRoute(ServixRoute.ChatConversation::class) == true
                        }
                        else -> currentDest?.hasRoute(tab.route::class) == true
                    }
                }
                ServixBottomBar(
                    tabs = tabs,
                    selectedIndex = selectedIndex,
                    onSelect = { index ->
                        val tab = visibleTabs.getOrNull(index)
                        if (tab != null) {
                            if (tab.route is ServixRoute.Search && currentDest?.hasRoute(ServixRoute.Search::class) != true) {
                                searchViewModel.ensureInitialized()
                            }
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        }
    ) { innerPadding ->
        val screenMs = motionMs(Motion.SCREEN_MS)
        val tabMs = motionMs(Motion.TAB_MS)
        val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(screenMs, easing = FastOutSlowInEasing)
        val fadeSpec = tween<Float>(screenMs, easing = FastOutSlowInEasing)
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            // Shared-axis X pushes: new screen slides in from the end, old one drifts back.
            enterTransition = {
                slideInHorizontally(slideSpec) { full -> full / 4 } + fadeIn(fadeSpec)
            },
            exitTransition = {
                slideOutHorizontally(slideSpec) { full -> -full / 12 } + fadeOut(fadeSpec)
            },
            popEnterTransition = {
                slideInHorizontally(slideSpec) { full -> -full / 12 } + fadeIn(fadeSpec)
            },
            popExitTransition = {
                slideOutHorizontally(slideSpec) { full -> full / 4 } + fadeOut(fadeSpec)
            },
        ) {
            composable<ServixRoute.Welcome> {
                WelcomeScreen(
                    onSignInClick = { navController.navigate(ServixRoute.Login) },
                    onCreateAccountClick = { navController.navigate(ServixRoute.Register) },
                    onBrowseFirstClick = {
                        navController.navigate(ServixRoute.Home) {
                            popUpTo(ServixRoute.Welcome) { inclusive = true }
                        }
                    },
                    providers = authUiState.oauthProviders,
                    onProviderClick = { provider ->
                        CustomTabLauncher.launchUrl(shellContext, OAuthReturnBus.nativeStartUrl(provider))
                    },
                )
            }

            composable<ServixRoute.Login> {
                LoginScreen(
                    uiState = authUiState,
                    onEmailChange = authViewModel::updateLoginEmail,
                    onPasswordChange = authViewModel::updateLoginPassword,
                    onSubmit = {
                        authViewModel.submitLogin { dest ->
                            when (dest) {
                                PostAuthDestination.SECURITY_CHECK -> {
                                    navController.navigate(ServixRoute.SecurityCheck) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                                PostAuthDestination.VERIFY_EMAIL,
                                PostAuthDestination.DASHBOARD -> navigateAfterAuthComplete()
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
                    providers = authUiState.oauthProviders,
                    onProviderClick = { provider ->
                        CustomTabLauncher.launchUrl(shellContext, OAuthReturnBus.nativeStartUrl(provider))
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
                    onTermsAcceptedChange = authViewModel::updateRegisterTermsAccepted,
                    onSubmit = {
                        authViewModel.submitRegister { dest ->
                            when (dest) {
                                PostAuthDestination.SECURITY_CHECK -> {
                                    navController.navigate(ServixRoute.SecurityCheck) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                                PostAuthDestination.VERIFY_EMAIL,
                                PostAuthDestination.DASHBOARD -> navigateAfterAuthComplete()
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
                    },
                    providers = authUiState.oauthProviders,
                    onProviderClick = { provider ->
                        CustomTabLauncher.launchUrl(shellContext, OAuthReturnBus.nativeStartUrl(provider))
                    },
                )
            }

            composable<ServixRoute.VerifyEmail> {
                val user = (sessionState as? SessionState.Authenticated)?.user
                VerifyEmailScreen(
                    userEmail = user?.email.orEmpty(),
                    isAlreadyVerified = user?.emailVerified == true,
                    uiState = authUiState,
                    onLoadMethod = authViewModel::loadVerificationMethod,
                    onOtpCodeChange = authViewModel::updateOtpCode,
                    onSubmitOtp = authViewModel::submitOtpCode,
                    onResend = authViewModel::resendVerificationEmail,
                    onCheckVerifiedStatus = {
                        authViewModel.checkEmailVerifiedStatus {
                            navigateAfterAuthComplete()
                        }
                    },
                    onContinueToHome = {
                        navigateAfterAuthComplete()
                    },
                    onSignOut = {
                        pendingPostAuthRoute = null
                        authViewModel.logout {
                            navController.navigate(ServixRoute.Welcome) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onRegisterAgain = {
                        pendingPostAuthRoute = null
                        authViewModel.logout {
                            navController.navigate(ServixRoute.Register) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                )
            }

            composable<ServixRoute.SecurityCheck> {
                SecurityCheckScreen(
                    uiState = authUiState,
                    onLoadSecurityCheck = authViewModel::loadSecurityCheck,
                    onCodeChange = authViewModel::updateSecurityCode,
                    onSubmitCode = authViewModel::submitSecurityCode,
                    onResendEmailCode = authViewModel::resendSecurityEmailCode,
                    onBeginTotpSetup = authViewModel::beginSecurityTotpSetup,
                    onClearTotpSetup = authViewModel::clearSecurityTotpSetup,
                    onToggleUseRecovery = authViewModel::toggleSecurityUseRecovery,
                    onSavedRecoveryChange = authViewModel::updateSecuritySavedRecovery,
                    onFinishSecurity = {
                        authViewModel.finishSecurityFlow {
                            navigateAfterAuthComplete()
                        }
                    },
                    onNewPasswordChange = authViewModel::updateSecurityNewPassword,
                    onConfirmPasswordChange = authViewModel::updateSecurityConfirmPassword,
                    onSubmitResetPassword = authViewModel::submitSecurityResetPassword,
                    onCancelAndReturnLogin = {
                        authViewModel.cancelSecurityFlow {
                            navController.navigate(ServixRoute.Login) {
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
                    onSubmit = {
                        authViewModel.submitForgotPassword(
                            onSecurityCheck = {
                                navController.navigate(ServixRoute.SecurityCheck) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    },
                    onBackToSignIn = {
                        if (!navController.popBackStack()) {
                            navController.navigate(ServixRoute.Login) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable<ServixRoute.Home>(
                enterTransition = { fadeIn(tween(tabMs)) },
                exitTransition = { fadeOut(tween(tabMs)) },
                popEnterTransition = { fadeIn(tween(tabMs)) },
                popExitTransition = { fadeOut(tween(tabMs)) },
            ) {
                val user = (sessionState as? SessionState.Authenticated)?.user
                val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                val aiUiState by aiViewModel.state.collectAsStateWithLifecycle()
                val professionalHome by homeViewModel.professionalState.collectAsStateWithLifecycle()

                LaunchedEffect(user?.id, user?.role, isOnline) {
                    if (user?.role != "professional") homeViewModel.loadHome(userId = user?.id, forceRefresh = false)
                }

                if (user?.role == "professional") {
                    ng.name.servix.app.ui.home.ProfessionalHomeScreen(
                        user = user, state = professionalHome.takeIf { it.userId == user.id } ?: ng.name.servix.app.ui.home.ProfessionalHomeState(),
                        unreadNotifications = unreadNotifications, chatsEnabled = featuresState.flags.community,
                        onNotifications = { navController.navigate(ServixRoute.Notifications) },
                        onAccount = { navController.navigate(ServixRoute.Account) },
                        onSeeAllJobs = { CustomTabLauncher.launchWebPath(shellContext, "/dashboard/work") },
                        onViewJob = { id -> navController.navigate(ServixRoute.BookingDetail(id)) },
                        onJobAction = homeViewModel::professionalBookingAction,
                        onMessage = { id -> openChatForBooking(id) },
                        onEditService = { id -> CustomTabLauncher.launchWebPath(shellContext, "/dashboard/gigs/${android.net.Uri.encode(id)}/edit") },
                        onAi = { navController.navigate(ServixRoute.ServixAi) },
                        onRetry = { homeViewModel.loadProfessionalHome(user.id, forceRefresh = true) },
                    )
                } else HomeTabScreen(
                    user = user,
                    city = city,
                    uiState = homeUiState,
                    aiUsage = aiUiState.usage,
                    onRefresh = { homeViewModel.loadHome(userId = user?.id, forceRefresh = true) },
                    // Chats card: same unread number as the tab badge (one source), AI is inside Chats now.
                    chatsEnabled = featuresState.flags.community,
                    chatsUnread = chatsState.unreadTotal,
                    onOpenChats = { navController.navigate(ServixRoute.Chats) },
                    onOpenSearch = { openExplore(query = null, categorySlug = null) },
                    onOpenCategory = { slug -> openExplore(query = null, categorySlug = slug) },
                    onOpenProfessional = { slug ->
                        navController.navigate(ServixRoute.ProfessionalProfile(slug = slug))
                    },
                    onOpenBookingDetail = { bookingId ->
                        navController.navigate(ServixRoute.BookingDetail(id = bookingId))
                    },
                    onOpenBookingsTab = {
                        navController.navigate(ServixRoute.Bookings) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenAi = {
                        navController.navigate(ServixRoute.ServixAi) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    unreadNotifications = unreadNotifications,
                    onOpenNotifications = { navController.navigate(ServixRoute.Notifications) },
                    onOpenAccount = {
                        navController.navigate(ServixRoute.Account) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onSignInClick = { navController.navigate(ServixRoute.Login) },
                    onCreateAccountClick = { navController.navigate(ServixRoute.Register) },
                )
            }

            composable<ServixRoute.Search>(
                enterTransition = { fadeIn(tween(tabMs)) },
                exitTransition = { fadeOut(tween(tabMs)) },
                popEnterTransition = { fadeIn(tween(tabMs)) },
                popExitTransition = { fadeOut(tween(tabMs)) },
            ) {
                val searchUiState by searchViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    searchViewModel.ensureInitialized()
                }

                ExploreScreen(
                    uiState = searchUiState,
                    categoryNameFor = searchViewModel::categoryNameFor,
                    onQueryChange = searchViewModel::updateQuery,
                    onQuerySubmit = searchViewModel::submitQueryImmediate,
                    onModeChange = searchViewModel::setMode,
                    onCategorySelect = searchViewModel::setCategory,
                    onLocationChange = searchViewModel::setLocation,
                    onRatingSelect = searchViewModel::setRatingFilter,
                    onPriceSelect = searchViewModel::setPriceFilter,
                    onPriceLimitChange = searchViewModel::setPriceLimit,
                    onAvailabilitySelect = searchViewModel::setAvailabilityFilter,
                    onSortSelect = searchViewModel::setSort,
                    onClearFilters = searchViewModel::clearFilters,
                    onRefresh = searchViewModel::refresh,
                    onLoadNextPage = searchViewModel::loadNextPage,
                    onOpenProfessional = { slug ->
                        navController.navigate(ServixRoute.ProfessionalProfile(slug = slug))
                    },
                    onOpenService = { slug ->
                        navController.navigate(ServixRoute.ServiceDetail(slug = slug))
                    },
                )
            }

            composable<ServixRoute.ProfessionalProfile> { backStackEntry ->
                val route = backStackEntry.toRoute<ServixRoute.ProfessionalProfile>()
                val user = (sessionState as? SessionState.Authenticated)?.user
                val profileViewModel: ProfessionalProfileViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = ProfessionalProfileViewModel.factory(catalogueRepository),
                )
                val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(route.slug, user?.id) {
                    profileViewModel.loadProfile(
                        slug = route.slug,
                        isAuthenticated = user != null,
                        forceRefresh = false,
                    )
                }

                ProfessionalProfileScreen(
                    user = user,
                    uiState = profileUiState,
                    categoryNameFor = profileViewModel::categoryNameFor,
                    onBackClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate(ServixRoute.Search)
                        }
                    },
                    onRetry = {
                        profileViewModel.loadProfile(
                            slug = route.slug,
                            isAuthenticated = user != null,
                            forceRefresh = true,
                        )
                    },
                    onBrowseProfessionalsClick = {
                        navController.navigate(ServixRoute.Search) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onToggleSave = profileViewModel::toggleSaveProfessional,
                    onSignInClick = { navController.navigate(ServixRoute.Login) },
                    onOpenService = { serviceSlug ->
                        navController.navigate(ServixRoute.ServiceDetail(slug = serviceSlug))
                    },
                )
            }

            composable<ServixRoute.ServiceDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<ServixRoute.ServiceDetail>()
                val user = (sessionState as? SessionState.Authenticated)?.user
                val serviceViewModel: ServiceDetailViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = ServiceDetailViewModel.factory(catalogueRepository, bookingRepository),
                )
                val serviceUiState by serviceViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(route.slug, route.rebookedFrom, route.initialNotes) {
                    serviceViewModel.loadService(
                        slug = route.slug,
                        rebookedFrom = route.rebookedFrom,
                        initialNotes = route.initialNotes,
                        forceRefresh = false,
                    )
                }

                ServiceDetailScreen(
                    user = user,
                    uiState = serviceUiState,
                    openBookingSheetInitially = route.openBookingSheet,
                    categoryNameFor = serviceViewModel::categoryNameFor,
                    onSelectSlot = serviceViewModel::selectSlot,
                    onNotesChange = serviceViewModel::updateBookingNotes,
                    onSubmitBookAndPay = serviceViewModel::submitBookAndPay,
                    onBackClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate(ServixRoute.Search)
                        }
                    },
                    onRetry = {
                        serviceViewModel.loadService(
                            slug = route.slug,
                            rebookedFrom = route.rebookedFrom,
                            initialNotes = route.initialNotes,
                            forceRefresh = true,
                        )
                    },
                    onBrowseServicesClick = {
                        navController.navigate(ServixRoute.Search) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenProfessional = { proSlug ->
                        navController.navigate(ServixRoute.ProfessionalProfile(slug = proSlug))
                    },
                    onSignInToBookClick = {
                        pendingPostAuthRoute = ServixRoute.ServiceDetail(
                            slug = route.slug,
                            rebookedFrom = serviceUiState.rebookedFrom,
                            initialNotes = serviceUiState.bookingNotes,
                            openBookingSheet = true,
                        )
                        navController.navigate(ServixRoute.Login)
                    },
                    onCreateAccountToBookClick = {
                        pendingPostAuthRoute = ServixRoute.ServiceDetail(
                            slug = route.slug,
                            rebookedFrom = serviceUiState.rebookedFrom,
                            initialNotes = serviceUiState.bookingNotes,
                            openBookingSheet = true,
                        )
                        navController.navigate(ServixRoute.Register)
                    },
                    onNavigateToBookingAfterPay = { bookingId ->
                        bookingsViewModel.loadBookings(forceRefresh = true)
                        homeViewModel.loadHome(userId = user?.id, forceRefresh = true)
                        navController.navigate(
                            ServixRoute.BookingDetail(
                                id = bookingId,
                                pollingAfterPay = true,
                            )
                        )
                    },
                )
            }

            composable<ServixRoute.Bookings>(
                enterTransition = { fadeIn(tween(tabMs)) },
                exitTransition = { fadeOut(tween(tabMs)) },
                popEnterTransition = { fadeIn(tween(tabMs)) },
                popExitTransition = { fadeOut(tween(tabMs)) },
            ) {
                val user = (sessionState as? SessionState.Authenticated)?.user
                val bookingsUiState by bookingsViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(user?.id) {
                    if (user != null) {
                        bookingsViewModel.loadBookings(forceRefresh = false)
                    }
                }

                BookingsScreen(
                    user = user,
                    uiState = bookingsUiState,
                    onTabSelect = bookingsViewModel::selectTab,
                    onRefresh = { bookingsViewModel.loadBookings(forceRefresh = true) },
                    onExploreServicesClick = {
                        searchViewModel.setMode(ng.name.servix.app.ui.catalogue.SearchMode.SERVICES)
                        navController.navigate(ServixRoute.Search) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onSignInClick = { navController.navigate(ServixRoute.Login) },
                    onOpenBooking = { bookingId ->
                        navController.navigate(ServixRoute.BookingDetail(id = bookingId))
                    },
                )
            }

            composable<ServixRoute.BookingDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<ServixRoute.BookingDetail>()
                val user = (sessionState as? SessionState.Authenticated)?.user
                val detailViewModel: BookingDetailViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = BookingDetailViewModel.factory(bookingRepository),
                )
                val detailUiState by detailViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(route.id) {
                    detailViewModel.loadBooking(
                        id = route.id,
                        startPollingIfPending = route.pollingAfterPay,
                        forceRefresh = false,
                    )
                }

                // Keep Bookings list & Home nextBooking synced when detail status changes
                LaunchedEffect(detailUiState.booking?.status, detailUiState.booking?.hasReview) {
                    if (detailUiState.booking != null) {
                        bookingsViewModel.loadBookings(forceRefresh = true)
                        homeViewModel.loadHome(userId = user?.id, forceRefresh = true)
                    }
                }

                BookingDetailScreen(
                    uiState = detailUiState,
                    pollingAfterPay = route.pollingAfterPay,
                    onResumeCheck = detailViewModel::onScreenResumed,
                    onBackClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate(ServixRoute.Bookings)
                        }
                    },
                    onRetry = {
                        detailViewModel.loadBooking(
                            id = route.id,
                            startPollingIfPending = route.pollingAfterPay,
                            forceRefresh = true,
                        )
                    },
                    onRefreshPaymentStatus = {
                        detailViewModel.startPaymentPolling(route.id)
                    },
                    onPayClick = { onOpenUrl ->
                        detailViewModel.initiatePayNow(onOpenUrl)
                    },
                    onOpenModal = detailViewModel::openModal,
                    onCloseModal = detailViewModel::closeModal,
                    onSubmitCancel = detailViewModel::submitCancel,
                    onSubmitConfirmCompletion = detailViewModel::submitConfirmCompletion,
                    onSubmitDispute = detailViewModel::submitDispute,
                    onSubmitReview = detailViewModel::submitReview,
                    onStartBookAgain = detailViewModel::startBookAgain,
                    onContinueRebook = { serviceSlug, rebookedFromId, initialNotes ->
                        navController.navigate(
                            ServixRoute.ServiceDetail(
                                slug = serviceSlug,
                                rebookedFrom = rebookedFromId,
                                initialNotes = initialNotes.takeIf { it.isNotBlank() },
                                openBookingSheet = true,
                            )
                        )
                    },
                    onOpenProfessional = { proSlug ->
                        navController.navigate(ServixRoute.ProfessionalProfile(slug = proSlug))
                    },
                    onOpenService = { serviceSlug ->
                        navController.navigate(ServixRoute.ServiceDetail(slug = serviceSlug))
                    },
                    // Only offered while the server has messaging switched on.
                    onMessageClick = if (featuresState.flags.community) {
                        { openChatForBooking(route.id) }
                    } else {
                        null
                    },
                )
            }

            composable<ServixRoute.ServixAi>(
                enterTransition = { fadeIn(tween(tabMs)) },
                exitTransition = { fadeOut(tween(tabMs)) },
                popEnterTransition = { fadeIn(tween(tabMs)) },
                popExitTransition = { fadeOut(tween(tabMs)) },
            ) {
                val user = (sessionState as? SessionState.Authenticated)?.user
                val aiState by aiViewModel.state.collectAsStateWithLifecycle()
                DisposableEffect(aiViewModel) { onDispose { aiViewModel.stopAll() } }
                AiScreen(
                    user = user,
                    state = aiState,
                    viewModel = aiViewModel,
                    onSignIn = {
                        pendingPostAuthRoute = ServixRoute.ServixAi
                        navController.navigate(ServixRoute.Login)
                    },
                )
            }

            composable<ServixRoute.Notifications> {
                val notificationsViewModel = remember { NotificationsViewModel(accountRepository) }
                val notificationsState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
                NotificationsScreen(
                    uiState = notificationsState,
                    onBackClick = { navController.popBackStack() },
                    onRefresh = { notificationsViewModel.load(refresh = true) },
                    onMarkAllRead = notificationsViewModel::markAllRead,
                    onItemClick = { item ->
                        notificationsViewModel.markRead(item.id)
                        val bookingId = Regex("/bookings/([^/?#]+)").find(item.link.orEmpty())?.groupValues?.get(1)
                        if (!bookingId.isNullOrBlank()) {
                            navController.navigate(ServixRoute.BookingDetail(bookingId))
                        }
                    },
                )
            }

            composable<ServixRoute.ChatAi>(
                enterTransition = { fadeIn(tween(tabMs)) },
                exitTransition = { fadeOut(tween(tabMs)) },
                popEnterTransition = { fadeIn(tween(tabMs)) },
                popExitTransition = { fadeOut(tween(tabMs)) },
            ) {
                val aiUiState by aiViewModel.state.collectAsStateWithLifecycle()
                AiThreadScreen(
                    state = aiUiState,
                    onBack = { navController.popBackStack() },
                    onInput = aiViewModel::updateInput,
                    onSend = { aiViewModel.send() },
                    onRetry = aiViewModel::retry,
                )
            }

            composable<ServixRoute.Account>(
                enterTransition = { fadeIn(tween(tabMs)) },
                exitTransition = { fadeOut(tween(tabMs)) },
                popEnterTransition = { fadeIn(tween(tabMs)) },
                popExitTransition = { fadeOut(tween(tabMs)) },
            ) {
                val user = (sessionState as? SessionState.Authenticated)?.user
                AccountHubScreen(
                    user = user,
                    accountRepository = accountRepository,
                    updateState = updateState,
                    city = city,
                    unreadNotifications = unreadNotifications,
                    onOpenNotifications = { navController.navigate(ServixRoute.Notifications) },
                    onSignInClick = { navController.navigate(ServixRoute.Login) },
                    onCreateAccountClick = { navController.navigate(ServixRoute.Register) },
                    onVerifyEmailClick = { navController.navigate(ServixRoute.VerifyEmail) },
                    onOpenBooking = { bookingId -> navController.navigate(ServixRoute.BookingDetail(id = bookingId)) },
                    onRefreshSessionUser = { authViewModel.refreshSessionUser() },
                    onSignOutClick = {
                        authViewModel.logout {
                            navController.navigate(ServixRoute.Welcome) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable<ServixRoute.Chats>(
                enterTransition = { fadeIn(tween(tabMs)) },
                exitTransition = { fadeOut(tween(tabMs)) },
                popEnterTransition = { fadeIn(tween(tabMs)) },
                popExitTransition = { fadeOut(tween(tabMs)) },
            ) {
                val user = (sessionState as? SessionState.Authenticated)?.user
                if (user == null) {
                    ChatsSignedOut(onSignIn = { navController.navigate(ServixRoute.Login) })
                } else {
                    ChatsScreen(
                        uiState = chatsState,
                        myUserId = user.id,
                        onOpenThread = { threadId -> navController.navigate(ServixRoute.ChatConversation(threadId)) },
                        onOpenAi = { navController.navigate(ServixRoute.ChatAi) },
                        onRetry = { chatsViewModel.refresh() },
                    )
                }
            }

            composable<ServixRoute.ChatConversation>(
                enterTransition = { fadeIn(tween(tabMs)) },
                exitTransition = { fadeOut(tween(tabMs)) },
                popEnterTransition = { fadeIn(tween(tabMs)) },
                popExitTransition = { fadeOut(tween(tabMs)) },
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<ServixRoute.ChatConversation>()
                val conversationViewModel: ConversationViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = ConversationViewModel.factory(
                        chatRepository = chatRepository,
                        uploadRepository = uploadRepository,
                        stream = chatEventStream,
                        threadId = route.threadId,
                        myUserId = sessionUserId.orEmpty(),
                    ),
                )
                val conversationState by conversationViewModel.uiState.collectAsStateWithLifecycle()
                val thread = chatsState.threads.firstOrNull { it.id == route.threadId }
                var contactCardOpen by remember { mutableStateOf(false) }
                LaunchedEffect(route.threadId) { chatsViewModel.markReadLocally(route.threadId) }
                ConversationScreen(
                    uiState = conversationState,
                    thread = thread,
                    onBack = { navController.popBackStack() },
                    onInput = conversationViewModel::onInputChange,
                    onSend = conversationViewModel::send,
                    onRetry = conversationViewModel::retry,
                    onLoadOlder = conversationViewModel::loadOlder,
                    onOpenContactCard = { contactCardOpen = true },
                    onSendImage = conversationViewModel::sendImage,
                    onSendFile = conversationViewModel::sendFile,
                    onSendVoice = conversationViewModel::sendVoice,
                    onCancelReply = conversationViewModel::cancelReply,
                    onOpenActions = conversationViewModel::openActions,
                    onDismissActions = conversationViewModel::closeActions,
                    onReply = conversationViewModel::reply,
                    onToggleReaction = conversationViewModel::toggleReaction,
                    onDeleteMessage = conversationViewModel::deleteForEveryone,
                    onOpenViewer = conversationViewModel::openViewer,
                    onCloseViewer = conversationViewModel::closeViewer,
                    onOpenAttachment = { url -> CustomTabLauncher.launchUrl(context, url) },
                    onSystemMessage = conversationViewModel::showNotice,
                )
                if (contactCardOpen && thread != null) {
                    ChatContactCard(
                        thread = thread,
                        messages = conversationState.messages,
                        onDismiss = { contactCardOpen = false },
                        onViewProfile = { slug ->
                            contactCardOpen = false
                            navController.navigate(ServixRoute.ProfessionalProfile(slug))
                        },
                        onBlock = {
                            conversationViewModel.block()
                            contactCardOpen = false
                        },
                    )
                }
            }
        }
    }
}
