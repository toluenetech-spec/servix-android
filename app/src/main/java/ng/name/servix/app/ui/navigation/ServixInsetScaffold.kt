package ng.name.servix.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import ng.name.servix.app.ui.theme.WarmIvory

/**
 * The shell is the only owner of system and IME padding. Scaffold does not add its bottom inset
 * when a bottom bar is present, so remove that bar while the IME is visible. Consuming the
 * applied padding tells descendants that those insets have already been handled.
 * The injectable values let layout tests exercise the same shell with a deterministic fake IME.
 */
@Composable
internal fun ServixInsetScaffold(
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    contentInsets: WindowInsets = WindowInsets.safeDrawing,
    imeVisible: Boolean = WindowInsets.ime.getBottom(LocalDensity.current) > 0,
    content: @Composable () -> Unit,
) {
    Scaffold(
        contentWindowInsets = contentInsets,
        containerColor = WarmIvory,
        topBar = topBar,
        bottomBar = { if (!imeVisible) bottomBar() },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)) {
            content()
        }
    }
}
