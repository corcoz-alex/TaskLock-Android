package com.corcozalex.tasklock.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.Call

data class LoginResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String
)

data class TokenRefreshRequest(
    val refresh_token: String
)

data class RegisterRequest(
    val email: String,
    val password: String
)

data class UserProfile(
    val id: Int,
    val email: String,
    val is_active: Boolean
)



interface TaskLockApi {
    @GET("/health")
    suspend fun checkHealth(): Map<String, String>

    // We tell Retrofit to encode this as standard Form Data
    @FormUrlEncoded
    @POST("/api/v1/auth/login")
    suspend fun login(
        @Field("username") email: String,
        @Field("password") password: String
    ): LoginResponse

    @POST("/api/v1/auth/refresh")
    fun refreshTokenSync(@Body request: TokenRefreshRequest): Call<LoginResponse>

    @POST("/api/v1/users/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Map<String, Any>

    @GET("/api/v1/users/me")
    suspend fun getMyProfile(): UserProfile

    @GET("/api/v1/tasks")
    suspend fun getTasks(): List<Task>

    @POST("/api/v1/tasks")
    suspend fun createTask(@Body request: TaskCreateRequest): Task

    @PUT("/api/v1/tasks/{task_id}")
    suspend fun updateTask(
        @Path("task_id") taskId: Int,
        @Body request: TaskUpdateRequest
    ): Task

    @DELETE("/api/v1/tasks/{task_id}")
    suspend fun deleteTask(@Path("task_id") taskId: Int)
}


object NetworkClient {
    private const val BASE_URL = "https://tasklock-api.me/api/v1/"

    // Make this publicly accessible so ViewModels can use it without context
    lateinit var tokenManager: TokenManager
        private set

    fun initialize(context: Context) {
        tokenManager = TokenManager(context)
    }

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        // Fast, synchronous hardware read. No deadlocks!
        val token = tokenManager.getAccessToken()
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(TokenAuthenticator(tokenManager)) // Wire the rotation engine
            .build()
    }

    val api: TaskLockApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TaskLockApi::class.java)
    }
}
