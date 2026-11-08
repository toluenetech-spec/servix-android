package ng.name.servix.app.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong
import ng.name.servix.app.R
import ng.name.servix.app.data.model.AccountPaymentsResponse
import ng.name.servix.app.data.model.SecuritySummaryDto
import ng.name.servix.app.ui.components.ServixBadge
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.Coral
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.ServixFormatters

/**
 * The account-security and payment-history sheets that used to live on the removed Dashboard tab.
 * They stay here so the Account tab (pass 4) can host them; Home no longer duplicates them.
 */

@Composable

@Composable

@Composable
internal fun AccountPaymentsSheetContent(
    payments: AccountPaymentsResponse?,
    onOpenBooking: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.dashboard_card_payments_eyebrow),
                style = MaterialTheme.typography.headlineSmall,
                color = DeepForest,
            )
            payments?.mode?.takeIf { it.isNotBlank() }?.let { mode ->
                ServixBadge(
                    text = mode.replaceFirstChar { it.uppercase() },
                    containerColor = SoftSand,
                    contentColor = DeepForest,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = SoftSand.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, BorderSand),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = DeepForest,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.escrow_copy),
                    style = MaterialTheme.typography.bodySmall,
                    color = Charcoal,
                )
            }
        }

        val records = payments?.records.orEmpty()
        if (records.isEmpty()) {
            ServixCard(
                containerColor = WarmIvory,
                contentPadding = PaddingValues(16.dp),
            ) {
                Text(
                    text = "No payments recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                records.forEach { rec ->
                    val bookingId = rec.booking?.id
                    ServixCard(
                        containerColor = WarmIvory,
                        contentPadding = PaddingValues(14.dp),
                        modifier = if (!bookingId.isNullOrBlank()) {
                            Modifier.clickable { onOpenBooking(bookingId) }
                        } else {
                            Modifier
                        },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = rec.booking?.serviceTitle ?: "Booking payment",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DeepForest,
                                )
                                Text(
                                    text = "${rec.booking?.reference ?: rec.reference} · ${ServixFormatters.formatDateTimeMedium(rec.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MutedGray,
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = ServixFormatters.formatNaira(rec.amount.roundToLong()),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Charcoal,
                                )
                                ServixBadge(
                                    text = rec.status.replaceFirstChar { it.uppercase() },
                                    containerColor = if (rec.status == "succeeded" || rec.status == "paid") {
                                        ForestGreen.copy(alpha = 0.14f)
                                    } else {
                                        Coral.copy(alpha = 0.14f)
                                    },
                                    contentColor = if (rec.status == "succeeded" || rec.status == "paid") {
                                        DeepForest
                                    } else {
                                        Charcoal
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        ServixSecondaryButton(
            text = stringResource(R.string.close),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            testTag = "payments_sheet_close_button",
        )
    }
}
