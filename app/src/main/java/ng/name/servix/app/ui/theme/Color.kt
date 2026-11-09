package ng.name.servix.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Servix brand palette — mapped 1:1 from the Stitch `tailwind.config` (CORRECTIONS.md §1 G1 / §4).
 * Every value here appears in the approved design; nothing is invented and Material dynamic colour is
 * never used (Theme.kt stays light-only).
 */
val DeepForest = Color(0xFF12372A)      // primary
val ForestGreen = Color(0xFF1F5C45)     // primary-container / secondary
val WarmIvory = Color(0xFFF7F4EC)       // surface / background
val SoftSand = Color(0xFFEDE8DC)        // surface-container / surface-variant
val Coral = Color(0xFFE56B5D)           // tertiary (badges, unread dots)
val Charcoal = Color(0xFF202622)        // on-surface
val MutedGray = Color(0xFF5F675F)       // on-surface-variant
val PureWhite = Color(0xFFFFFFFF)       // surface-container-lowest (cards)

// Surface steps and semantic containers used by the Stitch screens.
val SurfaceContainerLow = Color(0xFFF3EFE5)
val SurfaceContainerHigh = Color(0xFFE6E0D2)
val TertiaryContainer = Color(0xFFFBE4E0)   // tertiary-fixed: unread tint, soft coral panels
val OnTertiaryContainer = Color(0xFFB3372A)
val ErrorContainer = Color(0xFFF9DEDA)
val OnErrorContainer = Color(0xFF7A2418)
val InversePrimary = Color(0xFFCFE0D6)

// Semantic border & danger tokens from tokens.css
val BorderSand = Color(0xFFDDD7C9)        // outline-variant / surface-container-highest
val BorderStrongSand = Color(0xFFC9C2B1)  // outline
val DangerRed = Color(0xFFB3372A)         // error
