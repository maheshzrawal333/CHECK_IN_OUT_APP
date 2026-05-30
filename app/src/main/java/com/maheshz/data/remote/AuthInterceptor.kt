package com.maheshz.data.remote

import com.maheshz.util.DataStoreManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val dataStoreManager: DataStoreManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        // If it's not a public API, attach token
        if (!chain.request().url.encodedPath.contains("/api/public")) {
            runBlocking {
                val token = dataStoreManager.accessTokenFlow.firstOrNull()
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
            }
        }
        return chain.proceed(requestBuilder.build())
    }
}
