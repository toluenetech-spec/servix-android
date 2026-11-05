package ng.name.servix.app.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.graphics.toArgb
import ng.name.servix.app.BuildConfig
import ng.name.servix.app.ui.theme.DeepForest

object CustomTabLauncher {
    fun launchUrl(context: Context, url: String) {
        val colorParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(DeepForest.toArgb())
            .build()
        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colorParams)
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }

    fun launchWebPath(context: Context, path: String = "") {
        val base = BuildConfig.WEB_BASE_URL.trimEnd('/')
        val cleanPath = if (path.startsWith("/") || path.isEmpty()) path else "/$path"
        launchUrl(context, "$base$cleanPath")
    }
}
