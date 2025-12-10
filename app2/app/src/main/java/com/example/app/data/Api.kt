package com.example.app.data

import android.R
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header

data class RegisterRequest(
    val nickname: String,
    val password: String
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class TokenResponse(
    val refresh: String,
    val access: String
)

data class InformationAboutMe(
    val id: Integer,
    val username: String,
    val avatar: String,
    val exp: String,
    val streak: String
)

data class singleCourse(
    val id: Integer,
    val name: String
)

data class singleLesson(
    val id: Integer,
    val name: String,
    val order: Integer,
    val locked: Boolean,
    val completed: Boolean
)

interface ApiService {
    @POST("auth/register/") //Option to register
    suspend fun registerUser(@Body request: RegisterRequest): retrofit2.Response<Any>

    @POST("auth/login/") //Option to lig into app
    suspend fun loginUser(@Body request: LoginRequest): retrofit2.Response<TokenResponse>

    @POST("auth/logout") //Option to logout
    suspend fun logoutUser(
        @Header("Authorization") logoutToken: String
    ): retrofit2.Response<Any>

    @GET("auth/me") //Option to get informations about log in user
    suspend fun informationAboutMe(
        @Header("Authorization") token: String
    ): retrofit2.Response<InformationAboutMe>

    @GET("courses/")
    suspend fun allCourses(
        @Header("Authorization") token: String
    ): retrofit2.Response<List<singleCourse>>

    @GET("courses/{id}")
    suspend fun singleLesson(
        @Header("Authorization") token: String
    ): retrofit2.Response<List<singleLesson>>
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
