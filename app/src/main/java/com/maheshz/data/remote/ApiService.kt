package com.maheshz.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable
import retrofit2.http.*

@Serializable
data class RegisterRequest(
    val org_code: String,
    val employee_code: String,
    val full_name: String,
    val email: String,
    val phone: String,
    val department: String,
    val designation: String,
    val fp_public_key: String
)

@JsonClass(generateAdapter = true)
data class RegisterResponse(
    @Json(name = "employee_id") val employee_id: String,
    @Json(name = "org_id") val org_id: String,
    @Json(name = "employee_code") val employee_code: String, // Ensure this property is explicitly present!
    @Json(name = "jwt_access_token") val jwt_access_token: String,
    @Json(name = "jwt_refresh_token") val jwt_refresh_token: String
)
@Serializable
data class RefreshTokenRequest(
    val refresh_token: String
)

@Serializable
data class RefreshTokenResponse(
    val jwt_access_token: String
)

@Serializable
data class ChatRequest(
    val message: String,
    val session_id: String
)


@Suppress("ANNOTATION_TARGETS_NON_EXISTENT_EXPRESSION")
interface ApiService {

    @PUT("/api/employee/me")
    suspend fun updateProfile() // Add body

    @PUT("/api/employee/me/fingerprint")
    suspend fun reEnrollFingerprint()

    @POST("/api/employee/logout")
    suspend fun logout()

    @DELETE("/api/employee/me")
    suspend fun deleteAccount()

    @POST("/api/public/employee/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("/api/public/token/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): RefreshTokenResponse

    @POST("/api/public/employee/register-simplified")
    suspend fun registerSimplified(@Body request: SimplifiedRegisterRequest): RegisterResponse

    @Streaming
    @POST("/api/ai/chat")
    suspend fun chat(@Body request: ChatRequest): retrofit2.Response<okhttp3.ResponseBody>


}
