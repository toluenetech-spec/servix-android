package ng.name.servix.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.Coral
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory

/**
 * One destination in the bottom bar.
 *
 * [prominent] marks the single centre destination that must read as the strongest thing in the bar
 * (Release 1.1: Chats). It gets a filled icon and a SoftSand disc so it is recognisable at a glance
 * without a second accent colour; [badgeCount] draws the unread pill.
 */
internal data class ServixTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val badgeCount: Int? = null,
    val prominent: Boolean = false,
)

/**
 * The app's bottom bar, extracted from the shell so the information-architecture prototypes can
 * render exactly the same pixels as the shipping shell (see `docs/ia/IA_AUDIT.md`).
 *
 * Guidelines applied: Emil Kowalski — one primary action per surface, so exactly one tab may be
 * [ServixTab.prominent]; the selection indicator stays a soft tonal fill, never a colour gradient or a
 * shadow. Material's own selection semantics are kept (label always visible, 48 dp touch target).
 */
@Composable
internal fun ServixBottomBar(
    tabs: List<ServixTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        HorizontalDivider(color = BorderSand, thickness = 1.dp)
        NavigationBar(
            containerColor = WarmIvory,
            contentColor = Charcoal,
            tonalElevation = 0.dp,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(index) },
                    icon = {
                        // Fixed 34 dp icon slot for every tab: without it the 34 dp disc on the
                        // prominent tab pushes its label ~5 dp below the other four (reported on
                        // ia_01/ia_04). The disc fills the slot, so it can only grow upward.
                        Box(
                            modifier = Modifier.height(34.dp).fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (tab.prominent) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) DeepForest else SoftSand),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                        tint = if (selected) WarmIvory else DeepForest,
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                )
                            }
                            val badge = tab.badgeCount ?: 0
                            if (badge > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 10.dp, y = (-8).dp)
                                        .wrapContentSize()
                                        .clip(CircleShape)
                                        .background(Coral)
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                ) {
                                    Text(
                                        text = if (badge > 9) "9+" else badge.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarmIvory,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    },
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepForest,
                        selectedTextColor = DeepForest,
                        indicatorColor = if (tab.prominent) androidx.compose.ui.graphics.Color.Transparent else SoftSand,
                        unselectedIconColor = MutedGray,
                        unselectedTextColor = MutedGray,
                    ),
                    modifier = Modifier.testTag(tab.testTag),
                )
            }
        }
    }
}
