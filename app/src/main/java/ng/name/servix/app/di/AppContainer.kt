package ng.name.servix.app.di

import android.content.Context
import ng.name.servix.app.data.local.TermsAcceptanceStore
import ng.name.servix.app.data.local.TokenStore
import ng.name.servix.app.data.network.PersistentCookieJar
import ng.name.servix.app.data.network.ServixNetworkModule
import ng.name.servix.app.data.repository.AiRepository
import ng.name.servix.app.data.repository.AccountRepository
import ng.name.servix.app.data.repository.AuthRepository
import ng.name.servix.app.data.repository.BookingRepository
import ng.name.servix.app.data.realtime.ChatEventStream
import ng.name.servix.app.data.repository.CatalogueRepository
import ng.name.servix.app.data.repository.ChatRepository
import ng.name.servix.app.data.repository.FeaturesRepository
import ng.name.servix.app.data.repository.UploadRepository
import ng.name.servix.app.BuildConfig
import ng.name.servix.app.util.ConnectivityObserver

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val connectivityObserver = ConnectivityObserver(appContext)
    val tokenStore = TokenStore(appContext)
    val termsAcceptanceStore = TermsAcceptanceStore(appContext)
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

    val aiRepository = AiRepository(networkModule.aiApiService, networkModule.aiHttpClient)

    val catalogueRepository: CatalogueRepository = CatalogueRepository(
        apiProvider = { networkModule.apiService }
    )

    val bookingRepository: BookingRepository = BookingRepository(
        apiProvider = { networkModule.apiService }
    )

    val featuresRepository: FeaturesRepository = FeaturesRepository(networkModule.apiService)

    val chatRepository: ChatRepository = ChatRepository(networkModule.apiService)

    val uploadRepository: UploadRepository = UploadRepository(networkModule.apiService)

    /**
     * One event stream per app session. The URL is the documented SSE route; the client is the app's
     * authenticated one, so the bearer token and refresh flow apply unchanged.
     */
    val chatEventStream: ChatEventStream = ChatEventStream(
        client = networkModule.okHttpClient,
        eventsUrl = BuildConfig.API_BASE_URL.trimEnd('/') + "/community/events",
    )

    val accountRepository: AccountRepository = AccountRepository(
        apiProvider = { networkModule.apiService }
    )

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
