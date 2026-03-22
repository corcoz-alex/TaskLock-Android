package com.corcozalex.tasklock.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface TaskLockApi {
    @GET("/")
    suspend fun checkHealth(): Map<String, String>
}

object NetworkClient {
    private const val BASE_URL = "http://161.35.22.224/"

    val api: TaskLockApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TaskLockApi::class.java)
    }
}