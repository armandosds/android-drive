package me.proton.android.drive.network

import me.proton.android.drive.BuildConfig
import me.proton.core.network.domain.LogTag
import me.proton.core.util.kotlin.CoreLogger
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

class DriveHttpLoggingInterceptor : Interceptor {

    val logger: (String) -> Unit = { message ->
        CoreLogger.v(LogTag.DEFAULT, message)
    }
    val body =
        HttpLoggingInterceptor(logger).apply { level = HttpLoggingInterceptor.Level.BODY }

    val headers =
        HttpLoggingInterceptor(logger).apply { level = HttpLoggingInterceptor.Level.HEADERS }

    override fun intercept(
        chain: Interceptor.Chain,
    ): Response = if (BuildConfig.DEBUG || BuildConfig.FLAVOR == BuildConfig.FLAVOR_ALPHA) {
        if ("storage" in chain.request().url.pathSegments) {
            headers.intercept(chain)
        } else {
            body.intercept(chain)
        }
    } else {
        chain.proceed(chain.request())
    }
}
