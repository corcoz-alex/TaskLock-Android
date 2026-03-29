package com.corcozalex.tasklock.network

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val tokenManager: TokenManager
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        synchronized(this) {
            val currentAccessToken = tokenManager.getAccessToken() ?: return null

            // If the token in the failed request is DIFFERENT from the one in the vault,
            // it means another thread already refreshed it. Just retry with the new token.
            val requestHeader = response.request.header("Authorization")
            if (requestHeader != null && !requestHeader.contains(currentAccessToken)) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokenManager.getAccessToken()}")
                    .build()
            }

            val refreshToken = tokenManager.getRefreshToken() ?: return null

            // 1. Make the synchronous call to your FastAPI backend
            val refreshResponse = NetworkClient.api.refreshTokenSync(
                TokenRefreshRequest(refreshToken)
            ).execute()

            // 2. If it succeeds, save the new keys and retry the failed request
            return if (refreshResponse.isSuccessful) {
                val body = refreshResponse.body()
                if (body != null) {
                    tokenManager.saveTokens(body.access_token, body.refresh_token)

                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${body.access_token}")
                        .build()
                } else {
                    tokenManager.clearTokens()
                    null
                }
            } else {
                // 3. If it fails (refresh token expired/revoked), kill the session
                tokenManager.clearTokens()
                null
            }
        }
    }
}