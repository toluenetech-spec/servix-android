package ng.name.servix.app.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.ServixType
import ng.name.servix.app.ui.theme.reducedMotion

/** S1 while the existing session initialization runs; does not delay or trigger navigation. */
@Composable
fun SessionSplashScreen() {
    val still = reducedMotion()
    val reveal = remember(still) { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(still) { if (!still) reveal.animateTo(1f, tween(300)) }
    Box(Modifier.fillMaxSize().background(DeepForest).testTag("session_splash")) {
        Column(Modifier.align(Alignment.Center).graphicsLayer {
            alpha = reveal.value
            scaleX = 0.96f + 0.04f * reveal.value
            scaleY = scaleX
        }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(80.dp).background(PureWhite.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .border(1.dp, PureWhite.copy(alpha = 0.15f), RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Handyman, contentDescription = null, tint = PureWhite, modifier = Modifier.size(40.dp))
            }
            Text("Servix", style = ServixType.headlineMd, color = PureWhite)
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (still) Box(Modifier.size(24.dp).border(2.dp, PureWhite.copy(alpha = 0.7f), CircleShape))
            else CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PureWhite, trackColor = PureWhite.copy(alpha = 0.3f), strokeWidth = 2.dp)
            Text("Preparing your secure session…", style = ServixType.bodySm, color = PureWhite.copy(alpha = 0.7f))
        }
    }
}
