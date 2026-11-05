package ng.name.servix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ServixTheme
import ng.name.servix.app.ui.theme.WarmIvory

/**
 * One page that shows every §4 component in one place: used by the `00_components`
 * screenshot and available as an IDE preview.
 */
@Composable
fun ComponentsGallery(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(WarmIvory)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ServixTopBar(title = "Components") { ServixLogoMark(wordmark = false) }

        SectionHeader(title = "Section header", action = "See all") {}

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ServixPrimaryButton(text = "Primary button", onClick = {})
            ServixSecondaryButton(text = "Secondary button", onClick = {})
            ServixTextButton(text = "Text button", onClick = {})
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryChip(label = "Graphic design", selected = true, onClick = {})
                CategoryChip(label = "Web", selected = false, onClick = {})
            }
            SearchField(onClick = {})
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Status chips", style = MaterialTheme.typography.titleMedium, color = DeepForest)
            val statuses = listOf("pending_payment", "requested", "accepted", "in_progress", "delivered", "completed", "cancelled", "declined", "declined_unused", "disputed", "refunded")
            statuses.filter { it != "declined_unused" }.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { pair.forEach { StatusChip(it) } }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Professional row", style = MaterialTheme.typography.titleMedium, color = DeepForest)
            ProfessionalRow(
                name = "Tunde Bakare", title = "Brand & Graphic Designer", rating = 4.9, reviewCount = 156,
                startingPrice = 120000, verified = true, initials = "TB", subtitle = "Abuja",
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Service card", style = MaterialTheme.typography.titleMedium, color = DeepForest)
            ServiceCard(
                title = "Complete Brand Identity Package", professionalName = "Tunde Bakare",
                price = 350000, priceUnit = "per project", rating = 4.9, reviewCount = 71, image = null,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Loading skeleton", style = MaterialTheme.typography.titleMedium, color = DeepForest)
            ListSkeleton()
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Empty state", style = MaterialTheme.typography.titleMedium, color = DeepForest)
            EmptyState(title = "No bookings yet", body = "Book a professional and it will show up here.", actionLabel = "Find a professional") {}
        }

        ErrorCard(message = "We couldn't load your bookings.", onRetry = {})

        AttentionCard(text = "Finish paying ₦350,000", actionLabel = "Pay now") {}

        PriceText(amount = 350000, unit = "per project", style = MaterialTheme.typography.headlineSmall)

        Text("Caption — 12/16 Inter Medium", style = MaterialTheme.typography.labelSmall, color = Charcoal)
    }
}

@Preview(name = "Components — Pixel 7", showBackground = true, widthDp = 411, heightDp = 1800)
@Composable
private fun ComponentsGalleryPreview() {
    ServixTheme { ComponentsGallery() }
}

@Preview(name = "Status chips", showBackground = true, widthDp = 411)
@Composable
private fun StatusChipsPreview() {
    ServixTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("pending_payment", "requested", "accepted", "in_progress", "delivered").forEach { StatusChip(it) }
            listOf("completed", "cancelled", "declined", "refunded", "disputed").forEach { StatusChip(it) }
        }
    }
}

@Preview(name = "Buttons", showBackground = true, widthDp = 411)
@Composable
private fun ButtonsPreview() {
    ServixTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ServixPrimaryButton(text = "Book & Pay", onClick = {})
            ServixSecondaryButton(text = "Save professional", onClick = {})
            ServixTextButton(text = "Cancel", onClick = {})
        }
    }
}
