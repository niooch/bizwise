package com.example.app.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class RegisterRequest(
    val nickname: String,
    val password: String
)

data class LoginRequest(
    val nickname: String,
    val password: String
)

data class TokenResponse(
    val refresh: String,
    val access: String
)

interface ApiService {
    @POST("auth/register/")
    suspend fun registerUser(@Body request: RegisterRequest): retrofit2.Response<Any>

    @POST("auth/login/")
    suspend fun loginUser(@Body request: LoginRequest): retrofit2.Response<TokenResponse>
}

object RetrofitClient {
    private const val BASE_URL = "http://161.97.130.29/api/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
