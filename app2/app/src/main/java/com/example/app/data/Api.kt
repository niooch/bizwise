package com.example.app.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.Path


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

data class Streak(
    val best_streak: Int,
    val begin_date: String,
    val last_activity_date: String
)

data class InformationAboutMe(
    val id: Integer,
    val username: String,
    val avatar: String,
    val exp: String,
    val streak: Streak
)

data class singleCourse(
    val id: Integer,
    val name: String
)

data class SingleLesson(
    val id: Int,
    val name: String,
    val order: Int,
    val locked: Boolean,
    val completed: Boolean
)

data class CoursesDetailResponse(
    val id: Int,
    val name: String,
    val lessons: List<SingleLesson>
)

data class SingleSlide(
    val id: Int,
    val order: Int,
    val text_content: String,
    val image_url: String?
)

data class AllSlides(
    val id: Int,
    val name: String,
    val slides: List<SingleSlide>,
    val quiz_id: Int?
)

data class AnswerOpion(
    val id: Int,
    val content: String
)

data class Question(
    val id: Int,
    val question_type: String,
    val content: String,
    val answer_options: List<AnswerOpion>
)

data class Quizz(
    val id: Int,
    val name: String,
    val questions: List<Question>
)

data class Answer(
    val question_id: Int,
    val selected_option_id: Int,
    val numeric_answear: Int
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

    @GET("courses/") //Option to get all courses
    suspend fun allCourses(
        @Header("Authorization") token: String
    ): retrofit2.Response<List<singleCourse>>

    @GET("courses/{id}") //Option to get all lessons in single course
    suspend fun singleLesson(
        @Header("Authorization") token: String,
        @Path("id") courseId: Int
    ): retrofit2.Response<CoursesDetailResponse>

    @GET("courses/lessons/{id}/") //Option to get all slides which belong to lesson
    suspend fun allSlides(
        @Header("Authorization") token: String,
        @Path("id") slidesId: Int
    ): retrofit2.Response<AllSlides>

    @GET("quizzes/{id}/") //Option to get a quizz which belongs to lessom
    suspend fun quizzToLesson(
        @Header("Authorization") token: String,
        @Path("id") quizzId: Int
    ): retrofit2.Response<Quizz>

    @POST("quizzes/{id}/submit/")
    suspend fun submitAnswear(
        @Header("Authorization") token: String,
        @Body answer: Answer
    )
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
