package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

internal data class RegistroRequest(
    val email: String,
    val password: String,
    val name: String
)

internal data class InicioSesionRequest(
    val email: String,
    val password: String
)

internal data class GoogleAuthRequest(
    val idToken: String
)

internal data class UsuarioRespuesta(
    val user: AuthUser
)

internal interface EleganAuthApi {
    @POST("api/movil/auth/register")
    suspend fun register(@Body request: RegistroRequest): AuthResponse

    @POST("api/movil/auth/login")
    suspend fun login(@Body request: InicioSesionRequest): AuthResponse

    @POST("api/movil/auth/google")
    suspend fun loginGoogle(@Body request: GoogleAuthRequest): AuthResponse

    @GET("api/movil/auth/me")
    suspend fun me(@Header("Authorization") authorization: String): UsuarioRespuesta

    @POST("api/movil/auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String)
}

internal object ApiAutenticacionMovil {
    val service: EleganAuthApi by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.ELEGAN_API_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EleganAuthApi::class.java)
    }
}
