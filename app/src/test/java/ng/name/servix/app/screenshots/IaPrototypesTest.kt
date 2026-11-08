package ng.name.servix.app.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import android.content.Context
import ng.name.servix.app.data.model.AccountOverviewDto
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.ServixApiService
import ng.name.servix.app.data.repository.AccountRepository
import ng.name.servix.app.ui.account.AccountGroup
import ng.name.servix.app.ui.account.AccountHubScreen
import ng.name.servix.app.ui.account.AccountRow
import ng.name.servix.app.ui.catalogue.ServixAvatarImage
import ng.name.servix.app.ui.components.CategoryChip
import ng.name.servix.app.ui.components.ProfessionalRow
import ng.name.servix.app.ui.components.SectionHeader
import ng.name.servix.app.ui.components.ServixBadge
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.home.HomeTabScreen
import ng.name.servix.app.ui.home.HomeUiState
import ng.name.servix.app.ui.navigation.ServixBottomBar
import ng.name.servix.app.ui.navigation.ServixTab
import ng.name.servix.app.ui.theme.Coral
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.updates.UpdateUiState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Release 1.1, Stage 0 — information-architecture prototypes.
 *
 * These renders exist to get the **arrangement** approved before any feature code is written:
 *
 * - `ia_00` / `ia_01` — the bottom bar as it ships today and as proposed (Chats in the centre).
 * - `ia_02` / `ia_03` — what a *professional* sees today (the missing "before" state: the app renders
 *   the same customer Home and Account for every role).
 * - `ia_04`…`ia_08` — the proposed Home (customer / professional / guest) and Account (customer /
 *   professional), drawn from the shipping components, tokens and the real `ServixBottomBar`.
 *
 * The "after" screens are prototypes: they assemble production components in the proposed order and
 * are **not** wired into navigation yet. Nothing here ships to users.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = PIXEL_7_QUALIFIERS,
    instrumentedPackages = ["androidx.loader.content"],
)
class IaPrototypesTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val api: ServixApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://127.0.0.1:9/")
            .addConverterFactory(ApiErrorParser.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ServixApiService::class.java)
    }

    @Before
    fun installTestImageLoader() {
        TestAvatarImages.install(ApplicationProvider.getApplicationContext<Context>())
    }

    /* ---------------------------------------------------------------- bottom bar */

    private val beforeTabs = listOf(
        ServixTab("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
        ServixTab("Explore", Icons.Filled.Search, Icons.Outlined.Search, "nav_tab_search"),
        ServixTab("Bookings", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_tab_bookings"),
        ServixTab("Servix AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_tab_ai"),
        ServixTab("Account", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, "nav_tab_account"),
    )

    /** Guests can only browse and sign in (plan §2), so Bookings and Chats are not rendered for them. */
    private val guestTabs = listOf(
        ServixTab("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
        ServixTab("Explore", Icons.Filled.Search, Icons.Outlined.Search, "nav_tab_search"),
        ServixTab("Account", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, "nav_tab_account"),
    )

    private val afterTabs = listOf(
        ServixTab("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
        ServixTab("Explore", Icons.Filled.Search, Icons.Outlined.Search, "nav_tab_search"),
        ServixTab("Chats", Icons.Filled.Chat, Icons.Outlined.Chat, "nav_tab_chats", badgeCount = 3, prominent = true),
        ServixTab("Bookings", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_tab_bookings"),
        ServixTab("Account", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, "nav_tab_account"),
    )

    @Test
    fun bottomBarBefore() = rule.capture("ia_00_bottom_bar_before") {
        Column(Modifier.fillMaxSize().background(WarmIvory)) {
            Spacer(Modifier.weight(1f))
            ServixBottomBar(tabs = beforeTabs, selectedIndex = 0, onSelect = {})
        }
    }

    @Test
    fun bottomBarAfter() = rule.capture("ia_01_bottom_bar_after") {
        Column(Modifier.fillMaxSize().background(WarmIvory)) {
            Spacer(Modifier.weight(1f))
            ServixBottomBar(tabs = afterTabs, selectedIndex = 0, onSelect = {})
        }
    }

    /* ------------------------------------------------------------- before states */

    /** Today a professional gets exactly the customer Home: hire-side content only. */
    @Test
    fun homeProfessionalBefore() = rule.capture("ia_02_home_professional_before") {
        HomeTabScreen(
            user = Fixtures.userProfessional,
            city = "Lagos",
            uiState = HomeUiState(
                loading = false,
                categories = Fixtures.categories,
                professionals = Fixtures.professionals,
                overview = AccountOverviewDto(emailVerified = true),
            ),
            aiUsage = Fixtures.usage,
        )
    }

    /** …and exactly the customer Account: no Requests, no Network, no work-side rows. */
    @Test
    fun accountProfessionalBefore() = rule.capture("ia_03_account_professional_before") {
        AccountHubScreen(
            user = Fixtures.userProfessional,
            accountRepository = AccountRepository { api },
            updateState = UpdateUiState(
                context = ApplicationProvider.getApplicationContext(),
                scope = CoroutineScope(Dispatchers.Unconfined),
            ),
            city = "Lagos",
            unreadNotifications = 2,
        )
    }

    /* -------------------------------------------------------------- after states */

    @Test
    fun homeCustomerAfter() = rule.capture("ia_04_home_customer_after") {
        AfterHome(role = Role.CUSTOMER)
    }

    @Test
    fun homeProfessionalAfter() = rule.capture("ia_05_home_professional_after") {
        AfterHome(role = Role.PROFESSIONAL)
    }

    @Test
    fun homeGuestAfter() = rule.capture("ia_06_home_guest_after") {
        AfterHome(role = Role.GUEST)
    }

    @Test
    fun accountCustomerAfter() = rule.capture("ia_07_account_customer_after") {
        AfterAccount(role = Role.CUSTOMER)
    }

    @Test
    fun accountProfessionalAfter() = rule.capture("ia_08_account_professional_after") {
        AfterAccount(role = Role.PROFESSIONAL)
    }

    private enum class Role { CUSTOMER, PROFESSIONAL, GUEST }

    /* ------------------------------------------------------------------ prototypes */

    @Composable
    private fun AfterHome(role: Role) {
        // Professional sees 1 unread, customer 3: the card and the bar must agree.
        val unread = if (role == Role.PROFESSIONAL) 1 else 3
        val user = when (role) {
            Role.CUSTOMER -> Fixtures.user
            Role.PROFESSIONAL -> Fixtures.userProfessional
            Role.GUEST -> null
        }
        Column(Modifier.fillMaxSize().background(WarmIvory)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = when (role) {
                                Role.GUEST -> "Welcome to Servix"
                                else -> "Good afternoon, ${user?.fullName?.split(" ")?.first() ?: ""}"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = DeepForest,
                        )
                        Text("Saturday, 7 November", style = MaterialTheme.typography.bodySmall, color = MutedGray)
                    }
                    if (role != Role.GUEST) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon15(Icons.Filled.Notifications)
                            ServixAvatarImage(rawImageUrl = user?.avatarUrl, name = user?.fullName ?: "", size = 36.dp)
                        }
                    }
                }

                if (role == Role.GUEST) {
                    ServixCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Sign in to book and track jobs", style = MaterialTheme.typography.titleMedium, color = DeepForest)
                            Text(
                                "Your bookings, payments and chats stay in one place.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                            ServixPrimaryButton(text = "Sign in", onClick = {}, testTag = "proto_sign_in")
                        }
                    }
                }

                if (role == Role.PROFESSIONAL) {
                    // Work-side first: the two things a professional opens the app for.
                    ProtoRow(
                        icon = Icons.Filled.Assignment,
                        title = "Requests",
                        subtitle = "4 new requests match your work · 1 proposal accepted",
                        action = "Browse",
                        tone = SoftSand,
                    )
                    ProtoRow(
                        icon = Icons.Filled.Group,
                        title = "Network",
                        subtitle = "2 connection requests waiting",
                        action = "Review",
                        tone = WarmIvory,
                    )
                }

                if (role != Role.GUEST) {
                    ProtoRow(
                        icon = Icons.Filled.Chat,
                        title = "Chats",
                        subtitle = "$unread unread · Servix AI is here",
                        action = "Open",
                        tone = WarmIvory,
                        badge = unread,
                    )
                }

                if (role != Role.GUEST) {
                    SectionHeader(title = if (role == Role.PROFESSIONAL) "Your jobs" else "Your bookings", action = "See all")
                ServixCard(contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            ServixBadge("Requested", containerColor = SoftSand, contentColor = DeepForest)
                            Text("₦350,000", style = MaterialTheme.typography.titleSmall, color = DeepForest)
                        }
                        Text("Logo suite", style = MaterialTheme.typography.titleMedium, color = DeepForest)
                        Text(
                            // The counterparty, not the viewer: a professional's job card names the customer.
                            if (role == Role.PROFESSIONAL) "Chika Obi · Sat 7 Nov · 10:00" else "Tunde Bakare · Sat 7 Nov · 10:00",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                    }
                    }
                }

                if (role == Role.PROFESSIONAL) {
                    // Decision 4: what a professional acts on replaces hire-side category browsing.
                    ServixCard(contentPadding = PaddingValues(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("This week", style = MaterialTheme.typography.labelMedium, color = ForestGreen)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("3", style = MaterialTheme.typography.titleLarge, color = DeepForest)
                                    Text("Jobs scheduled", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    // A count, not a Naira figure: there is no payouts API in this release.
                                    Text("2", style = MaterialTheme.typography.titleLarge, color = DeepForest)
                                    Text("Completed", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                                }
                            }
                        }
                    }
                    ServixCard(contentPadding = PaddingValues(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Profile strength", style = MaterialTheme.typography.titleSmall, color = DeepForest)
                                Text("78%", style = MaterialTheme.typography.titleSmall, color = ForestGreen)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SoftSand),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.78f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ForestGreen),
                                )
                            }
                            Text(
                                "Photos, a clear title and three portfolio pieces get more bookings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }
                    }
                }

                if (role != Role.PROFESSIONAL) {
                    SectionHeader(title = "Browse by category", action = "See all")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Fixtures.categories.take(5)) { category ->
                            CategoryChip(label = category.name, selected = false, onClick = {})
                        }
                    }
                    SectionHeader(title = "Recommended for you", action = "See all")
                    Column {
                        Fixtures.professionals.take(2).forEach { professional ->
                            ProfessionalRow(
                                name = professional.name,
                                title = professional.title.ifBlank { professional.location },
                                rating = professional.rating,
                                reviewCount = professional.reviewCount,
                                startingPrice = professional.startingPrice,
                                verified = professional.verified,
                                initials = professional.name,
                                subtitle = professional.location.takeIf { it.isNotBlank() },
                            )
                        }
                    }
                }
            }
            ServixBottomBar(
                tabs = if (role == Role.GUEST) {
                    guestTabs
                } else {
                    afterTabs.map { tab ->
                        if (tab.testTag == "nav_tab_chats") tab.copy(badgeCount = unread) else tab
                    }
                },
                selectedIndex = 0,
                onSelect = {},
            )
        }
    }

    @Composable
    private fun AfterAccount(role: Role) {
        val user = if (role == Role.PROFESSIONAL) Fixtures.userProfessional else Fixtures.user
        Column(Modifier.fillMaxSize().background(WarmIvory)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Account", style = MaterialTheme.typography.headlineSmall, color = DeepForest)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ServixAvatarImage(rawImageUrl = user.avatarUrl, name = user.fullName, size = 64.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(user.fullName, style = MaterialTheme.typography.titleLarge, color = DeepForest)
                        Text(user.email, style = MaterialTheme.typography.bodySmall, color = MutedGray)
                        ServixBadge(
                            text = user.role.replaceFirstChar { it.uppercase() },
                            containerColor = SoftSand,
                            contentColor = DeepForest,
                        )
                    }
                }

                if (role == Role.PROFESSIONAL) {
                    AccountGroup(title = "Work") {
                        AccountRow("Requests", "4 new · 1 proposal accepted", "proto_row_requests") {}
                        AccountRow("Network", "2 connection requests", "proto_row_network") {}
                        AccountRow("Professional tools", "Services and availability on the website", "proto_row_protools") {}
                    }
                }

                AccountGroup(title = "Your Servix") {
                    AccountRow("Chats", "3 unread · Servix AI inside", "proto_row_chats") {}
                    if (role == Role.CUSTOMER) {
                        AccountRow("My network", "Connections and requests you sent", "proto_row_network") {}
                    }
                    AccountRow("Saved", "Professionals you kept", "proto_row_saved") {}
                    AccountRow("Payments", "Every charge and refund, read-only", "proto_row_payments") {}
                    AccountRow("Plan", "What your account includes", "proto_row_plan") {}
                }

                AccountGroup(title = "Account") {
                    AccountRow("Profile", "Name and photo", "proto_row_profile") {}
                    AccountRow("Security", "Sign-in protection", "proto_row_security") {}
                }

                AccountGroup(title = "This device") {
                    AccountRow("This device", "Sessions and app version", "proto_row_device") {}
                    AccountRow("Updates", "App version 1.1.0", "proto_row_updates") {}
                }

                AccountGroup(title = "Legal") {
                    AccountRow("Terms and privacy", "Both documents in one place", "proto_row_legal") {}
                    AccountRow("Delete account", "Handled by support over email", "proto_row_delete") {}
                }

                ServixSecondaryButton(
                    text = "Sign out",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "proto_sign_out",
                )
            }
            ServixBottomBar(tabs = afterTabs, selectedIndex = 4, onSelect = {})
        }
    }

    @Composable
    private fun ProtoRow(
        icon: ImageVector,
        title: String,
        subtitle: String,
        action: String,
        tone: androidx.compose.ui.graphics.Color,
        badge: Int = 0,
    ) {
        ServixCard(containerColor = tone, contentPadding = PaddingValues(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(SoftSand),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon15(icon, tint = DeepForest)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = DeepForest)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedGray)
                }
                if (badge > 0) {
                    Box(
                        modifier = Modifier.clip(CircleShape).background(Coral).padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(badge.toString(), style = MaterialTheme.typography.labelSmall, color = WarmIvory)
                    }
                }
                Text(action, style = MaterialTheme.typography.labelLarge, color = DeepForest)
            }
        }
        Spacer(Modifier.height(0.dp))
    }

    @Composable
    private fun Icon15(icon: ImageVector, tint: androidx.compose.ui.graphics.Color = MutedGray) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}
