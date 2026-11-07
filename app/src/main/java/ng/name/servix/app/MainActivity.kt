package ng.name.servix.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.ImageLoader
import okhttp3.OkHttpClient
import ng.name.servix.app.data.network.servixUserAgentInterceptor
import ng.name.servix.app.di.AppContainer
import ng.name.servix.app.ui.auth.AuthViewModel
import ng.name.servix.app.ui.navigation.ServixAppShell
import ng.name.servix.app.ui.theme.ServixTheme
import ng.name.servix.app.util.AppLinkBus
import ng.name.servix.app.util.OAuthReturnBus

private const val COIL_LOG_TAG = "ServixCoil"

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { uri ->
            OAuthReturnBus.publish(uri)
            AppLinkBus.publish(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Transparent bars with dark icons on the Warm Ivory canvas (plan §3).
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        intent?.data?.let(OAuthReturnBus::publish)
        val container = AppContainer.getInstance(applicationContext)
        // Images use their own client: never attach account tokens to third-party URLs.
        Coil.setImageLoader(ImageLoader.Builder(applicationContext)
            .okHttpClient(OkHttpClient.Builder().addInterceptor(servixUserAgentInterceptor).build())
            // A photo that fails to load is otherwise invisible: the UI quietly falls back to
            // initials. Log the URL and the reason at WARN so `adb logcat -s ServixCoil:W` answers
            // "why is my avatar still letters?" without a debugger.
            .eventListener(object : coil.EventListener {
                override fun onError(request: coil.request.ImageRequest, result: coil.request.ErrorResult) {
                    Log.w(
                        COIL_LOG_TAG,
                        "image failed: url=${request.data} key=${request.memoryCacheKey ?: request.data} " +
                            "reason=${result.throwable.javaClass.simpleName}: ${result.throwable.message}",
                    )
                }
            })
            .build())

        setContent {
            ServixTheme {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModel.factory(
                        authRepository = container.authRepository,
                        termsAcceptanceStore = container.termsAcceptanceStore,
                    )
                )
                val isOnline by container.connectivityObserver.isOnline
                    .collectAsStateWithLifecycle(initialValue = true)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ServixAppShell(
                        authViewModel = authViewModel,
                        catalogueRepository = container.catalogueRepository,
                        bookingRepository = container.bookingRepository,
                        accountRepository = container.accountRepository,
                        aiRepository = container.aiRepository,
                        isOnline = isOnline,
                    )
                }
            }
        }
    }
}
