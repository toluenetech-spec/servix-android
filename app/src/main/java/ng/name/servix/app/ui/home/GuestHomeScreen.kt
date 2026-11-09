package ng.name.servix.app.ui.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ng.name.servix.app.ui.catalogue.categoryIconFor
import ng.name.servix.app.ui.components.EmptyState
import ng.name.servix.app.ui.components.ErrorCard
import ng.name.servix.app.ui.components.ListSkeleton
import ng.name.servix.app.ui.components.ProfessionalRow
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.BorderStrongSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.ServixType
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory

@Composable
fun GuestHomeScreen(
    city: String?, state: HomeUiState, onRefresh: () -> Unit, onSearch: () -> Unit,
    onCategory: (String?) -> Unit, onProfessional: (String) -> Unit,
    onSignIn: () -> Unit, onCreateAccount: () -> Unit, onAi: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(WarmIvory)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Welcome", style = ServixType.titleLg, color = Charcoal)
                Text(city?.takeIf { it.isNotBlank() } ?: "Nigeria", style = ServixType.bodySm, color = MutedGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onSignIn, modifier = Modifier.heightIn(min = 48.dp)) { Text("Sign in", style = ServixType.labelLg, color = DeepForest) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
            Surface(onClick = onSearch, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 52.dp).testTag("home_search_field"),
                shape = RoundedCornerShape(16.dp), color = PureWhite, border = BorderStroke(1.dp, BorderStrongSand)) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = Charcoal, modifier = Modifier.size(22.dp))
                    Text("What do you need done?", style = ServixType.bodyLg, color = MutedGray)
                }
            }
            ServixCard(modifier = Modifier.padding(top = 16.dp).testTag("home_sign_in_card"), contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sign in to book and track jobs", style = ServixType.titleMd, color = Charcoal)
                    Text("Your bookings, payments and AI history stay in one place.", style = ServixType.bodySm, color = MutedGray)
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GuestButton("Sign in", onSignIn, Modifier.weight(1f).testTag("home_sign_in_button"))
                        GuestButton("Create account", onCreateAccount, Modifier.weight(1f).testTag("home_create_account_button"), secondary = true)
                    }
                }
            }
            if (state.error != null) ErrorCard(message = state.error, onRetry = onRefresh)
            if (state.loading && state.categories.isEmpty()) {
                GuestSection("Browse by category")
                ListSkeleton(rows = 4)
            } else {
                if (state.categories.isNotEmpty()) {
                    GuestSection("Browse by category", onSeeAll = { onCategory(null) })
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.categories.take(8), key = { it.id }) { category ->
                            Surface(onClick = { onCategory(category.id) }, modifier = Modifier.heightIn(min = 48.dp), shape = CircleShape,
                                color = PureWhite, border = BorderStroke(1.dp, BorderSand)) {
                                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(guestCategoryIcon(category.icon, category.name), contentDescription = null, tint = Charcoal, modifier = Modifier.size(18.dp))
                                    Text(category.name, style = ServixType.labelLg, color = Charcoal)
                                }
                            }
                        }
                    }
                }
                GuestSection("Recommended for you", onSeeAll = { onCategory(null) })
                state.recommended.forEach { professional ->
                    ServixCard(modifier = Modifier.padding(bottom = 8.dp), contentPadding = PaddingValues(4.dp)) {
                        ProfessionalRow(name = professional.name, title = professional.title.ifBlank { professional.location },
                            rating = professional.rating, reviewCount = professional.reviewCount, startingPrice = professional.startingPrice,
                            verified = professional.verified, initials = professional.name, subtitle = professional.location.takeIf { it.isNotBlank() },
                            avatarUrl = professional.image, onClick = { onProfessional(professional.slug) }, modifier = Modifier.testTag("home_professional_row"))
                    }
                }
                if (!state.loading && state.recommended.isEmpty()) EmptyState(title = "No professionals found",
                    body = "Try another category, or ask Servix AI what a job should cost.", actionLabel = "Explore services", onAction = { onCategory(null) })
            }
            ServixCard(modifier = Modifier.padding(top = 28.dp).testTag("home_ai_card"), containerColor = SoftSand.copy(alpha = 0.4f), contentPadding = PaddingValues(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(40.dp).background(DeepForest, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = PureWhite, modifier = Modifier.size(20.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Servix AI", style = ServixType.titleMd, color = Charcoal)
                        Text("Describe the job and get a fair price range before you book.", style = ServixType.bodySm, color = MutedGray)
                        GuestButton("Ask Servix AI", onAi)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestSection(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = ServixType.titleMd, color = Charcoal, modifier = Modifier.weight(1f))
        if (onSeeAll != null) TextButton(onClick = onSeeAll, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("See all", style = ServixType.labelLg, color = DeepForest)
        }
    }
}

@Composable
private fun GuestButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, secondary: Boolean = false) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (secondary) PureWhite else DeepForest, contentColor = if (secondary) DeepForest else PureWhite),
        border = if (secondary) BorderStroke(1.dp, BorderStrongSand) else null, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
        Text(text, style = ServixType.labelLg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

internal fun guestCategoryIcon(icon: String, name: String): androidx.compose.ui.graphics.vector.ImageVector {
    val key = (icon + " " + name).lowercase()
    return when {
        "electric" in key || "bolt" in key -> Icons.Rounded.FlashOn
        "plumb" in key -> Icons.Rounded.Build
        "fridge" in key || "conditioning" in key || "fan" in key -> Icons.Rounded.AcUnit
        "paint" in key || "clean" in key -> Icons.Rounded.Brush
        "tech" in key || "device" in key -> Icons.Rounded.Computer
        else -> categoryIconFor(icon)
    }
}
