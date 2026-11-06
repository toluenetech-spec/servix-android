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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ng.name.servix.app.ui.components.ProfessionalToolsWebCard
import ng.name.servix.app.ui.components.ServixBadge
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixLogoMark
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.components.SkeletonBox
import ng.name.servix.app.ui.home.HomeTabScreen
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

/** Plan §2: exactly five tabs, identical for guests and signed-in customers — no sixth Dashboard. */
private val BOTTOM_TABS = listOf(homeTab(), exploreTab(), bookingsTab(), aiTab(), accountTab())

@Composable
fun ServixAppShell(
    authViewModel: AuthViewModel,
    catalogueRepository: CatalogueRepository,
    bookingRepository: BookingRepository,
    accountRepository: AccountRepository,
    aiRepository: AiRepository,
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
                Column {
                    HorizontalDivider(color = BorderSand, thickness = 1.dp)
                    NavigationBar(
                        containerColor = WarmIvory,
                        contentColor = Charcoal,
                        tonalElevation = 0.dp,
                    ) {
                        BOTTOM_TABS.forEach { tab ->
                            val selected = when (tab.route) {
                                is ServixRoute.Search -> {
                                    currentDest?.hasRoute(ServixRoute.Search::class) == true ||
                                        currentDest?.hasRoute(ServixRoute.ProfessionalProfile::class) == true ||
                                        currentDest?.hasRoute(ServixRoute.ServiceDetail::class) == true
                                }
                                is ServixRoute.Bookings -> {
                                    currentDest?.hasRoute(ServixRoute.Bookings::class) == true ||
                                        currentDest?.hasRoute(ServixRoute.BookingDetail::class) == true
                                }
                                else -> currentDest?.hasRoute(tab.route::class) == true
                            }
                            val label = stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
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
                    }
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

            composable<ServixRoute.Home> {
                val user = (sessionState as? SessionState.Authenticated)?.user
                val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                val aiUiState by aiViewModel.state.collectAsStateWithLifecycle()

                LaunchedEffect(user?.id, isOnline) {
                    homeViewModel.loadHome(userId = user?.id, forceRefresh = false)
                }

                HomeTabScreen(
                    user = user,
                    city = city,
                    uiState = homeUiState,
                    aiUsage = aiUiState.usage,
                    onRefresh = { homeViewModel.loadHome(userId = user?.id, forceRefresh = true) },
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

            composable<ServixRoute.Search> {
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

            composable<ServixRoute.Bookings> {
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
                )
            }

            composable<ServixRoute.ServixAi> {
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

            composable<ServixRoute.Account> {
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
private fun Stage1AccountTab(
    user: UserDto?,
    accountRepository: AccountRepository,
    updateState: UpdateUiState,
    city: String?,
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
                            text = user.role.replaceFirstChar { it.uppercase() },
                            containerColor = SoftSand,
                            contentColor = DeepForest,
                        )
                    }

                    if (!user.emailVerified) {
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

        UpdateCard(updateState)
        if (updateState.enabled) {
            ServixSecondaryButton(text = "Check for updates", onClick = { updateState.check(force = true) }, testTag = "check_updates")
        }
        DeviceCard(repository = accountRepository, userId = user?.id, city = city)

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
