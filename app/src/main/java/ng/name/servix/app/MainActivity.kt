package ng.name.servix.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ng.name.servix.app.di.AppContainer
import ng.name.servix.app.ui.auth.AuthViewModel
import ng.name.servix.app.ui.navigation.ServixAppShell
import ng.name.servix.app.ui.theme.ServixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = AppContainer.getInstance(applicationContext)

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
                        isOnline = isOnline,
                    )
                }
            }
        }
    }
}
