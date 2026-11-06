package ng.name.servix.app.data.network

import android.os.Build
import ng.name.servix.app.BuildConfig
import okhttp3.Interceptor

val servixUserAgent: String
    get() = "ServixAndroid/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE}; ${Build.MANUFACTURER} ${Build.MODEL})"

val servixUserAgentInterceptor = Interceptor { chain ->
    val builder = chain.request().newBuilder()
        .header("User-Agent", servixUserAgent)
        .removeHeader("Origin")
    if (chain.request().url.host == "api.servix.name.ng") {
        builder.header("X-Servix-Client", "ng.name.servix.app")
    }
    chain.proceed(builder.build())
}
