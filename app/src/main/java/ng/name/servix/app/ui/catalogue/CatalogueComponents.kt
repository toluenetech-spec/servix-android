package ng.name.servix.app.ui.catalogue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import ng.name.servix.app.R
import ng.name.servix.app.data.model.CategoryDto
import ng.name.servix.app.data.model.ProfessionalSummaryDto
import ng.name.servix.app.data.model.ServiceSummaryDto
import ng.name.servix.app.ui.components.ServixBadge
import ng.name.servix.app.ui.components.ServixCard
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
import ng.name.servix.app.util.MediaCacheBust
import ng.name.servix.app.util.ServixFormatters
import java.util.Locale

/**
 * Loads a profile avatar with Coil, showing a SoftSand (#F3EFE6) placeholder while loading
 * and an initials fallback when missing or failed.
 */
@Composable
fun ServixAvatarImage(
    rawImageUrl: String?,
    name: String,
    size: Dp = 64.dp,
    shape: Shape = CircleShape,
    modifier: Modifier = Modifier,
) {
    val resolvedUrl = ServixFormatters.resolveServixMediaUrl(rawImageUrl)
    val initials = ServixFormatters.extractInitials(name)
    val context = LocalContext.current
    val cacheStamps by MediaCacheBust.versions.collectAsState()
    val cacheKey = resolvedUrl?.let { MediaCacheBust.cacheKey(it, cacheStamps[it]) }

    if (resolvedUrl.isNullOrBlank()) {
        InitialsAvatarFallback(
            initials = initials,
            contentDescription = "$name avatar",
            size = size,
            shape = shape,
            modifier = modifier,
        )
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(resolvedUrl)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .crossfade(true)
            .build(),
        contentDescription = "Portrait of $name",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(SoftSand)
            .border(1.dp, BorderSand, shape),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SoftSand),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepForest.copy(alpha = 0.55f),
                )
            }
        },
        error = {
            InitialsAvatarFallback(
                initials = initials,
                contentDescription = "$name avatar",
                size = size,
                shape = shape,
            )
        }
    )
}

@Composable
private fun InitialsAvatarFallback(
    initials: String,
    contentDescription: String,
    size: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(SoftSand)
            .border(1.dp, BorderSand, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = if (size >= 88.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
            color = DeepForest,
        )
    }
}

/**
 * Loads a service or portfolio image with Coil, showing a SoftSand placeholder and fallback icon.
 */
@Composable
fun ServixMediaImage(
    rawImageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    val resolvedUrl = ServixFormatters.resolveServixMediaUrl(rawImageUrl)
    val context = LocalContext.current
    val cacheStamps by MediaCacheBust.versions.collectAsState()
    val cacheKey = resolvedUrl?.let { MediaCacheBust.cacheKey(it, cacheStamps[it]) }

    if (resolvedUrl.isNullOrBlank()) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(SoftSand)
                .border(1.dp, BorderSand, shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Dashboard,
                contentDescription = contentDescription,
                tint = MutedGray,
                modifier = Modifier.size(28.dp),
            )
        }
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(resolvedUrl)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(shape)
            .background(SoftSand),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SoftSand),
            )
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SoftSand),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.BrokenImage,
                    contentDescription = contentDescription,
                    tint = MutedGray,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    )
}

/**
 * Renders "Verified" badge ONLY when verified == true (reference/web/src/components/ui/Badge.jsx).
 */
@Composable
fun VerifiedBadgeChip(
    verified: Boolean,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (!verified) return
    Surface(
        modifier = modifier.testTag("verified_badge"),
        shape = RoundedCornerShape(3.dp),
        color = ForestGreen.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = stringResource(R.string.verified_badge),
                tint = ForestGreen,
                modifier = Modifier.size(if (compact) 13.dp else 14.dp),
            )
            Text(
                text = stringResource(R.string.verified_badge),
                style = MaterialTheme.typography.labelSmall,
                color = DeepForest,
            )
        }
    }
}

/**
 * Rating display matching reference/web/src/components/ui/Rating.jsx.
 */
@Composable
fun RatingRow(
    rating: Double,
    reviewCount: Int? = null,
    showCount: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = "Rating",
            tint = Coral,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = if (rating > 0.0) String.format(Locale.US, "%.1f", rating) else "New",
            style = MaterialTheme.typography.labelLarge,
            color = Charcoal,
        )
        if (showCount && reviewCount != null) {
            Text(
                text = "($reviewCount)",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
        }
    }
}

/**
 * Mirrors reference/web/src/components/cards/ProfessionalCard.jsx:
 * - Avatar (64dp) with SoftSand placeholder and initials fallback
 * - Name + VerifiedBadge (only when pro.verified == true)
 * - Title
 * - Location + Rating(value=pro.rating, count=pro.reviewCount)
 * - "Starting at ₦..." + "View profile ->"
 * - "Free in your window" badge when pro.availableNow == true
 */
@Composable
fun ProfessionalCardItem(
    professional: ProfessionalSummaryDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ServixCard(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("pro_card_${professional.id}"),
        containerColor = PureWhite,
        contentPadding = PaddingValues(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ServixAvatarImage(
                    rawImageUrl = professional.image,
                    name = professional.name,
                    size = 60.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = professional.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Charcoal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        VerifiedBadgeChip(verified = professional.verified, compact = true)
                    }
                    Text(
                        text = professional.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "Location",
                        tint = MutedGray,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = professional.location.ifBlank { "Nigeria" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                RatingRow(
                    rating = professional.rating,
                    reviewCount = professional.reviewCount,
                )
            }

            if (professional.availableNow) {
                ServixBadge(
                    text = stringResource(R.string.free_in_window),
                    containerColor = ForestGreen.copy(alpha = 0.12f),
                    contentColor = DeepForest,
                )
            }

            HorizontalDivider(color = SoftSand, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val formattedPrice = ServixFormatters.formatNaira(professional.startingPrice)
                Text(
                    text = buildAnnotatedString {
                        append("Starting at ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Charcoal)) {
                            append(formattedPrice)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.view_profile),
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepForest,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = DeepForest,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}

/**
 * Mirrors reference/web/src/components/cards/ServiceCard.jsx:
 * - Service image with SoftSand placeholder
 * - Category name
 * - Service title
 * - "by {pro.name}" (when professional is embedded)
 * - Rating + reviewCount
 * - "From ₦..." + "View"
 */
@Composable
fun ServiceCardItem(
    service: ServiceSummaryDto,
    categoryName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ServixCard(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("service_card_${service.id}"),
        containerColor = PureWhite,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column {
            ServixMediaImage(
                rawImageUrl = service.image,
                contentDescription = service.title,
                shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp),
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (categoryName.isNotBlank()) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = ForestGreen,
                    )
                }
                Text(
                    text = service.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Charcoal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                service.professional?.let { pro ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "by ${pro.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                        VerifiedBadgeChip(verified = pro.verified, compact = true)
                    }
                }
                RatingRow(
                    rating = service.rating,
                    reviewCount = service.reviewCount,
                )
                HorizontalDivider(color = SoftSand, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val formattedPrice = ServixFormatters.formatNaira(service.price)
                    Text(
                        text = buildAnnotatedString {
                            append("From ")
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Charcoal)) {
                                append(formattedPrice)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "View",
                            style = MaterialTheme.typography.labelLarge,
                            color = DeepForest,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = DeepForest,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

fun categoryIconFor(iconKey: String): ImageVector {
    return when (iconKey.lowercase()) {
        "code" -> Icons.Outlined.Code
        "layout" -> Icons.Outlined.Dashboard
        "palette", "pen-tool" -> Icons.Outlined.Palette
        "camera" -> Icons.Outlined.CameraAlt
        "video" -> Icons.Outlined.Videocam
        "trending-up", "megaphone" -> Icons.Outlined.TrendingUp
        "edit", "file-text" -> Icons.Outlined.Create
        else -> Icons.Outlined.WorkOutline
    }
}

/**
 * Mirrors reference/web/src/components/cards/CategoryCard.jsx.
 */
@Composable
fun CategoryCardItem(
    category: CategoryDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ServixCard(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("category_card_${category.id}"),
        containerColor = PureWhite,
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SoftSand,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = categoryIconFor(category.icon),
                            contentDescription = category.name,
                            tint = DeepForest,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Charcoal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            if (category.description.isNotBlank()) {
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${category.serviceCount} services",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepForest,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = DeepForest,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    ServixCard(
        modifier = modifier.testTag("empty_state_card"),
        containerColor = PureWhite,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = SoftSand,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.SearchOff,
                        contentDescription = title,
                        tint = DeepForest,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Charcoal,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )
            if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                ServixSecondaryButton(
                    text = actionLabel,
                    onClick = onActionClick,
                    testTag = "empty_state_action_button",
                )
            }
        }
    }
}

@Composable
fun ProfessionalCardSkeleton(modifier: Modifier = Modifier) {
    ServixCard(
        modifier = modifier,
        contentPadding = PaddingValues(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SkeletonBox(
                    modifier = Modifier.size(60.dp),
                    height = 60.dp,
                    shape = CircleShape,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f), height = 18.dp)
                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.45f), height = 14.dp)
                }
            }
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.8f), height = 14.dp)
            SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 16.dp)
        }
    }
}
