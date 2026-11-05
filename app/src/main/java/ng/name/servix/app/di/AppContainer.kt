package ng.name.servix.app.di

import android.content.Context
import ng.name.servix.app.data.local.TokenStore
import ng.name.servix.app.data.network.PersistentCookieJar
import ng.name.servix.app.data.network.ServixNetworkModule
import ng.name.servix.app.data.repository.AuthRepository
import ng.name.servix.app.util.ConnectivityObserver

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val connectivityObserver = ConnectivityObserver(appContext)
    val tokenStore = TokenStore(appContext)
    val cookieJar = PersistentCookieJar(appContext)

    val authRepository: AuthRepository = AuthRepository(
        tokenStore = tokenStore,
        cookieJar = cookieJar,
    )

    val networkModule: ServixNetworkModule = ServixNetworkModule(
        context = appContext,
        tokenStore = tokenStore,
        cookieJar = cookieJar,
        onSessionRefreshed = { user, token ->
            authRepository.onSessionRefreshedFromAuthenticator(user, token)
        },
        onSessionExpired = {
            authRepository.onSessionExpiredFromAuthenticator()
        },
    ).also {
        authRepository.attachNetworkModule(it)
    }

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
        }
    }
}
