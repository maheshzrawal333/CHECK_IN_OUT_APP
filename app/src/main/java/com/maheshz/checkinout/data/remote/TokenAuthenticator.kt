package com.maheshz.checkinout.data.remote

import com.maheshz.checkinout.util.DataStoreManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val dataStoreManager: DataStoreManager,
    private val apiServiceProvider: () -> ApiService
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = runBlocking { dataStoreManager.refreshTokenFlow.firstOrNull() }
        if (refreshToken.isNullOrEmpty()) return null

        return runBlocking {
            try {
                val result = apiServiceProvider().refreshToken(RefreshTokenRequest(refreshToken))
                dataStoreManager.saveTokens(result.jwt_access_token, refreshToken)
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${result.jwt_access_token}")
                    .build()
            } catch (e: Exception) {
                dataStoreManager.clear()
                null
            }
        }
    }
}
