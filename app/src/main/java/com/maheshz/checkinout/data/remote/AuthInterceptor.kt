package com.maheshz.checkinout.data.remote

import com.maheshz.checkinout.util.DataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val dataStoreManager: DataStoreManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Skip authorization for public endpoints
        if (request.url.encodedPath.contains("/api/public")) {
            return chain.proceed(request)
        }

        val requestBuilder = request.newBuilder()

        // Force Dispatchers.IO to prevent OkHttp thread pool starvation
        val token = runBlocking(Dispatchers.IO) {
            dataStoreManager.accessTokenFlow.firstOrNull()
        }

        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}